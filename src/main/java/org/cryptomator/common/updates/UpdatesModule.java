package org.cryptomator.common.updates;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.integrations.update.UpdateService;

import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Module
public class UpdatesModule {

	@Provides
	@Singleton
	static List<UpdateService> provideSupportedUpdateServices() {
		return UpdateService.get().toList();
	}

	@Provides
	@Singleton
	static Optional<UpdateService> provideUpdateService(List<UpdateService> updateServices) {
		return updateServices.stream().findFirst();
	}
}
