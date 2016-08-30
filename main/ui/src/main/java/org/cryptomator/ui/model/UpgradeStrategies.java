package org.cryptomator.ui.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UpgradeStrategies {

	private final Collection<UpgradeStrategy> strategies;

	@Inject
	public UpgradeStrategies(UpgradeVersion3DropBundleExtension upgrader1, UpgradeVersion3to4 upgrader2, UpgradeVersion4to5 upgrader3) {
		strategies = Collections.unmodifiableList(Arrays.asList(upgrader1, upgrader2, upgrader3));
	}

	public Optional<UpgradeStrategy> getUpgradeStrategy(Vault vault) {
		if (vault == null) {
			return Optional.empty();
		}
		return strategies.stream().filter(strategy -> {
			return strategy.isApplicable(vault);
		}).findFirst();
	}

}
