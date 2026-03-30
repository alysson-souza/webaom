/*
 * WebAOM - Web Anime-O-Matic
 * Copyright (C) 2005-2010 epoximator 2025 Alysson Souza
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <https://www.gnu.org/licenses/>.
 */

package epox.swing;

import java.awt.Color;
import javax.swing.UIManager;

public final class ThemeColorSupport {
    private static final double CONTRAST_ADJUSTMENT_STEP = 0.10;

    private ThemeColorSupport() {
        // static only
    }

    public static Color colorOrDefault(Color primary, Color fallback, Color defaultColor) {
        if (primary != null) {
            return primary;
        }
        if (fallback != null) {
            return fallback;
        }
        return defaultColor;
    }

    public static Color colorOrDefault(Color primary, Color defaultColor, String... uiKeys) {
        if (primary != null) {
            return primary;
        }
        for (String uiKey : uiKeys) {
            Color color = UIManager.getColor(uiKey);
            if (color != null) {
                return color;
            }
        }
        return defaultColor;
    }

    public static Color blend(Color start, Color end, double ratio) {
        double boundedRatio = Math.max(0.0, Math.min(1.0, ratio));
        int red = (int) Math.round((start.getRed() * (1.0 - boundedRatio)) + (end.getRed() * boundedRatio));
        int green = (int) Math.round((start.getGreen() * (1.0 - boundedRatio)) + (end.getGreen() * boundedRatio));
        int blue = (int) Math.round((start.getBlue() * (1.0 - boundedRatio)) + (end.getBlue() * boundedRatio));
        return new Color(red, green, blue);
    }

    public static Color ensureContrast(Color preferred, Color background, Color fallback, double minimumContrast) {
        if (contrastRatio(preferred, background) >= minimumContrast) {
            return preferred;
        }

        if (contrastRatio(fallback, background) >= minimumContrast) {
            for (double ratio = CONTRAST_ADJUSTMENT_STEP; ratio <= 1.0; ratio += CONTRAST_ADJUSTMENT_STEP) {
                Color adjusted = blend(preferred, fallback, ratio);
                if (contrastRatio(adjusted, background) >= minimumContrast) {
                    return adjusted;
                }
            }
            return fallback;
        }

        return pickHighestContrast(background, preferred, fallback, Color.white, Color.black);
    }

    public static Color pickHighestContrast(Color background, Color... candidates) {
        Color best = candidates[0];
        double bestContrast = contrastRatio(best, background);
        for (int index = 1; index < candidates.length; index++) {
            Color candidate = candidates[index];
            double contrast = contrastRatio(candidate, background);
            if (contrast > bestContrast) {
                best = candidate;
                bestContrast = contrast;
            }
        }
        return best;
    }

    public static double contrastRatio(Color foreground, Color background) {
        double lighter = Math.max(relativeLuminance(foreground), relativeLuminance(background));
        double darker = Math.min(relativeLuminance(foreground), relativeLuminance(background));
        return (lighter + 0.05) / (darker + 0.05);
    }

    public static String toHex(Color color) {
        return String.format("%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static double relativeLuminance(Color color) {
        double red = linearize(color.getRed() / 255.0);
        double green = linearize(color.getGreen() / 255.0);
        double blue = linearize(color.getBlue() / 255.0);
        return (0.2126 * red) + (0.7152 * green) + (0.0722 * blue);
    }

    private static double linearize(double channel) {
        if (channel <= 0.03928) {
            return channel / 12.92;
        }
        return Math.pow((channel + 0.055) / 1.055, 2.4);
    }
}
