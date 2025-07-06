package org.cryptomator.updater;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public abstract class DownloadUpdateProcess implements UpdateProcess {

	protected final Path workDir;
	private final URI uri;
	private final byte[] checksum;
	private final AtomicLong totalBytes;
	private final LongAdder loadedBytes = new LongAdder();
	private final Thread downloadThread;
	private final CountDownLatch downloadCompleted = new CountDownLatch(1);
	protected volatile IOException downloadException;
	protected volatile boolean downloadSuccessful;

	/**
	 * Creates a new DownloadUpdateProcess instance.
	 * @param workdir The directory where the update will be downloaded to. Ideally, this should be a temporary directory that is cleaned up after the update process is complete.
	 * @param uri The URI from which the update will be downloaded.
	 * @param checksum (optional) The expected SHA-256 checksum of the downloaded file, can be null if not required.
	 * @param estDownloadSize The estimated size of the download in bytes.
	 */
	protected DownloadUpdateProcess(Path workdir, URI uri, byte[] checksum, long estDownloadSize) {
		this.workDir = workdir;
		this.uri = uri;
		this.checksum = checksum;
		this.totalBytes = new AtomicLong(estDownloadSize);
		this.downloadThread = Thread.ofVirtual().start(this::download);
	}

	@Override
	public double preparationProgress() {
		return (double) loadedBytes.sum() / totalBytes.get();
	}

	@Override
	public void await() throws InterruptedException {
		downloadCompleted.await();
	}

	@Override
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		return downloadCompleted.await(timeout, unit);
	}

	@Override
	public void cancel() {
		downloadThread.interrupt();
	}

	private void download() {
		try {
			download("update.dmg");
			downloadSuccessful = true;
		} catch (IOException e) {
			// TODO: eventually handle this via structured concurrency?
			downloadException = e;
		} finally {
			downloadCompleted.countDown();
		}
	}

	/**
	 * Downloads the update from the given URI and saves it to the specified filename in the working directory.
	 * @param filename the name of the file to save the update as in the working directory
	 * @throws IOException indicating I/O errors during the download or file writing process or due to checksum mismatch
	 */
	protected void download(String filename) throws IOException {
		var request = HttpRequest.newBuilder().uri(uri).GET().build();
		var downloadFile = workDir.resolve(filename);
		try (HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()) {
			// make download request
			var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200) {
				throw new IOException("Failed to download update, status code: " + response.statusCode());
			}

			// update totalBytes
			response.headers().firstValueAsLong("Content-Length").ifPresent(totalBytes::set);

			// prepare checksum calculation
			MessageDigest sha256;
			try {
				sha256 = MessageDigest.getInstance("SHA-256"); // Initialize SHA-256 digest, not used here but can be extended for checksum validation
			} catch (NoSuchAlgorithmException e) {
				throw new AssertionError("Every implementation of the Java platform is required to support [...] SHA-256", e);
			}

			// write bytes to file
			try (var in = new DownloadInputStream(response.body(), loadedBytes, sha256);
				 var src = Channels.newChannel(in);
				 var dst = FileChannel.open(downloadFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
				dst.transferFrom(src, 0, totalBytes.get());
			}

			// verify checksum if provided
			byte[] calculatedChecksum = sha256.digest();
			if (!MessageDigest.isEqual(calculatedChecksum, checksum)) {
				throw new IOException("Checksum verification failed for downloaded file: " + filename);
			}

			// post-download processing
			postDownload(downloadFile);
		} catch (InterruptedException e) {
			throw new InterruptedIOException("Download interrupted");
		}
	}

	protected void postDownload(Path downloadedFile) throws IOException {
		// Default implementation does nothing, can be overridden by subclasses for specific post-download actions
	}

	/**
	 * An InputStream decorator that counts the number of bytes read and updates a MessageDigest for checksum calculation.
	 */
	private static class DownloadInputStream extends FilterInputStream {

		private final LongAdder counter;
		private final MessageDigest digest;

		protected DownloadInputStream(InputStream in, LongAdder counter, MessageDigest digest) {
			super(in);
			this.counter = counter;
			this.digest = digest;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int n = super.read(b, off, len);
			digest.update(b, off, n);
			counter.add(n);
			return n;
		}

		@Override
		public int read() throws IOException {
			int b = super.read();
			if (b != -1) {
				digest.update((byte) b);
				counter.increment();
			}
			return b;
		}

	}

}
