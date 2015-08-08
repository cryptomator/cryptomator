package org.cryptomator.ui.settings;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.cryptomator.ui.util.DeferredCloser;

import com.fasterxml.jackson.databind.ObjectMapper;

import dagger.Module;

@Module
public class SettingsModule {

	@Singleton
	Provider<Settings> provideSettingsProvider(Provider<ObjectMapper> objectMapper) {
		return new SettingsProvider(new DeferredCloser(), objectMapper.get());
	}

}