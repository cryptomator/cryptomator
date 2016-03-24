/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui;

import java.util.concurrent.ExecutorService;

import javax.inject.Singleton;

import org.cryptomator.ui.controllers.MainController;
import org.cryptomator.ui.settings.Localization;
import org.cryptomator.ui.util.DeferredCloser;

import dagger.Component;

@Singleton
@Component(modules = CryptomatorModule.class)
interface CryptomatorComponent {
	ExecutorService executorService();

	DeferredCloser deferredCloser();

	MainController mainController();

	Localization localization();

	ExitUtil exitUtil();
}