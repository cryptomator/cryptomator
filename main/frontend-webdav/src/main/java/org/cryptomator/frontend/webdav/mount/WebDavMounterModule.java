package org.cryptomator.frontend.webdav.mount;

import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.collect.Sets;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;

@Module
public class WebDavMounterModule {

	@Provides
	@ElementsIntoSet
	static Set<WebDavMounterStrategy> provideMounters(LinuxGvfsWebDavMounter linuxWebDavMounter, LinuxGvfsDavMounter linuxDavMounter, MacOsXAppleScriptWebDavMounter osxAppleScriptMounter,
			MacOsXShellScriptWebDavMounter osxShellScriptMounter, WindowsWebDavMounter winMounter) {
		return Sets.newHashSet(linuxWebDavMounter, linuxDavMounter, osxAppleScriptMounter, osxShellScriptMounter, winMounter);
	}

	@Provides
	@Singleton
	@Named("fallback")
	static WebDavMounterStrategy provideFallbackStrategy() {
		return new FallbackWebDavMounter();
	}

}
