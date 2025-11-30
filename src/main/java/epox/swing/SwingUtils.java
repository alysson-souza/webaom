/*
 * Created on 29. juni. 2007 23.40.57
 * Filename: MyUtils.java
 */
package epox.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * Utility class for Swing-related helper methods.
 */
public final class SwingUtils {
	private SwingUtils() {
		// Prevent instantiation
	}

	/**
	 * Centers a component on the screen.
	 */
	public static void centerComponent(Component component) {
		Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle bounds = component.getBounds();
		int centeredX = screenSize.width / 2 - bounds.width / 2;
		int centeredY = screenSize.height / 2 - bounds.height / 2;
		component.setBounds(centeredX, centeredY, bounds.width, bounds.height);
	}
}
