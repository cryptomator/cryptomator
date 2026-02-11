package org.cryptomator.ui.fxapp;

import org.cryptomator.integrations.common.DisplayName;
import org.cryptomator.integrations.common.OperatingSystem;
import org.cryptomator.integrations.common.Priority;
import org.cryptomator.integrations.uiappearance.Theme;
import org.cryptomator.integrations.uiappearance.UiAppearanceException;
import org.cryptomator.integrations.uiappearance.UiAppearanceListener;
import org.cryptomator.integrations.uiappearance.UiAppearanceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@DisplayName("JavaFX Color Scheme switcher")
@OperatingSystem(OperatingSystem.Value.LINUX)
@OperatingSystem(OperatingSystem.Value.WINDOWS)
@Priority(1050)
public class JfxUiAppearanceProvider implements UiAppearanceProvider {

	private static final Logger LOG = LoggerFactory.getLogger(JfxUiAppearanceProvider.class);

	private final AtomicReference<JfxUiAppearanceImpl> realImpl = new AtomicReference<>(null);

	public void initialize(Platform.Preferences preferences) {
		var isSet = realImpl.compareAndSet(null, new JfxUiAppearanceImpl(preferences));
		if (isSet) {
			LOG.debug("Initialized {} with JavaFX preferences", JfxUiAppearanceImpl.class);
		}
	}

	private static class JfxUiAppearanceImpl implements UiAppearanceProvider {

		private final Platform.Preferences preferences;
		private final ConcurrentHashMap<UiAppearanceListener, ChangeListener<ColorScheme>> uiAppearanceListeners = new ConcurrentHashMap<>();

		private JfxUiAppearanceImpl(Platform.Preferences preferences) {
			this.preferences = preferences;
		}

		@Override
		public Theme getSystemTheme() {
			return switch (preferences.getColorScheme()) {
				case DARK -> Theme.DARK;
				case LIGHT -> Theme.LIGHT;
			};
		}

		@Override
		public void adjustToTheme(Theme theme) {
			//no-op
		}

		@Override
		public void addListener(UiAppearanceListener uiAppearanceListener) throws UiAppearanceException {
			var fxChangeListener = (ChangeListener<ColorScheme>) (_, _, newScheme) -> {
				var newTheme = switch (newScheme) {
					case DARK -> Theme.DARK;
					case LIGHT -> Theme.LIGHT;
				};
				uiAppearanceListener.systemAppearanceChanged(newTheme);
			};
			LOG.debug("Register listener for OS theme changes");
			uiAppearanceListeners.computeIfAbsent(uiAppearanceListener, k -> {
				Platform.runLater(() -> preferences.colorSchemeProperty().addListener(fxChangeListener));
				return fxChangeListener;
			});
		}

		@Override
		public void removeListener(UiAppearanceListener uiAppearanceListener) throws UiAppearanceException {
			var fxChangeListener = uiAppearanceListeners.remove(uiAppearanceListener);
			if (fxChangeListener != null) {
				LOG.debug("Removing listener for OS theme changes");
				Platform.runLater(() -> preferences.colorSchemeProperty().removeListener(fxChangeListener));
			}
		}
	}


	//just delegate methods
	@Override
	public Theme getSystemTheme() {
		var impl = realImpl.get();
		if (impl != null) {
			return impl.getSystemTheme();
		} else {
			return Theme.LIGHT;
		}
	}

	@Override
	public void adjustToTheme(Theme theme) {
		var impl = realImpl.get();
		if (impl != null) {
			impl.adjustToTheme(theme);
		}
	}

	@Override
	public void addListener(UiAppearanceListener uiAppearanceListener) throws UiAppearanceException {
		var impl = realImpl.get();
		if (impl != null) {
			impl.addListener(uiAppearanceListener);
		}
	}

	@Override
	public void removeListener(UiAppearanceListener uiAppearanceListener) throws UiAppearanceException {
		var impl = realImpl.get();
		if (impl != null) {
			impl.removeListener(uiAppearanceListener);
		}
	}

}
