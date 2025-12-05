package org.cryptomator.event;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.FilesystemEvent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

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
 */
@Singleton
public class NotificationManager {

	private static final int DEBOUNCE_THRESHOLD_SECONDS = 5;

	Cache<FSEventBucket, FilesystemEvent> eventCache;
	ConcurrentLinkedQueue<VaultEvent> eventsRequiringNotification;

	@Inject
	public NotificationManager() {
		eventCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(DEBOUNCE_THRESHOLD_SECONDS)).build();
		eventsRequiringNotification = new ConcurrentLinkedQueue<>();
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
			//example: case BrokenFileNodeEvent bfne -> addEvent(v, bfne.ciphertextPath(), bfne);
			default -> false;
		};
	}

	boolean addEvent(Vault v, Path keyPath, FilesystemEvent e) {
		var key = new FSEventBucket(v, keyPath, e.getClass());
		var cachedElement = eventCache.get(key, _ -> {
			synchronized (this) {
				eventsRequiringNotification.add(new VaultEvent(v, e));
			}
			return e;
		});
		return cachedElement != e;
	}

	/**
	 * Clones all events requiring a notification to the target list and clears afterward the notification manager queue
	 *
	 * @param target list the queue is cloned to
	 * @return {@code true}, if elements were copied
	 */
	public boolean cloneTo(List<VaultEvent> target) {
		synchronized (this) {
			var result = target.addAll(eventsRequiringNotification);
			eventsRequiringNotification.clear();
			return result;
		}
	}
}
