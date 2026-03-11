package org.cryptomator.event;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.FileIsInUseEvent;
import org.cryptomator.cryptofs.event.FilesystemEvent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manager for notifications.
 * <p>
 * To add (filesystem) events, use method {@link #offer(Vault, FilesystemEvent)}. If the input event is eligible, it is added to an internal queue.
 * An event is eligible, if
 * <ul>
 *     <li>the event should trigger a notification and</li>
 *     <li>it is not added within the last {@value DEBOUNCE_THRESHOLD_SECONDS} seconds</li>
 * </ul>
 *
 * @see org.cryptomator.ui.fxapp.FxNotificationManager
 */
@Singleton
public class NotificationManager {

	private static final int DEBOUNCE_THRESHOLD_SECONDS = 5;

	private final Cache<FSEventBucket, FilesystemEvent> debounceCache;
	private final ConcurrentLinkedQueue<VaultEvent> pendingEvents;

	@Inject
	public NotificationManager() {
		debounceCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(DEBOUNCE_THRESHOLD_SECONDS)).build();
		pendingEvents = new ConcurrentLinkedQueue<>();
	}

	/**
	 * Offers the given filesystem event to the notification manager.
	 *
	 * @param v The vault where the filesystem event happened
	 * @param e the actual filesystem event
	 * @return {@code true} if the filesystem event is accepted, otherwise {@code false}.
	 */
	public boolean offer(Vault v, FilesystemEvent e) {
		return switch (e) {
			case FileIsInUseEvent fiiue -> addEvent(v, fiiue.ciphertextPath(), fiiue);
			default -> false;
		};
	}

	boolean addEvent(Vault v, Path keyPath, FilesystemEvent e) {
		var key = new FSEventBucket(v, keyPath, e.getClass());
		var isAdded = new AtomicBoolean(false);
		debounceCache.asMap().computeIfAbsent(key, _ -> {
			synchronized (this) {
				pendingEvents.add(new VaultEvent(v, e));
				isAdded.set(true);
			}
			return e;
		});
		return isAdded.get();
	}

	/**
	 * Adds all events to the target list and clears afterward the pending-event-queue
	 *
	 * @param target list where the filesystem events are copied to
	 * @return {@code true}, if elements were copied
	 */
	public boolean appendToAndClear(List<VaultEvent> target) {
		//it is not clear if addAll iterates thread-safe over the pendingEvents
		//hence we synchronize moving (copy then clear) and adding-single-element operations
		synchronized (this) {
			var result = target.addAll(pendingEvents);
			pendingEvents.clear();
			return result;
		}
	}
}
