package org.cryptomator.ui.traymenu;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.vaults.Volume;

import java.awt.Desktop;
import java.io.IOException;

@Module
abstract class TrayMenuModule {

	@Provides
	static Volume.Revealer provideAwtRevealer(){
		return p -> {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
				try {
					Desktop.getDesktop().open(p.toFile());
				} catch (IOException e) {
					throw new Volume.VolumeException(e);
				}
			} else {
				throw new Volume.VolumeException("API to browse files not supported. Please try again from inside the application.");
			}
		};
	}
}
