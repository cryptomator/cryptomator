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
	public UpgradeStrategies(UpgradeVersion3DropBundleExtension upgrader1, UpgradeVersion3to4 upgrader2) {
		strategies = Collections.unmodifiableList(Arrays.asList(upgrader1, upgrader2));
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
