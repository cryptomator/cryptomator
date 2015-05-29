/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.ResourceType;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.crypto.exceptions.CounterOverflowException;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.EncryptFailedException;
import org.cryptomator.webdav.exceptions.DavRuntimeException;
import org.cryptomator.webdav.exceptions.IORuntimeException;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EncryptedDir extends AbstractEncryptedNode implements FileConstants {

	private static final Logger LOG = LoggerFactory.getLogger(EncryptedDir.class);
	private final FilenameTranslator filenameTranslator;
	private String directoryId;
	private Path directoryPath;

	public EncryptedDir(CryptoResourceFactory factory, DavResourceLocator locator, DavSession session, LockManager lockManager, Cryptor cryptor, FilenameTranslator filenameTranslator, Path filePath) {
		super(factory, locator, session, lockManager, cryptor, filePath);
		this.filenameTranslator = filenameTranslator;
		properties.add(new ResourceType(ResourceType.COLLECTION));
		properties.add(new DefaultDavProperty<Integer>(DavPropertyName.ISCOLLECTION, 1));
	}

	/**
	 * @return Path or <code>null</code>, if directory does not yet exist.
	 */
	protected synchronized String getDirectoryId() {
		if (directoryId == null) {
			try {
				directoryId = filenameTranslator.getDirectoryId(filePath, false);
			} catch (IOException e) {
				throw new IORuntimeException(e);
			}
		}
		return directoryId;
	}

	/**
	 * @return Path or <code>null</code>, if directory does not yet exist.
	 */
	private synchronized Path getDirectoryPath() {
		if (directoryPath == null) {
			final String dirId = getDirectoryId();
			if (dirId != null) {
				directoryPath = filenameTranslator.getEncryptedDirectoryPath(directoryId);
			}
		}
		return directoryPath;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public long getModificationTime() {
		try {
			final Path dirPath = getDirectoryPath();
			if (dirPath == null) {
				return -1;
			} else {
				return Files.getLastModifiedTime(dirPath).toMillis();
			}
		} catch (IOException e) {
			return -1;
		}
	}

	@Override
	public void addMember(DavResource resource, InputContext inputContext) throws DavException {
		if (resource instanceof AbstractEncryptedNode) {
			addMember((AbstractEncryptedNode) resource, inputContext);
		} else {
			throw new IllegalArgumentException("Unsupported resource type: " + resource.getClass().getName());
		}
	}

	private void addMember(AbstractEncryptedNode childResource, InputContext inputContext) throws DavException {
		if (childResource.isCollection()) {
			this.addMemberDir(childResource.getLocator(), inputContext);
		} else {
			this.addMemberFile(childResource.getLocator(), inputContext);
		}
	}

	private void addMemberDir(DavResourceLocator childLocator, InputContext inputContext) throws DavException {
		final Path dirPath = getDirectoryPath();
		if (dirPath == null) {
			throw new DavException(DavServletResponse.SC_NOT_FOUND);
		}
		try {
			final String cleartextDirName = FilenameUtils.getName(childLocator.getResourcePath());
			final String ciphertextDirName = filenameTranslator.getEncryptedDirFileName(cleartextDirName);
			final Path dirFilePath = dirPath.resolve(ciphertextDirName);
			final String directoryId = filenameTranslator.getDirectoryId(dirFilePath, true);
			final Path directoryPath = filenameTranslator.getEncryptedDirectoryPath(directoryId);
			Files.createDirectories(directoryPath);
		} catch (SecurityException e) {
			throw new DavException(DavServletResponse.SC_FORBIDDEN, e);
		} catch (IOException e) {
			throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	private void addMemberFile(DavResourceLocator childLocator, InputContext inputContext) throws DavException {
		final Path dirPath = getDirectoryPath();
		if (dirPath == null) {
			throw new DavException(DavServletResponse.SC_NOT_FOUND);
		}
		try {
			final String cleartextFilename = FilenameUtils.getName(childLocator.getResourcePath());
			final String ciphertextFilename = filenameTranslator.getEncryptedFilename(cleartextFilename);
			final Path filePath = dirPath.resolve(ciphertextFilename);
			try (final FileChannel c = FileChannel.open(filePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING); final FileLock lock = c.lock(0L, FILE_HEADER_LENGTH, false)) {
				cryptor.encryptFile(inputContext.getInputStream(), c);
			} catch (SecurityException e) {
				throw new DavException(DavServletResponse.SC_FORBIDDEN, e);
			} catch (CounterOverflowException e) {
				// lets indicate this to the client as a "file too big" error
				throw new DavException(DavServletResponse.SC_INSUFFICIENT_SPACE_ON_RESOURCE, e);
			} catch (EncryptFailedException e) {
				LOG.error("Encryption failed for unknown reasons.", e);
				throw new IllegalStateException("Encryption failed for unknown reasons.", e);
			} finally {
				IOUtils.closeQuietly(inputContext.getInputStream());
			}
		} catch (IOException e) {
			LOG.error("Failed to create file.", e);
			throw new IORuntimeException(e);
		}
	}

	@Override
	public DavResourceIterator getMembers() {
		try {
			final Path dirPath = getDirectoryPath();
			if (dirPath == null) {
				throw new DavException(DavServletResponse.SC_NOT_FOUND);
			}
			final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dirPath, DIRECTORY_CONTENT_FILTER);
			final List<DavResource> result = new ArrayList<>();

			for (final Path childPath : directoryStream) {
				try {
					final String cleartextFilename = filenameTranslator.getCleartextFilename(childPath.getFileName().toString());
					final String cleartextFilepath = FilenameUtils.concat(getResourcePath(), cleartextFilename);
					final DavResourceLocator childLocator = locator.getFactory().createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), cleartextFilepath);
					final DavResource resource;
					if (StringUtil.endsWithIgnoreCase(childPath.getFileName().toString(), DIR_EXT)) {
						resource = factory.createChildDirectoryResource(childLocator, session, childPath);
					} else {
						assert StringUtil.endsWithIgnoreCase(childPath.getFileName().toString(), FILE_EXT);
						resource = factory.createChildFileResource(childLocator, session, childPath);
					}
					result.add(resource);
				} catch (DecryptFailedException e) {
					LOG.warn("Decryption of resource failed: " + childPath);
					continue;
				}
			}
			return new DavResourceIteratorImpl(result);
		} catch (IOException e) {
			LOG.error("Exception during getMembers.", e);
			throw new IORuntimeException(e);
		} catch (DavException e) {
			LOG.error("Exception during getMembers.", e);
			throw new DavRuntimeException(e);
		}
	}

	@Override
	public void removeMember(DavResource member) throws DavException {
		if (member instanceof AbstractEncryptedNode) {
			removeMember((AbstractEncryptedNode) member);
		} else {
			throw new IllegalArgumentException("Unsupported resource type: " + member.getClass().getName());
		}
	}

	private void removeMember(AbstractEncryptedNode member) throws DavException {
		final Path dirPath = getDirectoryPath();
		if (dirPath == null) {
			throw new DavException(DavServletResponse.SC_NOT_FOUND);
		}
		try {
			final String cleartextFilename = FilenameUtils.getName(member.getResourcePath());
			final String ciphertextFilename;
			if (member instanceof EncryptedDir) {
				final EncryptedDir subDir = (EncryptedDir) member;
				// remove sub-members recursively before deleting own directory
				for (Iterator<DavResource> iterator = member.getMembers(); iterator.hasNext();) {
					DavResource m = iterator.next();
					member.removeMember(m);
				}
				final Path subDirPath = subDir.getDirectoryPath();
				if (subDirPath != null) {
					Files.deleteIfExists(subDirPath);
				}
				ciphertextFilename = filenameTranslator.getEncryptedDirFileName(cleartextFilename);
			} else {
				ciphertextFilename = filenameTranslator.getEncryptedFilename(cleartextFilename);
			}
			final Path memberPath = dirPath.resolve(ciphertextFilename);
			Files.deleteIfExists(memberPath);
		} catch (FileNotFoundException e) {
			// no-op
		} catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public void move(AbstractEncryptedNode dest) throws DavException, IOException {
		// when moving a directory we only need to move the file (actual dir is ID-dependent and won't change)
		final Path srcPath = filePath;
		final Path dstPath;
		if (dest instanceof NonExistingNode) {
			dstPath = ((NonExistingNode) dest).getDirFilePath();
		} else {
			dstPath = dest.filePath;
		}

		// move:
		Files.createDirectories(dstPath.getParent());
		try {
			Files.move(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@Override
	public void copy(AbstractEncryptedNode dest, boolean shallow) throws DavException, IOException {
		final Path dstDirFilePath;
		if (dest instanceof NonExistingNode) {
			dstDirFilePath = ((NonExistingNode) dest).getDirFilePath();
		} else {
			dstDirFilePath = dest.filePath;
		}

		// copy dirFile:
		final String srcDirId = getDirectoryId();
		if (srcDirId == null) {
			throw new DavException(DavServletResponse.SC_NOT_FOUND);
		}
		final String dstDirId = UUID.randomUUID().toString();
		try (final FileChannel c = FileChannel.open(dstDirFilePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DSYNC); final FileLock lock = c.lock()) {
			c.write(ByteBuffer.wrap(dstDirId.getBytes(StandardCharsets.UTF_8)));
		}

		// copy actual dir:
		if (!shallow) {
			copyDirectoryContents(srcDirId, dstDirId);
		} else {
			final Path dstDirPath = filenameTranslator.getEncryptedDirectoryPath(dstDirId);
			Files.createDirectories(dstDirPath);
		}
	}

	private void copyDirectoryContents(String srcDirId, String dstDirId) throws IOException {
		final Path srcDirPath = filenameTranslator.getEncryptedDirectoryPath(srcDirId);
		final Path dstDirPath = filenameTranslator.getEncryptedDirectoryPath(dstDirId);
		Files.createDirectories(dstDirPath);
		final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(srcDirPath, DIRECTORY_CONTENT_FILTER);
		for (final Path srcChildPath : directoryStream) {
			final String childName = srcChildPath.getFileName().toString();
			final Path dstChildPath = dstDirPath.resolve(childName);
			if (StringUtils.endsWithIgnoreCase(childName, FILE_EXT)) {
				try {
					Files.copy(srcChildPath, dstChildPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				} catch (AtomicMoveNotSupportedException e) {
					Files.copy(srcChildPath, dstChildPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
				}
			} else if (StringUtils.endsWithIgnoreCase(childName, DIR_EXT)) {
				final String srcSubdirId = filenameTranslator.getDirectoryId(srcChildPath, false);
				final String dstSubdirId = filenameTranslator.getDirectoryId(dstChildPath, true);
				copyDirectoryContents(srcSubdirId, dstSubdirId);
			}
		}
	}

	@Override
	public void spool(OutputContext outputContext) throws IOException {
		// do nothing
	}

}
