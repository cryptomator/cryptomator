package org.cryptomator.event;

public sealed interface Event extends Comparable<Event> permits UpdateEvent, VaultEvent {

	long getTimestampMilli();

	default int compareTo(Event other) {
		long timeDiff = this.getTimestampMilli() - other.getTimestampMilli();
		if (timeDiff != 0) {
			return (int) timeDiff;
		}
		return this.equals(other) ? 0 : this.getClass().getName().compareTo(other.getClass().getName());
	}

}
