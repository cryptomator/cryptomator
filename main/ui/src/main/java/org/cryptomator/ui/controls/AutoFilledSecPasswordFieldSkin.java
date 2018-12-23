package org.cryptomator.ui.controls;

import javafx.event.EventHandler;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.input.KeyEvent;

public class AutoFilledSecPasswordFieldSkin extends TextFieldSkin {

    /**
     * Defines the length of the masked password to be displayed in
     * place of the real password.
     */
    private static final int AUTOFILLED_PASSWORD_MASK_LENGTH = 50;
    /**
     * Identical to {@link TextFieldSkin#BULLET TextFieldSkin.BULLET}.
     */
    private static final char BULLET = '\u25cf';

    /**
     * Creates a new AutoFilledSecPasswordFieldSkin instance, that makes it appear
     * as if the password field contains a password with the length of
     * {@link AutoFilledSecPasswordFieldSkin#AUTOFILLED_PASSWORD_MASK_LENGTH}. On
     * a key press, the password field is swiped and this skin is removed, so that the
     * auto-filled password's length is never disclosed and a newly entered password's
     * length is properly displayed.
     *
     * @param passwordField The password field that this skin should be installed onto.
     */
    public AutoFilledSecPasswordFieldSkin(SecPasswordField passwordField) {
        super(passwordField);
        passwordField.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<>() {
            @Override
            public void handle(KeyEvent event) {
                passwordField.swipe();
                passwordField.removeEventHandler(KeyEvent.KEY_PRESSED, this);
                dispose();
            }
        });
    }

    @Override
    protected String maskText(String txt) {
        int n = AUTOFILLED_PASSWORD_MASK_LENGTH;
        StringBuilder passwordBuilder = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            passwordBuilder.append(BULLET);
        }
        return passwordBuilder.toString();
    }
}
