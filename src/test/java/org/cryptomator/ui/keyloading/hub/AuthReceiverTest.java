package org.cryptomator.ui.keyloading.hub;

public class AuthReceiverTest {

	static {
		System.setProperty("LOGLEVEL", "INFO");
	}

	public static void main(String[] args) {
		try (var receiver = AuthReceiver.start()) {
			System.out.println("Waiting on " + receiver.getRedirectURL());
			var token = receiver.receive();
			System.out.println("SUCCESS: " + token);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			System.out.println("CANCELLED");
		} catch (Exception e) {
			System.out.println("ERROR");
			e.printStackTrace();
		}
	}

}