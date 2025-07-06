import ch.qos.logback.classic.spi.Configurator;
import org.cryptomator.networking.SSLContextWithPKCS12TrustStore;
import org.cryptomator.common.locationpresets.DropboxLinuxLocationPresetsProvider;
import org.cryptomator.common.locationpresets.DropboxMacLocationPresetsProvider;
import org.cryptomator.common.locationpresets.DropboxWindowsLocationPresetsProvider;
import org.cryptomator.common.locationpresets.GoogleDriveMacLocationPresetsProvider;
import org.cryptomator.common.locationpresets.GoogleDriveWindowsLocationPresetsProvider;
import org.cryptomator.common.locationpresets.ICloudMacLocationPresetsProvider;
import org.cryptomator.common.locationpresets.ICloudWindowsLocationPresetsProvider;
import org.cryptomator.common.locationpresets.LeitzcloudLocationPresetsProvider;
import org.cryptomator.common.locationpresets.LocationPresetsProvider;
import org.cryptomator.common.locationpresets.MegaLocationPresetsProvider;
import org.cryptomator.common.locationpresets.OneDriveLinuxLocationPresetsProvider;
import org.cryptomator.common.locationpresets.OneDriveMacLocationPresetsProvider;
import org.cryptomator.common.locationpresets.OneDriveWindowsLocationPresetsProvider;
import org.cryptomator.common.locationpresets.PCloudLocationPresetsProvider;
import org.cryptomator.networking.SSLContextWithMacKeychain;
import org.cryptomator.networking.SSLContextProvider;
import org.cryptomator.networking.SSLContextWithWindowsCertStore;
import org.cryptomator.integrations.tray.TrayMenuController;
import org.cryptomator.logging.LogbackConfiguratorFactory;
import org.cryptomator.ui.traymenu.AwtTrayMenuController;
import org.cryptomator.updater.MacOsDmgUpdateMechanism;
import org.cryptomator.updater.UpdateMechanism;

open module org.cryptomator.desktop {
	requires static org.jetbrains.annotations;

	requires org.cryptomator.cryptolib;
	requires org.cryptomator.cryptofs;
	requires org.cryptomator.frontend.fuse;
	requires org.cryptomator.frontend.webdav;
	requires org.cryptomator.integrations.api;
	// jdk:
	requires java.desktop;
	requires java.net.http;
	requires javafx.base;
	requires javafx.graphics;
	requires javafx.controls;
	requires javafx.fxml;
	// 3rd party:
	requires ch.qos.logback.classic;
	requires ch.qos.logback.core;
	requires com.auth0.jwt;
	requires com.google.common;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.datatype.jsr310;
	requires com.nimbusds.jose.jwt;
	requires com.nulabinc.zxcvbn;
	requires com.tobiasdiez.easybind;
	requires dagger;
	requires io.github.coffeelibs.tinyoauth2client;
	requires org.slf4j;
	requires org.apache.commons.lang3;

	/* dagger bs */
	requires jakarta.inject;
	requires static javax.inject;
	requires java.compiler;
	requires com.github.benmanes.caffeine;

	uses org.cryptomator.common.locationpresets.LocationPresetsProvider;
	uses SSLContextProvider;
	uses org.cryptomator.event.NotificationHandler;

	// opens org.cryptomator.updater to org.cryptomator.integrations.api;
	provides UpdateMechanism with MacOsDmgUpdateMechanism; // TODO: move to integrations-mac

	provides TrayMenuController with AwtTrayMenuController;
	provides Configurator with LogbackConfiguratorFactory;
	provides SSLContextProvider with SSLContextWithWindowsCertStore, SSLContextWithMacKeychain, SSLContextWithPKCS12TrustStore;
	provides LocationPresetsProvider with //
			DropboxWindowsLocationPresetsProvider, DropboxMacLocationPresetsProvider, DropboxLinuxLocationPresetsProvider, //
			GoogleDriveMacLocationPresetsProvider, GoogleDriveWindowsLocationPresetsProvider, //
			ICloudWindowsLocationPresetsProvider, ICloudMacLocationPresetsProvider, //
			LeitzcloudLocationPresetsProvider, //
			MegaLocationPresetsProvider, //
			OneDriveWindowsLocationPresetsProvider, OneDriveMacLocationPresetsProvider, OneDriveLinuxLocationPresetsProvider, //
			PCloudLocationPresetsProvider;
}