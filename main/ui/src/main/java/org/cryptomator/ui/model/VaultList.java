package org.cryptomator.ui.model;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.ui.settings.Settings;
import org.cryptomator.ui.settings.VaultSettings;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.transformation.TransformationList;

@Singleton
public class VaultList extends TransformationList<Vault, VaultSettings> {

	private final VaultFactory vaultFactory;
	private final ObservableList<VaultSettings> source;

	@Inject
	public VaultList(Settings settings, VaultFactory vaultFactory) {
		this(FXCollections.observableList(settings.getDirectories()), settings, vaultFactory);
	}

	private VaultList(ObservableList<VaultSettings> source, Settings settings, VaultFactory vaultFactory) {
		super(source);
		this.source = source;
		this.vaultFactory = vaultFactory;
		addListener((Change<? extends Vault> change) -> settings.save());
	}

	@Override
	public int getSourceIndex(int index) {
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
	protected void sourceChanged(Change<? extends VaultSettings> c) {
		this.fireChange(new VaultListChange(c));
	}

	private class VaultListChange extends Change<Vault> {

		private final Change<? extends VaultSettings> delegate;

		public VaultListChange(Change<? extends VaultSettings> delegate) {
			super(VaultList.this);
			this.delegate = delegate;
		}

		@Override
		public boolean next() {
			return delegate.next();
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
			List<Vault> removed = new ArrayList<>();
			for (VaultSettings s : delegate.getRemoved()) {
				removed.add(vaultFactory.get(s));
			}
			return removed;
		}

		@Override
		protected int[] getPermutation() {
			if (delegate.wasPermutated()) {
				int len = getTo() - getFrom();
				int[] permutations = new int[len];
				for (int i = 0; i < len; i++) {
					permutations[i] = getPermutation(i);
				}
				return permutations;
			} else {
				return new int[0];
			}
		}

	}

}
