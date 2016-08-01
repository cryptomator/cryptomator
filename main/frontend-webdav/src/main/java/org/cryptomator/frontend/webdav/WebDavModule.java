package org.cryptomator.frontend.webdav;

import org.cryptomator.common.CommonsModule;
import org.cryptomator.frontend.webdav.mount.WebDavMounterModule;

import dagger.Module;

@Module(includes = {CommonsModule.class, WebDavMounterModule.class})
public class WebDavModule {

}
