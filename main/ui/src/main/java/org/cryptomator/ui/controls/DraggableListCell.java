/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.controls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderImage;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

class DraggableListCell<T> extends ListCell<T> {

	private static final double DROP_LINE_WIDTH = 4.0;
	private static final Paint DROP_LINE_COLOR = Color.gray(0.0, 0.6);

	private final List<BorderStroke> defaultBorderStrokes;
	private final List<BorderImage> defaultBorderImages;

	public DraggableListCell() {
		setOnDragDetected(this::onDragDetected);
		setOnDragOver(this::onDragOver);
		setOnDragEntered(this::onDragEntered);
		setOnDragExited(this::onDragExited);
		setOnDragDropped(this::onDragDropped);
		setOnDragDone(DragEvent::consume);
		this.defaultBorderStrokes = this.getBorder() == null ? Collections.emptyList() : this.getBorder().getStrokes();
		this.defaultBorderImages = this.getBorder() == null ? Collections.emptyList() : this.getBorder().getImages();
	}

	private Border createDropPositionBorder(double verticalCursorPosition) {
		boolean isUpperHalf = verticalCursorPosition < this.getHeight() / 2.0;
		final double topBorder = isUpperHalf ? DROP_LINE_WIDTH : 0.0;
		final double bottomBorder = !isUpperHalf ? DROP_LINE_WIDTH : 0.0;
		final BorderWidths borderWidths = new BorderWidths(topBorder, 0.0, bottomBorder, 0.0);
		final BorderStroke dragStroke = new BorderStroke(DROP_LINE_COLOR, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, borderWidths, Insets.EMPTY);
		final List<BorderStroke> strokes = new ArrayList<BorderStroke>(defaultBorderStrokes);
		strokes.add(0, dragStroke);
		return new Border(strokes, defaultBorderImages);
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
			setBorder(createDropPositionBorder(event.getY()));
		}

		event.consume();
	}

	private void onDragEntered(DragEvent event) {
		if (getItem() == null) {
			return;
		}

		if (event.getGestureSource() instanceof DraggableListCell<?> && event.getGestureSource() != this && event.getDragboard().hasString()) {
			setBorder(createDropPositionBorder(event.getY()));
		}
	}

	private void onDragExited(DragEvent event) {
		if (getItem() == null) {
			return;
		}

		if (event.getGestureSource() instanceof DraggableListCell<?> && event.getGestureSource() != this && event.getDragboard().hasString()) {
			setBorder(new Border(defaultBorderStrokes, defaultBorderImages));
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
