/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import javax.inject.Singleton;

import org.cryptomator.common.CommonsModule;

import dagger.Component;

@Singleton
@Component(modules = {CommonsModule.class})
public interface WebDavComponent {

	WebDavServer server();

}