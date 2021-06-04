/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.controls;

import com.tobiasdiez.easybind.EasyBind;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import java.util.List;

public class DraggableListCell<T> extends ListCell<T> {

	private static final String DROP_ABOVE_CLASS = "drop-above";
	private static final String DROP_BELOW_CLASS = "drop-below";

	private final BooleanProperty dropAbove = new SimpleBooleanProperty();
	private final BooleanProperty dropBelow = new SimpleBooleanProperty();

	public DraggableListCell() {
		setOnDragDetected(this::onDragDetected);
		setOnDragOver(this::onDragOver);
		setOnDragEntered(this::onDragEntered);
		setOnDragExited(this::onDragExited);
		setOnDragDropped(this::onDragDropped);
		setOnDragDone(DragEvent::consume);

		EasyBind.includeWhen(getStyleClass(), DROP_ABOVE_CLASS, dropAbove);
		EasyBind.includeWhen(getStyleClass(), DROP_BELOW_CLASS, dropBelow);
	}

	private void setDropPositionStyleClass(double verticalCursorPosition) {
		boolean isUpperHalf = verticalCursorPosition < this.getHeight() / 2.0;
		if (isUpperHalf) {
			this.dropAbove.set(true);
			this.dropBelow.set(false);
		} else {
			this.dropAbove.set(false);
			this.dropBelow.set(true);
		}
	}

	private void resetDropPositionStyleClasses() {
		this.dropAbove.set(false);
		this.dropBelow.set(false);
	}

	private void onDragDetected(MouseEvent event) {
		if (getItem() == null) {
			return;
		}

		final ClipboardContent content = new ClipboardContent();
		content.putString(Integer.toString(getIndex()));
		final Image snapshot = this.snapshot(new SnapshotParameters(), null);
		final Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
		dragboard.setDragView(snapshot);
		dragboard.setContent(content);

		event.consume();
	}

	private void onDragOver(DragEvent event) {
		if (getItem() == null) {
			return;
		}

		if (event.getGestureSource() instanceof DraggableListCell<?> && event.getGestureSource() != this && event.getDragboard().hasString()) {
			event.acceptTransferModes(TransferMode.MOVE);
			setDropPositionStyleClass(event.getY());
		}

		event.consume();
	}

	private void onDragEntered(DragEvent event) {
		if (getItem() == null) {
			return;
		}

		if (event.getGestureSource() instanceof DraggableListCell<?> && event.getGestureSource() != this && event.getDragboard().hasString()) {
			setDropPositionStyleClass(event.getY());
		}
	}

	private void onDragExited(DragEvent event) {
		if (getItem() == null) {
			return;
		}

		if (event.getGestureSource() instanceof DraggableListCell<?> && event.getGestureSource() != this && event.getDragboard().hasString()) {
			resetDropPositionStyleClasses();
		}
	}

	private void onDragDropped(DragEvent event) {
		if (getItem() == null) {
			return;
		}

		if (event.getGestureSource() instanceof DraggableListCell<?> && event.getDragboard().hasString()) {
			final List<T> list = getListView().getItems();
			try {
				// where to insert what?
				final int draggedIdx = Integer.parseInt(event.getDragboard().getString());
				final T currentItem = this.getItem();
				final T draggedItem = list.remove(draggedIdx);
				final int currentItemIdx = list.indexOf(currentItem);

				// insert before or after currentItem?
				boolean insertBefore = event.getY() < this.getHeight() / 2.0;
				final int insertPosition = insertBefore ? currentItemIdx : currentItemIdx + 1;

				// insert!
				getListView().getItems().add(insertPosition, draggedItem);
				getListView().getSelectionModel().select(insertPosition);
				event.setDropCompleted(true);
			} catch (NumberFormatException e) {
				event.setDropCompleted(false);
			}
		}

		event.consume();
	}
}
