package org.cryptomator.event;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.event.BrokenFileNodeEvent;
import org.cryptomator.cryptofs.event.FilesystemEvent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Manager for notifications.
 * <p>
 * To add (filesystem) events, use method {@link #tryAddEvent(Vault, FilesystemEvent)}. If the input event is eligible, it is added to an internal queue.
 * An event is eligible, if
 * <li>
 *     <ul>the event should trigger a notification and</ul>
 *     <ul>it is not added within the last {@value DEBOUNCE_THRESHOLD_SECONDS} seconds</ul>
 * </li>
 *
 */
@Singleton
public class NotificationManager {

	private static final int DEBOUNCE_THRESHOLD_SECONDS = 5;

	Cache<Path, FilesystemEvent> eventCache;
	Queue<VaultEvent> eventsRequiringNotification;

	@Inject
	public NotificationManager() {
		eventCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(DEBOUNCE_THRESHOLD_SECONDS)).build();
		eventsRequiringNotification = new ArrayDeque<>();
	}

	public boolean tryAddEvent(Vault v, FilesystemEvent e) {
		var notRecentlyAdded = switch (e) {
			case BrokenFileNodeEvent bfne -> isRecent(bfne.ciphertextPath(), bfne);
			default -> false;
		};
		if (notRecentlyAdded) {
			synchronized (this) {
				eventsRequiringNotification.add(new VaultEvent(v, e));
			}
		}
		return notRecentlyAdded;
	}

	boolean isRecent(Path key, FilesystemEvent e) {
		var cacheElement = eventCache.get(key, _ -> e);
		return cacheElement == e;
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
