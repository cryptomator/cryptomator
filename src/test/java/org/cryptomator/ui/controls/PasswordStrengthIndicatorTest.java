package org.cryptomator.ui.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.scene.layout.Region;

public class PasswordStrengthIndicatorTest {

    private PasswordStrengthIndicator indicator;

    @BeforeEach
    public void setUp() {
        indicator = new PasswordStrengthIndicator();
    }

    @Test
    public void testInitialization() {
        // Verify that 5 children are present
        assertEquals(5, indicator.getChildren().size());
        
        // Verify that all children are Regions
        indicator.getChildren().forEach(node -> {
            assertTrue(node instanceof Region);
        });
    }

    @Test
    public void testDefaultState() {
        // Check initial strength
        assertEquals(0, indicator.getStrength());

        // Check initial style class
        assertTrue(indicator.getStyleClass().contains(PasswordStrengthIndicator.STRENGTH_0_CLASS));
    }

    @Test
    public void testStrengthChangeTo1() {
        // Change strength to 1
        indicator.setStrength(1);

        // Check style class updates
        assertTrue(indicator.getStyleClass().contains(PasswordStrengthIndicator.STRENGTH_1_CLASS));
        assertTrue(indicator.getChildren().get(0).getStyleClass().contains(PasswordStrengthIndicator.ACTIVE_SEGMENT_CLASS));
    }

    @Test
    public void testStrengthChangeTo2() {
        indicator.setStrength(2);

        // Check style class updates
        assertTrue(indicator.getStyleClass().contains(PasswordStrengthIndicator.STRENGTH_2_CLASS));
        assertTrue(indicator.getChildren().get(0).getStyleClass().contains(PasswordStrengthIndicator.ACTIVE_SEGMENT_CLASS));
        assertTrue(indicator.getChildren().get(1).getStyleClass().contains(PasswordStrengthIndicator.ACTIVE_SEGMENT_CLASS));
    }

    @Test
    public void testStrengthChangeTo3() {
        indicator.setStrength(3);

        assertTrue(indicator.getStyleClass().contains(PasswordStrengthIndicator.STRENGTH_3_CLASS));
        assertTrue(indicator.getChildren().get(0).getStyleClass().contains(PasswordStrengthIndicator.ACTIVE_SEGMENT_CLASS));
        assertTrue(indicator.getChildren().get(1).getStyleClass().contains(PasswordStrengthIndicator.ACTIVE_SEGMENT_CLASS));
        assertTrue(indicator.getChildren().get(2).getStyleClass().contains(PasswordStrengthIndicator.ACTIVE_SEGMENT_CLASS));
    }

    @Test
    public void testStrengthChangeTo4() {
        indicator.setStrength(4);

        assertTrue(indicator.getStyleClass().contains(PasswordStrengthIndicator.STRENGTH_4_CLASS));
        assertTrue(indicator.getChildren().get(0).getStyleClass().contains(PasswordStrengthIndicator.ACTIVE_SEGMENT_CLASS));
        assertTrue(indicator.getChildren().get(1).getStyleClass().contains(PasswordStrengthIndicator.ACTIVE_SEGMENT_CLASS));
        assertTrue(indicator.getChildren().get(2).getStyleClass().contains(PasswordStrengthIndicator.ACTIVE_SEGMENT_CLASS));
        assertTrue(indicator.getChildren().get(3).getStyleClass().contains(PasswordStrengthIndicator.ACTIVE_SEGMENT_CLASS));
        assertTrue(indicator.getChildren().get(4).getStyleClass().contains(PasswordStrengthIndicator.ACTIVE_SEGMENT_CLASS));
    }

    @Test
    public void testStrengthChangeOutOfRange() {
        // Negative value
        indicator.setStrength(-1);
        assertEquals(-1, indicator.getStrength());

        // Value greater than 4
        indicator.setStrength(5);
        assertEquals(5, indicator.getStrength());
    }
}
