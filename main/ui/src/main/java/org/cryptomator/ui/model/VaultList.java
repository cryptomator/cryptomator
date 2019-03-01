/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.model;

import com.google.common.collect.Lists;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.TransformationList;
import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.IntStream;

@FxApplicationScoped
public class VaultList extends TransformationList<Vault, VaultSettings> {

	private final VaultFactory vaultFactory;
	private final ObservableList<VaultSettings> source;

	@Inject
	public VaultList(Settings settings, VaultFactory vaultFactory) {
		super(settings.getDirectories());
		this.source = settings.getDirectories();
		this.vaultFactory = vaultFactory;
	}

	@Override
	public int getSourceIndex(int index) {
		return index;
	}

	@Override
	public int getViewIndex(int index) {
		return index;
	}

	@Override
	public Vault get(int index) {
		VaultSettings s = source.get(index);
		return vaultFactory.get(s);
	}

	@Override
	public void add(int index, Vault element) {
		source.add(index, element.getVaultSettings());
	}

	@Override
	public Vault remove(int index) {
		VaultSettings s = source.remove(index);
		return vaultFactory.get(s);
	}

	@Override
	public int size() {
		return getSource().size();
	}

	@Override
	protected void sourceChanged(ListChangeListener.Change<? extends VaultSettings> c) {
		this.fireChange(new VaultListChange(c));
	}

	private class VaultListChange extends ListChangeListener.Change<Vault> {

		private final ListChangeListener.Change<? extends VaultSettings> delegate;

		public VaultListChange(ListChangeListener.Change<? extends VaultSettings> delegate) {
			super(VaultList.this);
			this.delegate = delegate;
		}

		@Override
		public boolean next() {
			return delegate.next();
		}

		@Override
		public boolean wasUpdated() {
			return delegate.wasUpdated();
		}

		@Override
		public void reset() {
			delegate.reset();
		}

		@Override
		public int getFrom() {
			return delegate.getFrom();
		}

		@Override
		public int getTo() {
			return delegate.getTo();
		}

		@Override
		public List<Vault> getRemoved() {
			return Lists.transform(delegate.getRemoved(), vaultFactory::get);
		}

		@Override
		public boolean wasPermutated() {
			return delegate.wasPermutated();
		}

		@Override
		protected int[] getPermutation() {
			if (delegate.wasPermutated()) {
				return IntStream.range(getFrom(), getTo()).map(delegate::getPermutation).toArray();
			} else {
				return new int[0];
			}
		}

		@Override
		public String toString() {
			return delegate.toString();
		}

	}

}
