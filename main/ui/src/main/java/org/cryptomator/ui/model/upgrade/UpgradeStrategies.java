/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.model.upgrade;

import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.ui.model.Vault;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

@FxApplicationScoped
public class UpgradeStrategies {

	private final Collection<UpgradeStrategy> strategies;

	@Inject
	public UpgradeStrategies(UpgradeVersion3DropBundleExtension upgrader1, UpgradeVersion3to4 upgrader2, UpgradeVersion4to5 upgrader3, UpgradeVersion5toX upgrader4) {
		strategies = Collections.unmodifiableList(Arrays.asList(upgrader1, upgrader2, upgrader3, upgrader4));
	}

	public UpgradeStrategy getUpgradeStrategy(Vault vault) {
		Objects.requireNonNull(vault);
		return strategies.stream().filter(strategy -> {
			return strategy.isApplicable(vault);
		}).findFirst().orElse(null);
	}

}
