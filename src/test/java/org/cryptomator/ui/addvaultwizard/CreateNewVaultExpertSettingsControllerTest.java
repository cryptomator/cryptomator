package org.cryptomator.ui.addvaultwizard;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class CreateNewVaultExpertSettingsControllerTest {

	@Test
	public void testScryptCostIsValidWithinExpertRange() {
		IntegerProperty cost = new SimpleIntegerProperty(AddVaultModule.provideScryptCostParam().get());
		var controller = new CreateNewVaultExpertSettingsController(null, null, null, null,
				new SimpleStringProperty("vault"), new SimpleObjectProperty<>(Path.of("vault")), new SimpleIntegerProperty(220), cost);

		Assertions.assertEquals(1 << 17, cost.get());
		Assertions.assertTrue(controller.isValidExpertSettings());
		cost.set(1 << 15);
		Assertions.assertTrue(controller.isValidExpertSettings());
		cost.set(1 << 18);
		Assertions.assertTrue(controller.isValidExpertSettings());
	}

	@Test
	public void testScryptCostIsInvalidOutsideExpertRange() {
		IntegerProperty cost = new SimpleIntegerProperty(1 << 17);
		var controller = new CreateNewVaultExpertSettingsController(null, null, null, null,
				new SimpleStringProperty("vault"), new SimpleObjectProperty<>(Path.of("vault")), new SimpleIntegerProperty(220), cost);

		cost.set(0);
		Assertions.assertFalse(controller.isValidExpertSettings());
		cost.set((1 << 17) + 1);
		Assertions.assertFalse(controller.isValidExpertSettings());
		cost.set(1 << 19);
		Assertions.assertFalse(controller.isValidExpertSettings());
	}

}
