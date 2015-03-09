/*******************************************************************************
 * Copyright (c) 2014 cryptomator.org
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *    Sebastian Stenzel - initial implementation
 ******************************************************************************/
package org.cryptomator.ui.util;

import java.util.Collection;

import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

/**
 * From the moment on, this aggregator is added as an observer to one or many {@link ObservableSet}s, change-events will be passed through
 * to the given aggregation.
 */
public class ObservableSetAggregator<E> implements SetChangeListener<E> {

	private final Collection<E> aggregation;

	/**
	 * @param aggregation Set to which elements from observed subsets shall be added.
	 */
	public ObservableSetAggregator(final Collection<E> aggregation) {
		this.aggregation = aggregation;
	}

	@Override
	public void onChanged(Change<? extends E> change) {
		if (change.getSet() == aggregation) {
			// break cycle if aggregator observes aggregation
			return;
		}
		if (change.wasAdded()) {
			aggregation.add(change.getElementAdded());
		} else if (change.wasRemoved()) {
			aggregation.remove(change.getElementRemoved());
		}
	}

}
