package org.cryptomator.ui.common;

import javafx.stage.Screen;

public class SystemBarUtil {

	public enum Placement {
		LEFT,
		TOP,
		RIGHT,
		BOTTOM;
	}

	public static Placement getPlacementOfSystembar(Screen screen) {
		var bounds = screen.getBounds();
		var vBounds = screen.getVisualBounds();
		//assumption: the system bar fills a whole screen side
		if(bounds.getMinX() != vBounds.getMinX()){
			return Placement.LEFT;
		} else if (bounds.getMinY() != vBounds.getMinY()) {
			return Placement.TOP;
		} else if (bounds.getMaxX() != vBounds.getMaxX()) {
			return Placement.RIGHT;
		} else {
			return Placement.BOTTOM;
		}
	}
}
