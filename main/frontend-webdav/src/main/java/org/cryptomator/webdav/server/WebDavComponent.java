package org.cryptomator.webdav.server;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component
public interface WebDavComponent {

	WebDavServer server();

}