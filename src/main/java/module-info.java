import org.cryptomator.integrations.autostart.AutoStartProvider;
import org.cryptomator.integrations.keychain.KeychainAccessProvider;
import org.cryptomator.integrations.tray.TrayIntegrationProvider;
import org.cryptomator.integrations.uiappearance.UiAppearanceProvider;

module org.cryptomator.desktop {
	requires org.cryptomator.cryptolib;
	requires org.cryptomator.cryptofs;
	requires org.cryptomator.frontend.dokany;
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
	requires com.auth0.jwt;
	requires com.google.common;
	requires com.google.gson;
	requires com.nulabinc.zxcvbn;
	requires com.tobiasdiez.easybind;
	requires dagger;
	requires org.slf4j;
	requires org.apache.commons.lang3;
	requires org.eclipse.jetty.server;
	requires org.eclipse.jetty.webapp;
	requires org.eclipse.jetty.servlets;

	/* TODO: filename-based modules: */
	requires static javax.inject; /* ugly dagger/guava crap */
	requires logback.classic;
	requires logback.core;
	requires com.nimbusds.jose.jwt;

	uses AutoStartProvider;
	uses KeychainAccessProvider;
	uses TrayIntegrationProvider;
	uses UiAppearanceProvider;

	exports org.cryptomator.ui.keyloading.hub to com.fasterxml.jackson.databind;

	opens org.cryptomator.common.settings to com.google.gson;

	opens org.cryptomator.common to javafx.fxml;
	opens org.cryptomator.common.vaults to javafx.fxml;
	opens org.cryptomator.ui.addvaultwizard to javafx.fxml;
	opens org.cryptomator.ui.changepassword to javafx.fxml;
	opens org.cryptomator.ui.common to javafx.fxml;
	opens org.cryptomator.ui.controls to javafx.fxml;
	opens org.cryptomator.ui.forgetPassword to javafx.fxml;
	opens org.cryptomator.ui.fxapp to javafx.fxml;
	opens org.cryptomator.ui.health to javafx.fxml;
	opens org.cryptomator.ui.keyloading.hub to javafx.fxml;
	opens org.cryptomator.ui.keyloading.masterkeyfile to javafx.fxml;
	opens org.cryptomator.ui.lock to javafx.fxml;
	opens org.cryptomator.ui.mainwindow to javafx.fxml;
	opens org.cryptomator.ui.migration to javafx.fxml;
	opens org.cryptomator.ui.preferences to javafx.fxml;
	opens org.cryptomator.ui.quit to javafx.fxml;
	opens org.cryptomator.ui.recoverykey to javafx.fxml;
	opens org.cryptomator.ui.removevault to javafx.fxml;
	opens org.cryptomator.ui.stats to javafx.fxml;
	opens org.cryptomator.ui.unlock to javafx.fxml;
	opens org.cryptomator.ui.vaultoptions to javafx.fxml;
	opens org.cryptomator.ui.wrongfilealert to javafx.fxml;
}