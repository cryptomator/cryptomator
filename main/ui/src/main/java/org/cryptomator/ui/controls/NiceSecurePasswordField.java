package org.cryptomator.ui.controls;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class NiceSecurePasswordField extends StackPane {

	private static final String STYLE_CLASS = "nice-secure-password-field";
	private static final String ICONS_STLYE_CLASS = "icons";
	private static final String REVEAL_BUTTON_STLYE_CLASS = "reveal-button";
	private static final int ICON_SPACING = 6;
	private static final double ICON_SIZE = 14.0;

	private final SecurePasswordField passwordField = new SecurePasswordField();
	private final FontAwesome5IconView capsLockedIcon = new FontAwesome5IconView();
	private final FontAwesome5IconView nonPrintableCharsIcon = new FontAwesome5IconView();
	private final FontAwesome5IconView revealPasswordIcon = new FontAwesome5IconView();
	private final ToggleButton revealPasswordButton = new ToggleButton(null, revealPasswordIcon);
	private final HBox iconContainer = new HBox(ICON_SPACING, nonPrintableCharsIcon, capsLockedIcon, revealPasswordButton);

	public NiceSecurePasswordField() {
		getStyleClass().add(STYLE_CLASS);

		iconContainer.setAlignment(Pos.CENTER_RIGHT);
		iconContainer.setMaxWidth(Double.NEGATIVE_INFINITY);
		iconContainer.setPrefWidth(42); // TODO
		iconContainer.getStyleClass().add(ICONS_STLYE_CLASS);
		StackPane.setAlignment(iconContainer, Pos.CENTER_RIGHT);

		capsLockedIcon.setGlyph(FontAwesome5Icon.ARROW_UP);
		capsLockedIcon.setGlyphSize(ICON_SIZE);
		capsLockedIcon.visibleProperty().bind(passwordField.capsLockedProperty());
		capsLockedIcon.managedProperty().bind(passwordField.capsLockedProperty());

		nonPrintableCharsIcon.setGlyph(FontAwesome5Icon.EXCLAMATION_TRIANGLE);
		nonPrintableCharsIcon.setGlyphSize(ICON_SIZE);
		nonPrintableCharsIcon.visibleProperty().bind(passwordField.containingNonPrintableCharsProperty());
		nonPrintableCharsIcon.managedProperty().bind(passwordField.containingNonPrintableCharsProperty());

		revealPasswordIcon.setGlyph(FontAwesome5Icon.EYE);
		revealPasswordIcon.glyphProperty().bind(Bindings.createObjectBinding(this::getRevealPasswordGlyph, revealPasswordButton.selectedProperty()));
		revealPasswordIcon.setGlyphSize(ICON_SIZE);

		revealPasswordButton.setContentDisplay(ContentDisplay.LEFT);
		revealPasswordButton.setFocusTraversable(false);
		revealPasswordButton.visibleProperty().bind(passwordField.focusedProperty());
		revealPasswordButton.managedProperty().bind(passwordField.focusedProperty());
		revealPasswordButton.getStyleClass().add(REVEAL_BUTTON_STLYE_CLASS);

		passwordField.revealPasswordProperty().bind(revealPasswordButton.selectedProperty());

		getChildren().addAll(passwordField, iconContainer);
		disabledProperty().addListener(this::disabledChanged);
	}

	private FontAwesome5Icon getRevealPasswordGlyph() {
		return revealPasswordButton.isSelected() ? FontAwesome5Icon.EYE_SLASH : FontAwesome5Icon.EYE;
	}

	private void disabledChanged(@SuppressWarnings("unused") Observable observable) {
		revealPasswordButton.setSelected(false);
	}

	/* Passthrough */

	@Override
	public void requestFocus() {
		passwordField.requestFocus();
	}

	public String getText() {
		return passwordField.getText();
	}

	public StringProperty textProperty() {
		return passwordField.textProperty();
	}

	public CharSequence getCharacters() {
		return passwordField.getCharacters();
	}

	public void setPassword(CharSequence password) {
		passwordField.setPassword(password);
	}

	public void setPassword(char[] password) {
		passwordField.setPassword(password);
	}

	public void swipe() {
		passwordField.swipe();
	}

	public void selectAll() {
		passwordField.selectAll();
	}

	public void selectRange(int anchor, int caretPosition) {
		passwordField.selectRange(anchor, caretPosition);
	}
}
