package org.cryptomator.ui.fxapp;

import org.cryptomator.integrations.common.DisplayName;
import org.cryptomator.integrations.common.OperatingSystem;
import org.cryptomator.integrations.common.Priority;
import org.cryptomator.integrations.uiappearance.Theme;
import org.cryptomator.integrations.uiappearance.UiAppearanceException;
import org.cryptomator.integrations.uiappearance.UiAppearanceListener;
import org.cryptomator.integrations.uiappearance.UiAppearanceProvider;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@DisplayName("JavaFX Color Scheme switcher")
@OperatingSystem(OperatingSystem.Value.LINUX)
@OperatingSystem(OperatingSystem.Value.WINDOWS)
@OperatingSystem(OperatingSystem.Value.MAC)
@Priority(1050)
public class JfxUiAppearanceProvider implements UiAppearanceProvider {

	final ConcurrentHashMap<UiAppearanceListener, ChangeListener<ColorScheme>> uiAppearanceListeners = new ConcurrentHashMap<>();
	final AtomicReference<Platform.Preferences> fxPreferencesContainer = new AtomicReference<>();

	public void setJavaFXPlatform(Platform.Preferences preferences) {
		fxPreferencesContainer.set(preferences);
	}

	@Override
	public Theme getSystemTheme() {
		var pref = fxPreferencesContainer.get();
		if (pref != null) {
			return switch (pref.getColorScheme()) {
				case DARK -> Theme.DARK;
				case LIGHT -> Theme.LIGHT;
			};
		}
		return Theme.LIGHT;
	}

	@Override
	public void adjustToTheme(Theme theme) {
		//no-op
	}

	@Override
	public void addListener(UiAppearanceListener uiAppearanceListener) throws UiAppearanceException {
		var pref = fxPreferencesContainer.get();
		if (pref != null) {
			var fxChangeListener = (ChangeListener<ColorScheme>) (_, _, newScheme) -> {
				var newTheme = switch (newScheme) {
					case DARK -> Theme.DARK;
					case LIGHT -> Theme.LIGHT;
				};
				uiAppearanceListener.systemAppearanceChanged(newTheme);
			};
			uiAppearanceListeners.compute(uiAppearanceListener, (k, v) -> {
				pref.colorSchemeProperty().addListener(fxChangeListener);
				return fxChangeListener;
			});
		}
	}

	@Override
	public void removeListener(UiAppearanceListener uiAppearanceListener) throws UiAppearanceException {
		var pref = fxPreferencesContainer.get();
		var fxChangeListener = uiAppearanceListeners.remove(uiAppearanceListener);
		if (pref != null && fxChangeListener != null) {
			pref.colorSchemeProperty().removeListener(fxChangeListener);
		}
	}

}
