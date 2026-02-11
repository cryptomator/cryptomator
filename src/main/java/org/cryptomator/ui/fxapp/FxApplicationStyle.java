package org.cryptomator.ui.fxapp;

import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.integrations.uiappearance.Theme;
import org.cryptomator.integrations.uiappearance.UiAppearanceException;
import org.cryptomator.integrations.uiappearance.UiAppearanceListener;
import org.cryptomator.integrations.uiappearance.UiAppearanceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import java.util.Optional;

@FxApplicationScoped
public class FxApplicationStyle {

	private static final Logger LOG = LoggerFactory.getLogger(FxApplicationStyle.class);

	private final Settings settings;
	private final Optional<UiAppearanceProvider> appearanceProvider;
	private final LicenseHolder licenseHolder;
	private final UiAppearanceListener systemInterfaceThemeListener = this::systemInterfaceThemeChanged;
	private final ObjectProperty<Theme> appliedTheme = new SimpleObjectProperty<>(Theme.LIGHT);

	@Inject
	public FxApplicationStyle(Settings settings, Optional<UiAppearanceProvider> appearanceProvider, LicenseHolder licenseHolder) {
		this.settings = settings;
		this.appearanceProvider = appearanceProvider;
		this.licenseHolder = licenseHolder;
	}

	public void initialize() {
		appearanceProvider.ifPresent(service -> {
			if (service instanceof JfxUiAppearanceProvider fxService) {
				fxService.initialize(Platform.getPreferences());
			}
		});
		applyTheme(settings.theme.get());
		settings.theme.addListener(this::appThemeChanged);
	}

	private void appThemeChanged(@SuppressWarnings("unused") ObservableValue<? extends UiTheme> observable, UiTheme oldValue, UiTheme newValue) {
		if (oldValue == UiTheme.AUTOMATIC && newValue != UiTheme.AUTOMATIC) {
			appearanceProvider.ifPresent(service -> {
				try {
					service.removeListener(systemInterfaceThemeListener);
				} catch (UiAppearanceException e) {
					LOG.warn("Failed to disable automatic theme switching.");
				}
			});
		}
		applyTheme(newValue);
	}

	private void applyTheme(UiTheme theme) {
		if (!licenseHolder.isValidLicense()) {
			loadAndApplyLightTheme();
		} else {
			switch (theme) {
				case AUTOMATIC -> registerAutomaticThemeChange();
				case LIGHT -> loadAndApplyLightTheme();
				case DARK -> loadAndApplyDarkTheme();
			}
		}
	}

	private void registerAutomaticThemeChange() {
		appearanceProvider.ifPresentOrElse(provider -> {
					try {
						provider.addListener(systemInterfaceThemeListener);
					} catch (UiAppearanceException e) {
						LOG.error("Failed to enable automatic theme switching.");
					}
					systemInterfaceThemeChanged(provider.getSystemTheme());
				}, //
				() -> LOG.warn("UI theme AUTOMATIC selected, but no supported UiAppearanceProvider present"));
	}

	private void systemInterfaceThemeChanged(Theme osTheme) {
		switch (osTheme) {
			case LIGHT -> loadAndApplyLightTheme();
			case DARK -> loadAndApplyDarkTheme();
		}
	}

	private void loadAndApplyLightTheme() {
		loadAndApplyTheme(Theme.LIGHT, "/css/light_theme.css");
	}

	private void loadAndApplyDarkTheme() {
		loadAndApplyTheme(Theme.DARK, "/css/dark_theme.css");
	}

	private void loadAndApplyTheme(Theme appTheme, String cssFile) {
		var stylesheet = getClass().getResource(cssFile);
		if (stylesheet == null) {
			throw new IllegalStateException("Cannot find resource %s".formatted(cssFile));
		}
		Application.setUserAgentStylesheet(stylesheet.toString());
		appearanceProvider.ifPresent(provider -> provider.adjustToTheme(appTheme));
		appliedTheme.set(appTheme);
	}

	public ObjectProperty<Theme> appliedAppThemeProperty() {
		return appliedTheme;
	}
}
