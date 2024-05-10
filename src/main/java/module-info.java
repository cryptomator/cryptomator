import ch.qos.logback.classic.spi.Configurator;
import org.cryptomator.common.locationpresets.DropboxLinuxLocationPresetsProvider;
import org.cryptomator.common.locationpresets.DropboxMacLocationPresetsProvider;
import org.cryptomator.common.locationpresets.DropboxWindowsLocationPresetsProvider;
import org.cryptomator.common.locationpresets.GoogleDriveLocationPresetsProvider;
import org.cryptomator.common.locationpresets.ICloudMacLocationPresetsProvider;
import org.cryptomator.common.locationpresets.ICloudWindowsLocationPresetsProvider;
import org.cryptomator.common.locationpresets.LeitzcloudLocationPresetsProvider;
import org.cryptomator.common.locationpresets.LocationPresetsProvider;
import org.cryptomator.common.locationpresets.MegaLocationPresetsProvider;
import org.cryptomator.common.locationpresets.OneDriveLinuxLocationPresetsProvider;
import org.cryptomator.common.locationpresets.OneDriveMacLocationPresetsProvider;
import org.cryptomator.common.locationpresets.OneDriveWindowsLocationPresetsProvider;
import org.cryptomator.common.locationpresets.PCloudLocationPresetsProvider;
import org.cryptomator.integrations.tray.TrayMenuController;
import org.cryptomator.logging.LogbackConfiguratorFactory;
import org.cryptomator.ui.traymenu.AwtTrayMenuController;

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
	requires jdk.crypto.ec;
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

	/* TODO: filename-based modules: */
	requires static javax.inject; /* ugly dagger/guava crap */

	uses org.cryptomator.common.locationpresets.LocationPresetsProvider;

	provides TrayMenuController with AwtTrayMenuController;
	provides Configurator with LogbackConfiguratorFactory;
	provides LocationPresetsProvider with //
			DropboxWindowsLocationPresetsProvider, DropboxMacLocationPresetsProvider, DropboxLinuxLocationPresetsProvider, //
			GoogleDriveLocationPresetsProvider, //
			ICloudWindowsLocationPresetsProvider, ICloudMacLocationPresetsProvider, //
			LeitzcloudLocationPresetsProvider, //
			MegaLocationPresetsProvider, //
			OneDriveWindowsLocationPresetsProvider, OneDriveMacLocationPresetsProvider, OneDriveLinuxLocationPresetsProvider, //
			PCloudLocationPresetsProvider;
}