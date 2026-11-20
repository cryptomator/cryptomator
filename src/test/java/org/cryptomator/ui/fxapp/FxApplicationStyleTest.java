package org.cryptomator.ui.fxapp;

import org.cryptomator.common.Environment;
import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.SettingsProvider;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.integrations.uiappearance.Theme;
import org.cryptomator.integrations.uiappearance.UiAppearanceException;
import org.cryptomator.integrations.uiappearance.UiAppearanceListener;
import org.cryptomator.integrations.uiappearance.UiAppearanceProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.util.Optional;

class FxApplicationStyleTest {

	private final LicenseHolder licenseHolder = Mockito.mock(LicenseHolder.class);
	private final UiAppearanceProvider appearanceProvider = Mockito.mock(UiAppearanceProvider.class);
	private final BooleanProperty licenseValid = new SimpleBooleanProperty(false);
	private Settings settings;
	private FxApplicationStyle appStyle;
	private MockedStatic<Application> application;
	private String appliedStylesheet;

	@BeforeEach
	public void setup() {
		settings = Settings.create(Mockito.mock(SettingsProvider.class), Mockito.mock(Environment.class));
		var validLicenseBinding = Bindings.createBooleanBinding(licenseValid::get, licenseValid);
		Mockito.when(licenseHolder.validLicenseProperty()).thenReturn(validLicenseBinding);
		Mockito.when(licenseHolder.isValidLicense()).thenAnswer(_ -> licenseValid.get());
		Mockito.when(appearanceProvider.getSystemTheme()).thenReturn(Theme.LIGHT);
		application = Mockito.mockStatic(Application.class);
		application.when(() -> Application.setUserAgentStylesheet(Mockito.anyString())).thenAnswer(invocation -> {
			appliedStylesheet = invocation.getArgument(0);
			return null;
		});
	}

	@AfterEach
	public void tearDown() {
		application.close();
	}

	private void initialize(UiTheme preference, boolean licensed, Optional<UiAppearanceProvider> provider) {
		settings.theme.set(preference);
		licenseValid.set(licensed);
		appStyle = new FxApplicationStyle(settings, provider, licenseHolder);
		appStyle.initialize();
	}

	private UiAppearanceListener registeredOsListener() throws UiAppearanceException {
		var listener = ArgumentCaptor.forClass(UiAppearanceListener.class);
		Mockito.verify(appearanceProvider).addListener(listener.capture());
		return listener.getValue();
	}

	private void assertAppliedTheme(Theme expected) {
		Assertions.assertEquals(expected, appStyle.appliedAppThemeProperty().get());
		String resource = expected == Theme.DARK ? "/css/dark_theme.css" : "/css/light_theme.css";
		Assertions.assertEquals(getClass().getResource(resource).toString(), appliedStylesheet);
	}

	@Test
	public void testUnlicensedAutomaticThemeRejectsOsDarkCallback() throws UiAppearanceException {
		initialize(UiTheme.AUTOMATIC, false, Optional.of(appearanceProvider));
		var listener = registeredOsListener();
		assertAppliedTheme(Theme.LIGHT);

		listener.systemAppearanceChanged(Theme.DARK);

		assertAppliedTheme(Theme.LIGHT);
	}

	@Test
	public void testLicensedAutomaticThemeFollowsOsChanges() throws UiAppearanceException {
		Mockito.when(appearanceProvider.getSystemTheme()).thenReturn(Theme.DARK);
		initialize(UiTheme.AUTOMATIC, true, Optional.of(appearanceProvider));
		var listener = registeredOsListener();
		assertAppliedTheme(Theme.DARK);

		listener.systemAppearanceChanged(Theme.LIGHT);
		assertAppliedTheme(Theme.LIGHT);
		listener.systemAppearanceChanged(Theme.DARK);
		assertAppliedTheme(Theme.DARK);
	}

	@ParameterizedTest
	@EnumSource(value = UiTheme.class, names = {"LIGHT", "DARK"})
	public void testStaleOsCallbackDoesNotOverrideManualTheme(UiTheme manualTheme) throws UiAppearanceException {
		initialize(UiTheme.AUTOMATIC, true, Optional.of(appearanceProvider));
		var listener = registeredOsListener();
		settings.theme.set(manualTheme);
		Theme expected = manualTheme == UiTheme.DARK ? Theme.DARK : Theme.LIGHT;
		Theme staleTheme = expected == Theme.DARK ? Theme.LIGHT : Theme.DARK;
		assertAppliedTheme(expected);

		listener.systemAppearanceChanged(staleTheme);

		assertAppliedTheme(expected);
	}

	@ParameterizedTest
	@EnumSource(value = UiTheme.class, names = {"DARK", "AUTOMATIC"})
	public void testLicenseRevocationImmediatelyAppliesLightTheme(UiTheme preference) {
		Mockito.when(appearanceProvider.getSystemTheme()).thenReturn(Theme.DARK);
		initialize(preference, true, Optional.of(appearanceProvider));
		assertAppliedTheme(Theme.DARK);

		licenseValid.set(false);

		assertAppliedTheme(Theme.LIGHT);
		Assertions.assertEquals(preference, settings.theme.get());
	}

	@ParameterizedTest
	@EnumSource(value = UiTheme.class, names = {"DARK", "AUTOMATIC"})
	public void testLicenseGrantAppliesRequestedTheme(UiTheme preference) {
		Mockito.when(appearanceProvider.getSystemTheme()).thenReturn(Theme.DARK);
		initialize(preference, false, Optional.of(appearanceProvider));
		assertAppliedTheme(Theme.LIGHT);

		licenseValid.set(true);

		assertAppliedTheme(Theme.DARK);
		Assertions.assertEquals(preference, settings.theme.get());
	}

	@Test
	public void testAutomaticThemeWithoutAppearanceProviderFallsBackToLight() {
		initialize(UiTheme.AUTOMATIC, true, Optional.empty());
		assertAppliedTheme(Theme.LIGHT);

		licenseValid.set(false);
		licenseValid.set(true);

		assertAppliedTheme(Theme.LIGHT);
	}
}
