import ch.qos.logback.classic.spi.Configurator;
import org.cryptomator.integrations.tray.TrayMenuController;
import org.cryptomator.logging.LogbackConfiguratorFactory;
import org.cryptomator.ui.traymenu.AwtTrayMenuController;

open module org.cryptomator.desktop {
	requires static org.jetbrains.annotations;

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
	requires ch.qos.logback.classic;
	requires ch.qos.logback.core;
	requires com.auth0.jwt;
	requires com.google.common;
	requires com.google.gson;
	requires com.nimbusds.jose.jwt;
	requires com.nulabinc.zxcvbn;
	requires com.tobiasdiez.easybind;
	requires dagger;
	requires io.github.coffeelibs.tinyoauth2client;
	requires org.slf4j;
	requires org.apache.commons.lang3;

	/* TODO: filename-based modules: */
	requires static javax.inject;

	provides TrayMenuController with AwtTrayMenuController;
	provides Configurator with LogbackConfiguratorFactory;
}