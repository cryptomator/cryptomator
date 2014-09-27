package de.sebastianstenzel.oce.crypto.exceptions;

public class StorageCryptingException extends Exception {
	private static final long serialVersionUID = -6622699014483319376L;
	
	public StorageCryptingException(String string) {
		super(string);
	}
	
	public StorageCryptingException(String string, Throwable t) {
		super(string, t);
	}
}