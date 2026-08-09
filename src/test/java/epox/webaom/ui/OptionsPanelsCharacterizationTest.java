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

package epox.webaom.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.swing.FlatLafTheme;
import epox.webaom.DiskIOManager;
import epox.webaom.Options;
import epox.webaom.data.Mylist;
import epox.webaom.data.MylistStates;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.text.JTextComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OptionsPanelsCharacterizationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void miscOptionsRoundTripFieldsFlagsThemesAndChecksumSelection() {
        Options source = options("misc-source");
        source.setBoolean(Options.BOOL_HASH_CRC, true);
        source.setBoolean(Options.BOOL_HASH_MD5, true);
        source.setBoolean(Options.BOOL_HASH_SHA, false);
        source.setBoolean(Options.BOOL_HASH_TTH, true);
        source.setBoolean(Options.BOOL_STORE_PASSWORD, true);
        source.setBoolean(Options.BOOL_AUTO_LOAD_DATABASE, true);
        source.setBoolean(Options.BOOL_AUTO_LOG, true);
        source.setBoolean(Options.BOOL_AUTO_SAVE, true);
        source.setBoolean(Options.BOOL_AUTO_RENAME, false);
        source.setString(Options.STR_HASH_DIRECTORY, "/one;/two");
        source.setString(Options.STR_BROWSER, "/usr/bin/browser");
        source.setString(Options.STR_DATABASE_URL, "jdbc:sqlite:test.db");
        source.setString(Options.STR_LOG_FILE, "/tmp/webaom.log");
        source.setString(Options.STR_THEME_LIGHT, FlatLafTheme.CATPPUCCIN_LATTE.getOptionValue());
        source.setString(Options.STR_THEME_DARK, FlatLafTheme.CATPPUCCIN_MOCHA.getOptionValue());
        MiscOptionsPanel panel = new MiscOptionsPanel();

        panel.loadFromOptions(source);

        assertTrue(panel.isStorePasswordEnabled());
        assertTrue(panel.isAutoLoadDatabaseEnabled());
        assertTrue(panel.isAutoLogEnabled());
        assertArrayEquals(new File[] {new File("/one"), new File("/two")}, panel.getHashDirectories());
        assertEquals(FlatLafTheme.CATPPUCCIN_LATTE, panel.getThemeComboBox().getLightTheme());
        assertEquals(FlatLafTheme.CATPPUCCIN_MOCHA, panel.getThemeComboBox().getDarkTheme());

        LinkedHashMap<String, DiskIOManager.ChecksumData> first = panel.createChecksums();
        LinkedHashMap<String, DiskIOManager.ChecksumData> second = panel.getChecksums();
        assertEquals(List.of("ed2k", "crc32", "md5", "tth"), new ArrayList<>(first.keySet()));
        assertEquals(first.keySet(), second.keySet());
        assertFalse(first.get("ed2k") == second.get("ed2k"));

        Options saved = options("misc-saved");
        panel.saveToOptions(saved);
        assertTrue(saved.getBoolean(Options.BOOL_HASH_CRC));
        assertTrue(saved.getBoolean(Options.BOOL_HASH_MD5));
        assertFalse(saved.getBoolean(Options.BOOL_HASH_SHA));
        assertTrue(saved.getBoolean(Options.BOOL_HASH_TTH));
        assertFalse(saved.getBoolean(Options.BOOL_AUTO_RENAME));
        assertEquals("/usr/bin/browser", saved.getString(Options.STR_BROWSER));
        assertEquals("jdbc:sqlite:test.db", saved.getString(Options.STR_DATABASE_URL));
        assertEquals("/tmp/webaom.log", saved.getString(Options.STR_LOG_FILE));
    }

    @Test
    void miscEnabledStateControlsHashInputsButLeavesPanelAndEd2kStateStable() {
        MiscOptionsPanel panel = new MiscOptionsPanel();

        panel.setEnabled(false);

        // TODO: Review whether setEnabled should also update the panel's own enabled property.
        assertTrue(panel.isEnabled());
        assertFalse(panel.hashDirectoriesField.isEnabled());
        assertFalse(panel.hashCheckBoxes[MiscOptionsPanel.HASH_CRC32].isEnabled());
        assertFalse(panel.hashCheckBoxes[MiscOptionsPanel.HASH_ED2K].isEnabled());

        panel.setEnabled(true);
        assertTrue(panel.hashDirectoriesField.isEnabled());
        assertTrue(panel.hashCheckBoxes[MiscOptionsPanel.HASH_CRC32].isEnabled());
        assertFalse(panel.hashCheckBoxes[MiscOptionsPanel.HASH_ED2K].isEnabled());
    }

    @Test
    void connectionOptionsClampLoadedTimingValuesAndSaveCurrentFields() {
        ConnectionOptionsPanel panel = new ConnectionOptionsPanel();
        Options low = options("connection-low");
        low.setString(Options.STR_HOST_URL, "localhost");
        low.setInteger(Options.INT_REMOTE_PORT, 9001);
        low.setInteger(Options.INT_LOCAL_PORT, 45679);
        low.setInteger(Options.INT_TIMEOUT, 1);
        low.setInteger(Options.INT_DATAGRAM_DELAY, 1);
        low.setBoolean(Options.BOOL_NAT_KEEP_ALIVE, true);

        panel.loadOptions(low);

        assertEquals(10, panel.getTimeout());
        assertEquals(3000, panel.getDelayMillis());
        assertTrue(panel.isNatKeepAliveEnabled());
        assertEquals("localhost", panel.hostTextField.getText());
        assertEquals("9001", panel.remotePortTextField.getText());
        assertEquals("45679", panel.localPortTextField.getText());

        Options high = options("connection-high");
        high.setString(Options.STR_HOST_URL, "api.example");
        high.setInteger(Options.INT_REMOTE_PORT, 1234);
        high.setInteger(Options.INT_LOCAL_PORT, 5678);
        high.setInteger(Options.INT_TIMEOUT, 100);
        high.setInteger(Options.INT_DATAGRAM_DELAY, 100);
        panel.loadOptions(high);
        assertEquals(60, panel.getTimeout());
        assertEquals(10_000, panel.getDelayMillis());

        Options saved = options("connection-saved");
        panel.saveOptions(saved);
        assertEquals("api.example", saved.getString(Options.STR_HOST_URL));
        assertEquals(1234, saved.getInteger(Options.INT_REMOTE_PORT));
        assertEquals(5678, saved.getInteger(Options.INT_LOCAL_PORT));
        assertEquals(60, saved.getInteger(Options.INT_TIMEOUT));
        assertEquals(10, saved.getInteger(Options.INT_DATAGRAM_DELAY));
    }

    @Test
    void connectionPortFieldsConsumeNonNumericTypedCharacters() {
        ConnectionOptionsPanel panel = new ConnectionOptionsPanel();

        KeyEvent letter = typed(panel.remotePortTextField, 'x');
        for (KeyListener listener : panel.remotePortTextField.getKeyListeners()) {
            listener.keyTyped(letter);
        }
        KeyEvent digit = typed(panel.localPortTextField, '7');
        for (KeyListener listener : panel.localPortTextField.getKeyListeners()) {
            listener.keyTyped(digit);
        }

        assertTrue(letter.isConsumed());
        assertFalse(digit.isConsumed());
    }

    @Test
    void connectionEnabledStateLeavesKeepAliveControlEnabled() {
        ConnectionOptionsPanel panel = new ConnectionOptionsPanel();
        JCheckBox keepAlive = components(panel, JCheckBox.class).getFirst();

        panel.setEnabled(false);

        assertFalse(panel.hostTextField.isEnabled());
        assertFalse(panel.remotePortTextField.isEnabled());
        assertFalse(panel.localPortTextField.isEnabled());
        assertFalse(panel.pingButton.isEnabled());
        // TODO: Review whether disabling connection settings should also disable Keep-Alive.
        assertTrue(keepAlive.isEnabled());

        JSlider timeout = components(panel, JSlider.class).stream()
                .filter(slider -> slider.getMaximum() == 60)
                .findFirst()
                .orElseThrow();
        timeout.setValue(33);
        assertEquals("33 sec", timeout.getToolTipText());
    }

    @Test
    void mylistOptionsRoundTripAndProduceAniDbPayload() {
        Options source = options("mylist-source");
        source.setBoolean(Options.BOOL_WATCHED, true);
        source.setInteger(Options.INT_FILE_STATE, MylistStates.LOCATION_REMOTE);
        source.setString(Options.STR_STORAGE, "NAS");
        source.setString(Options.STR_SOURCE_FOLDER, "Usenet");
        source.setString(Options.STR_OTHER_INFO, "Archived");
        MylistOptionsPanel panel = new MylistOptionsPanel();

        panel.loadFromOptions(source);
        Mylist mylist = panel.getMylistData();

        assertEquals(MylistStates.LOCATION_REMOTE, mylist.state);
        assertEquals(1, mylist.viewed);
        assertEquals("NAS", mylist.storage);
        assertEquals("Usenet", mylist.source);
        assertEquals("Archived", mylist.other);

        Options saved = options("mylist-saved");
        panel.saveToOptions(saved);
        assertTrue(saved.getBoolean(Options.BOOL_WATCHED));
        assertEquals(MylistStates.LOCATION_REMOTE, saved.getInteger(Options.INT_FILE_STATE));
        assertEquals("NAS", saved.getString(Options.STR_STORAGE));
        assertEquals("Usenet", saved.getString(Options.STR_SOURCE_FOLDER));
        assertEquals("Archived", saved.getString(Options.STR_OTHER_INFO));
    }

    @Test
    void mylistPanelDisablesEveryEditableControlAndItself() {
        MylistOptionsPanel panel = new MylistOptionsPanel();

        panel.setEnabled(false);

        assertFalse(panel.isEnabled());
        for (JTextComponent textComponent : components(panel, JTextComponent.class)) {
            assertFalse(textComponent.isEnabled());
        }
        for (JCheckBox checkBox : components(panel, JCheckBox.class)) {
            assertFalse(checkBox.isEnabled());
        }
        for (JComboBox<?> comboBox : components(panel, JComboBox.class)) {
            assertFalse(comboBox.isEnabled());
        }
    }

    private Options options(String name) {
        return new Options(temporaryDirectory.resolve(name));
    }

    private static KeyEvent typed(Component source, char character) {
        return new KeyEvent(source, KeyEvent.KEY_TYPED, 1L, 0, KeyEvent.VK_UNDEFINED, character);
    }

    private static <T extends Component> List<T> components(Container root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                matches.add(type.cast(component));
            }
            if (component instanceof Container container) {
                matches.addAll(components(container, type));
            }
        }
        return matches;
    }
}
