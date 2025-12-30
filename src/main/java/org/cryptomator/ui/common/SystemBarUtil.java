package org.cryptomator.ui.common;

import javafx.stage.Screen;

/**
 * Utility class providing methods regarding the OS bar.
 */
public class SystemBarUtil {

	public enum Placement {
		/**
		 * OS Bar placed at the left screen edge
		 */
		LEFT,
		/**
		 * OS Bar placed at the top screen edge
		 */
		TOP,
		/**
		 * OS Bar placed at the right screen edge
		 */
		RIGHT,
		/**
		 * OS Bar placed at the bottom screen edge
		 */
		BOTTOM;
	}

	/**
	 * Determines the placement of the OS bar on the given screen.
	 * <p>
	 * <b>Assuming the OS bar fills one screen edge completely</b>,
	 * this method determines that screen edge by comparing the actual screen bounds with the visual ones.
	 * <p>
	 * If the screen does not have a system bar, the bottom placement is returned.
	 * If the screen does have multiple system bars, the first in following priority is returned:
	 * LEFT, TOP, RIGHT, BOTTOM.
	 *
	 * @param screen a {@link Screen} where an OS bar exists
	 * @return {@link Placement} indicating the screen edge.
	 */
	public static Placement getPlacementOfSystembar(Screen screen) {
		var bounds = screen.getBounds();
		var vBounds = screen.getVisualBounds();
		//assumption: the system bar fills a whole screen side
		if (bounds.getMinX() != vBounds.getMinX()) {
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
