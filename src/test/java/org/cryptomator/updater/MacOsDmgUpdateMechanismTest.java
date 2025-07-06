package org.cryptomator.updater;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

class MacOsDmgUpdateMechanismTest {

	public static void main(String args[]) throws InterruptedException, IOException {
		UpdateMechanism updateMechanism = new MacOsDmgUpdateMechanism();
		if (updateMechanism.isUpdateAvailable()) {
			System.out.println("Update is available.");
		}
		var updateProcess = updateMechanism.prepareUpdate();
		do {
			double percentage = updateProcess.preparationProgress() * 100.0;
			System.out.printf("\rPreparing update: %.2f%%", percentage);
		} while (!updateProcess.await(100, TimeUnit.MILLISECONDS));
		System.out.println("\nUpdate ready...");
		Process p = updateProcess.applyUpdate();
		p.isAlive();
		System.out.println("Update running, exiting...");
		// exit.
	}

}