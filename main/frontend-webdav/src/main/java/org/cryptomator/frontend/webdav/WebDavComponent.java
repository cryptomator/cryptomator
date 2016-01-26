package org.cryptomator.frontend.webdav;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component
public interface WebDavComponent {

	WebDavServer server();

}