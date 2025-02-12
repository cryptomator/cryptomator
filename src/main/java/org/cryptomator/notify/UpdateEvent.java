package org.cryptomator.notify;

public record UpdateEvent(String newVersion) implements Event {}
