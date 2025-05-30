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
		settings.theme.addListener(this::appThemeChanged);
		loadSelectedStyleSheet(settings.theme.get());
	}

	private void appThemeChanged(@SuppressWarnings("unused") ObservableValue<? extends UiTheme> observable, @SuppressWarnings("unused") UiTheme oldValue, UiTheme newValue) {
		if (appearanceProvider.isPresent() && oldValue == UiTheme.AUTOMATIC && newValue != UiTheme.AUTOMATIC) {
			try {
				appearanceProvider.get().removeListener(systemInterfaceThemeListener);
			} catch (UiAppearanceException e) {
				LOG.error("Failed to disable automatic theme switching.");
			}
		}
		loadSelectedStyleSheet(newValue);
	}

	private void loadSelectedStyleSheet(UiTheme desiredTheme) {
		UiTheme theme = licenseHolder.isValidLicense() ? desiredTheme : UiTheme.LIGHT;
		switch (theme) {
			case LIGHT -> applyLightTheme();
			case DARK -> applyDarkTheme();
			case AUTOMATIC -> {
				appearanceProvider.ifPresent(provider -> {
					try {
						provider.addListener(systemInterfaceThemeListener);
					} catch (UiAppearanceException e) {
						LOG.error("Failed to enable automatic theme switching.");
					}
				});
				applySystemTheme();
			}
		}
	}

	private void systemInterfaceThemeChanged(Theme theme) {
		switch (theme) {
			case LIGHT -> applyLightTheme();
			case DARK -> applyDarkTheme();
		}
	}

	private void applySystemTheme() {
		if (appearanceProvider.isPresent()) {
			systemInterfaceThemeChanged(appearanceProvider.get().getSystemTheme());
		} else {
			LOG.warn("No UiAppearanceProvider present, assuming LIGHT theme...");
			applyLightTheme();
		}
	}

	private void applyLightTheme() {
		var stylesheet = Optional //
				.ofNullable(getClass().getResource("/css/light_theme.bss")) //
				.orElse(getClass().getResource("/css/light_theme.css"));
		if (stylesheet == null) {
			LOG.warn("Failed to load light_theme stylesheet");
		} else {
			Application.setUserAgentStylesheet(stylesheet.toString());
			appearanceProvider.ifPresent(provider -> provider.adjustToTheme(Theme.LIGHT));
			appliedTheme.set(Theme.LIGHT);
		}
	}

	private void applyDarkTheme() {
		var stylesheet = Optional //
				.ofNullable(getClass().getResource("/css/dark_theme.bss")) //
				.orElse(getClass().getResource("/css/dark_theme.css"));
		if (stylesheet == null) {
			LOG.warn("Failed to load dark_theme stylesheet");
		} else {
			Application.setUserAgentStylesheet(stylesheet.toString());
			appearanceProvider.ifPresent(provider -> provider.adjustToTheme(Theme.DARK));
			appliedTheme.set(Theme.DARK);
		}
	}

	public ObjectProperty<Theme> appliedThemeProperty() {
		return appliedTheme;
	}
}
