/*
 * WebAOM - Web Anime-O-Matic
 * Copyright (C) 2005-2010 epoximator 2026 Alysson Souza
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

package epox.webaom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import epox.webaom.data.AniDBEntity;
import epox.webaom.ui.MainPanel;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JPanel;
import javax.swing.UIManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppContextCharacterizationTest {
    private static final String[] FONT_KEYS = {
        "Button.font",
        "CheckBox.font",
        "CheckBoxMenuItem.acceleratorFont",
        "CheckBoxMenuItem.font",
        "ComboBox.font",
        "DesktopIcon.font",
        "EditorPane.font",
        "FormattedTextField.font",
        "Label.font",
        "List.font",
        "Menu.font",
        "MenuItem.acceleratorFont",
        "MenuItem.font",
        "OptionPane.font",
        "Panel.font",
        "PasswordField.font",
        "PopupMenu.font",
        "ProgressBar.font",
        "RadioButton.font",
        "RadioButtonMenuItem.acceleratorFont",
        "RadioButtonMenuItem.font",
        "ScrollPane.font",
        "Slider.font",
        "TabbedPane.font",
        "Table.font",
        "TableHeader.font",
        "TextArea.font",
        "TextField.font",
        "TextPane.font",
        "TitledBorder.font",
        "ToggleButton.font",
        "ToolTip.font",
        "Tree.font",
        "Viewport.font"
    };

    @TempDir
    Path temporaryDirectory;

    private MainPanel previousGui;
    private Component previousComponent;
    private Frame previousFrame;
    private Component previousPrimaryPopup;
    private Component previousSecondaryPopup;
    private AppContext.AppMode previousMode;
    private Map<String, Object> previousFonts;

    @BeforeEach
    void setUp() {
        previousGui = AppContext.gui;
        previousComponent = AppContext.component;
        previousFrame = AppContext.frame;
        previousPrimaryPopup = AppContext.primaryPopupMenu;
        previousSecondaryPopup = AppContext.secondaryPopupMenu;
        previousMode = AppContext.getAppMode();
        previousFonts = captureUiDefaults(FONT_KEYS);
        AppContext.gui = null;
        AppContext.component = null;
        AppContext.frame = null;
        AppContext.primaryPopupMenu = null;
        AppContext.secondaryPopupMenu = null;
    }

    @AfterEach
    void tearDown() {
        AppContext.gui = previousGui;
        AppContext.component = previousComponent;
        AppContext.frame = previousFrame;
        AppContext.primaryPopupMenu = previousPrimaryPopup;
        AppContext.secondaryPopupMenu = previousSecondaryPopup;
        AppContext.setAppMode(previousMode);
        restoreUiDefaults(previousFonts);
    }

    @Test
    void appModeDefaultsNullToNormalAndBlocksOnlyForIncompatibleDatabases() {
        AppContext.setAppMode(AppContext.AppMode.INCOMPATIBLE_DATABASE);
        assertTrue(AppContext.isInteractionBlocked());
        assertEquals(AppContext.AppMode.INCOMPATIBLE_DATABASE, AppContext.getAppMode());

        AppContext.setAppMode(null);
        assertFalse(AppContext.isInteractionBlocked());
        assertEquals(AppContext.AppMode.NORMAL, AppContext.getAppMode());
    }

    @Test
    void headlessDialogWritesTheTitleAndMessageToStandardError() {
        AppContext.component = new JPanel();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        PrintStream previousErr = System.err;
        try {
            System.setErr(new PrintStream(errors, true, StandardCharsets.UTF_8));
            AppContext.dialog("Startup", "Configuration unavailable");
        } finally {
            System.setErr(previousErr);
        }

        assertEquals(
                "Startup: Configuration unavailable" + System.lineSeparator(), errors.toString(StandardCharsets.UTF_8));
    }

    @Test
    void shutdownWithoutOptionProcessingResetsAndStopsTheGui() {
        MainPanel gui = mock(MainPanel.class);
        AppContext.gui = gui;

        assertTrue(AppContext.shutdown(false));

        verify(gui).reset();
        verify(gui).shutdown();
    }

    @Test
    void configuredFontsUpdateSwingDefaultsAndNotifyTheGui() {
        MainPanel gui = mock(MainPanel.class);
        AppContext.gui = gui;

        AppContext.setFont("Monospaced,18");

        Font labelFont = UIManager.getFont("Label.font");
        assertEquals(Font.MONOSPACED, labelFont.getFamily());
        assertEquals(18, labelFont.getSize());
        assertEquals(18, UIManager.getFont("Table.font").getSize());
        verify(gui).scaleCurrentUi(anyDouble());

        AppContext.setFont("Dialog,broken", true);
        assertEquals(Font.DIALOG, UIManager.getFont("Label.font").getFamily());
        assertEquals(11, UIManager.getFont("Label.font").getSize());
        verify(gui).applyScaleAwareSizing();
    }

    @Test
    void blankFontConfigurationUsesTheLegacySansSerifDefault() {
        AppContext.setFont(null);

        Font labelFont = UIManager.getFont("Label.font");
        assertEquals(Font.SANS_SERIF, labelFont.getFamily());
        assertEquals(11, labelFont.getSize());
    }

    @Test
    void explicitTemplateTakesPriorityWithoutEvaluatingBundledFallback() throws Exception {
        Path template = Files.writeString(temporaryDirectory.resolve("template.htm"), "explicit template");
        AtomicBoolean fallbackRead = new AtomicBoolean(false);

        String loaded = AppContext.loadFileSchemaTemplate(template.toString(), null, () -> {
            fallbackRead.set(true);
            return "bundled";
        });

        assertEquals("explicit template", loaded);
        assertFalse(fallbackRead.get());
    }

    @Test
    void bundledResourcesAreReturnedAndMissingResourcesBecomeExceptionMessages() {
        assertTrue(AppContext.getFileString("file.htm").contains("<html"));

        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        PrintStream previousErr = System.err;
        String missing;
        try {
            System.setErr(new PrintStream(errors, true, StandardCharsets.UTF_8));
            missing = AppContext.getFileString("missing-webaom-resource.txt");
        } finally {
            System.setErr(previousErr);
        }

        // TODO: Review whether a missing bundled resource should return an exception message instead of null.
        assertNotNull(missing);
        assertFalse(missing.isBlank());
        assertFalse(errors.toString(StandardCharsets.UTF_8).isBlank());
    }

    @Test
    void deleteFileAndFolderRemovesOnlyTheNowEmptyParent() throws Exception {
        Path parent = Files.createDirectory(temporaryDirectory.resolve("empty-after-delete"));
        Path file = Files.writeString(parent.resolve("episode.tmp"), "temporary");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream previousOut = System.out;
        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            AppContext.deleteFileAndFolder(file.toFile(), "characterization");
        } finally {
            System.setOut(previousOut);
        }

        assertFalse(Files.exists(file));
        assertFalse(Files.exists(parent));
        assertTrue(Files.exists(temporaryDirectory));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("$ Deleted"));
    }

    @Test
    void filenameLimitDialogCurrentlyPropagatesHeadlessFailure() {
        assertTrue(GraphicsEnvironment.isHeadless());

        // TODO: Review whether headless callers should receive FILENAME_SKIP instead of HeadlessException.
        assertThrows(
                java.awt.HeadlessException.class,
                () -> AppContext.showFilenameTooLongDialog("x".repeat(80) + ".mkv", 300, 255));
    }

    @Test
    void bitComparisonRequiresEveryMaskedBit() {
        assertTrue(AppContext.bitcmp(0b1110, 0b0110));
        assertTrue(AppContext.bitcmp(0b1010, 0));
        assertFalse(AppContext.bitcmp(0b1010, 0b0110));
    }

    @Test
    void dumpStatsTraversesTheBuiltAnimeHierarchy() {
        assertEquals(0, AppContext.animeTreeRoot.size());
        JobList previousJobs = AppContext.jobs;
        Cache previousCache = AppContext.cache;
        AppContext.jobs = new JobList();
        AppContext.cache = new Cache();
        AniDBEntity anime = entity(1);
        AniDBEntity group = entity(2);
        AniDBEntity file = entity(3);
        group.add(file);
        anime.add(group);
        AppContext.animeTreeRoot.add(anime);
        AppContext.animeTreeRoot.buildSortedChildArray();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream previousOut = System.out;
        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            AppContext.dumpStats();
        } finally {
            System.setOut(previousOut);
            AppContext.animeTreeRoot.clear();
            AppContext.jobs = previousJobs;
            AppContext.cache = previousCache;
        }

        String stats = output.toString(StandardCharsets.UTF_8);
        assertTrue(stats.contains("@ JobList:"));
        assertTrue(stats.contains("@ Cache:"));
        assertTrue(stats.contains("@ Tree: 1, 1, 1"));
        assertEquals(0, AppContext.animeTreeRoot.size());
        assertSame(previousJobs, AppContext.jobs);
        assertSame(previousCache, AppContext.cache);
    }

    private static AniDBEntity entity(int id) {
        AniDBEntity entity = new AniDBEntity();
        entity.setId(id);
        return entity;
    }

    private static Map<String, Object> captureUiDefaults(String... keys) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : keys) {
            values.put(key, UIManager.get(key));
        }
        return values;
    }

    private static void restoreUiDefaults(Map<String, Object> values) {
        values.forEach((key, value) -> {
            if (value == null) {
                UIManager.getDefaults().remove(key);
            } else {
                UIManager.put(key, value);
            }
        });
    }
}
