package org.cryptomator.launcher;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.DefaultLoggingEvent;
import org.slf4j.event.Level;
import org.slf4j.event.LoggingEvent;
import org.slf4j.helpers.AbstractLogger;

import java.util.ArrayDeque;
import java.util.Queue;

class EventualLogger extends AbstractLogger {

	static EventualLogger getInstance() {
		return Wrapped.INSTANCE.get();
	}


	private final Queue<LoggingEvent> bufferedEvents = new ArrayDeque<>();

	private EventualLogger() {
	}

	synchronized void drainTo(Logger gutter) {
		for (var event : bufferedEvents) {
			var builder = gutter.atLevel(event.getLevel());
			builder.setCause(event.getThrowable());
			builder.setMessage(event.getMessage());
			event.getArguments().forEach(builder::addArgument);
			event.getMarkers().forEach(builder::addMarker);
		}
		bufferedEvents.clear();
	}

	@Override
	protected synchronized void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
		var event = new DefaultLoggingEvent(level, this);
		if (marker != null) {
			event.addMarker(marker);
		}
		event.setMessage(messagePattern);
		for (var arg : arguments) {
			event.addArgument(arg);
		}
		event.setThrowable(throwable);
		bufferedEvents.add(event);
	}

	//Unclear, unused and undocumented method of slf4j, see also https://github.com/qos-ch/slf4j/discussions/348
	@Override
	protected String getFullyQualifiedCallerName() {
		return getClass().getCanonicalName();
	}


	@Override
	public boolean isTraceEnabled() {
		return true;
	}

	@Override
	public boolean isTraceEnabled(Marker marker) {
		return true;
	}

	@Override
	public boolean isDebugEnabled() {
		return true;
	}

	@Override
	public boolean isDebugEnabled(Marker marker) {
		return true;
	}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public boolean isInfoEnabled(Marker marker) {
		return true;
	}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}

	@Override
	public boolean isWarnEnabled(Marker marker) {
		return true;
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public boolean isErrorEnabled(Marker marker) {
		return true;
	}

	private enum Wrapped {
		INSTANCE;

		EventualLogger actualInstance;

		Wrapped() {
			actualInstance = new EventualLogger();
		}

		public EventualLogger get() {
			return actualInstance;
		}
	}
}
