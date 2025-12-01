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

package epox.webaom.ui;

import epox.swing.JComboBoxLF;
import epox.util.TTH;
import epox.webaom.AppContext;
import epox.webaom.DiskIOManager;
import epox.webaom.Options;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class MiscOptionsPanel extends JPanel {
    public static final int HASH_ED2K = 0;
    public static final int HASH_CRC32 = 1;
    public static final int HASH_MD5 = 2;
    public static final int HASH_SHA1 = 3;
    public static final int HASH_TTH = 4;
    private final JCheckBox storePasswordCheckBox;
    private final JCheckBox autoLoadDatabaseCheckBox;
    private final JCheckBox autoLogCheckBox;
    private final JCheckBox autoSaveCheckBox;
    private final JCheckBox autoRenameCheckBox;
    public JCheckBox[] hashCheckBoxes = new JCheckBox[5];
    public JTextField newExtensionField;
    public JTextField hashDirectoriesField;
    public JTextField browserPathField;
    public JTextField databaseUrlField;
    public JButton disconnectButton;
    public JTextField logFilePathField;

    public MiscOptionsPanel() {
        super(new GridBagLayout());

        hashDirectoriesField = new JTextField();
        browserPathField = new JTextField();
        databaseUrlField = new JTextField();
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setToolTipText("Disconnect from database");
        disconnectButton.setEnabled(false);
        logFilePathField = new JTextField();

        hashDirectoriesField.setToolTipText("Check these directories for new files every now and then");
        browserPathField.setToolTipText("Absolute path to preferred browser");
        databaseUrlField.setToolTipText(
                "JDBC url (e.g. jdbc:postgresql://localhost:5432/webaom?user=root&password=pass)."
                        + " If empty, embedded SQLite database will be used. Press enter to connect.");
        logFilePathField.setToolTipText("Absolute path to log file, press enter to enable");

        autoLoadDatabaseCheckBox = new JCheckBox("Auto db");
        autoLoadDatabaseCheckBox.setToolTipText("Load db on startup");
        autoLogCheckBox = new JCheckBox("Auto log");
        autoLogCheckBox.setToolTipText("Start logging to disk on startup");
        autoSaveCheckBox = new JCheckBox("Auto save");
        autoSaveCheckBox.setToolTipText("Save options on exit without asking");
        autoRenameCheckBox = new JCheckBox("Auto rename");
        autoRenameCheckBox.setToolTipText("Automatically rename/move files after identification");
        storePasswordCheckBox = new JCheckBox("Store password");
        storePasswordCheckBox.setToolTipText("Do you want to store your password on disk? (not cleartext)");

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 4, 2, 4);
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weighty = 0.1;

        addLabeledComponent("Hash Dirs", hashDirectoriesField, constraints);
        addLabeledComponent("Browser Path", browserPathField, constraints);

        // Database row with disconnect button
        JPanel databasePanel = new JPanel(new GridBagLayout());
        GridBagConstraints dbConstraints = new GridBagConstraints();
        dbConstraints.fill = GridBagConstraints.HORIZONTAL;
        dbConstraints.weightx = 1.0;
        databasePanel.add(databaseUrlField, dbConstraints);
        dbConstraints.weightx = 0;
        dbConstraints.insets = new Insets(0, 4, 0, 0);
        databasePanel.add(disconnectButton, dbConstraints);
        addLabeledComponent("My Database", databasePanel, constraints);

        addLabeledComponent("Log File", logFilePathField, constraints);
        addLabeledComponent("LookAndFeel", new JComboBoxLF(AppContext.component), constraints);

        constraints.gridwidth = 1;

        JPanel hashOptionsPanel = new JPanel(new GridBagLayout());
        for (int i = 0; i < hashCheckBoxes.length; i++) {
            hashCheckBoxes[i] = new JCheckBox(getHashName(i), false);
            hashOptionsPanel.add(hashCheckBoxes[i], constraints);
        }
        hashCheckBoxes[HASH_ED2K].setSelected(true);
        hashCheckBoxes[HASH_ED2K].setEnabled(false);

        JPanel booleanOptionsPanel = new JPanel(new GridBagLayout());
        booleanOptionsPanel.add(autoLoadDatabaseCheckBox, constraints);
        booleanOptionsPanel.add(autoLogCheckBox, constraints);
        booleanOptionsPanel.add(autoSaveCheckBox, constraints);
        booleanOptionsPanel.add(autoRenameCheckBox, constraints);
        booleanOptionsPanel.add(storePasswordCheckBox, constraints);

        constraints.gridwidth = GridBagConstraints.REMAINDER;

        add(booleanOptionsPanel, constraints);
        add(hashOptionsPanel, constraints);
    }

    private static String getHashName(int hashTypeCode) {
        switch (hashTypeCode) {
            case HASH_ED2K:
                return "ed2k";
            case HASH_CRC32:
                return "crc32";
            case HASH_MD5:
                return "md5";
            case HASH_SHA1:
                return "sha1";
            case HASH_TTH:
                return "tth";
            default:
                return "NOT HASH";
        }
    }

    private void addLabeledComponent(String labelText, Component component, GridBagConstraints constraints) {
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        add(new JLabel(labelText), constraints);
        constraints.weightx = 1.0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        add(component, constraints);
    }

    public boolean isAutoLoadDatabaseEnabled() {
        return autoLoadDatabaseCheckBox.isSelected();
    }

    public boolean isAutoLogEnabled() {
        return autoLogCheckBox.isSelected();
    }

    public boolean isStorePasswordEnabled() {
        return storePasswordCheckBox.isSelected();
    }

    public File[] getHashDirectories() {
        StringTokenizer tokenizer = new StringTokenizer(hashDirectoriesField.getText(), ";");
        File[] directories = new File[tokenizer.countTokens()];
        for (int i = 0; i < directories.length; i++) {
            directories[i] = new File(tokenizer.nextToken());
        }
        return directories;
    }

    public void setEnabled(boolean enabled) {
        hashDirectoriesField.setEnabled(enabled);
        for (int i = 1; i < hashCheckBoxes.length; i++) {
            hashCheckBoxes[i].setEnabled(enabled);
        }
    }

    public void saveToOptions(Options options) {
        options.setBoolean(Options.BOOL_HASH_CRC, hashCheckBoxes[HASH_CRC32].isSelected());
        options.setBoolean(Options.BOOL_HASH_MD5, hashCheckBoxes[HASH_MD5].isSelected());
        options.setBoolean(Options.BOOL_HASH_SHA, hashCheckBoxes[HASH_SHA1].isSelected());
        options.setBoolean(Options.BOOL_HASH_TTH, hashCheckBoxes[HASH_TTH].isSelected());
        options.setBoolean(Options.BOOL_STORE_PASSWORD, storePasswordCheckBox.isSelected());
        options.setBoolean(Options.BOOL_AUTO_LOAD_DATABASE, autoLoadDatabaseCheckBox.isSelected());
        options.setBoolean(Options.BOOL_AUTO_LOGIN, autoLogCheckBox.isSelected());
        options.setBoolean(Options.BOOL_AUTO_SAVE, autoSaveCheckBox.isSelected());
        options.setBoolean(Options.BOOL_AUTO_RENAME, autoRenameCheckBox.isSelected());

        options.setString(Options.STR_HASH_DIRECTORY, hashDirectoriesField.getText());
        options.setString(Options.STR_BROWSER, browserPathField.getText());
        options.setString(Options.STR_DATABASE_URL, databaseUrlField.getText());
        options.setString(Options.STR_LOG_FILE, logFilePathField.getText());
    }

    public void loadFromOptions(Options options) {
        hashCheckBoxes[HASH_CRC32].setSelected(options.getBoolean(Options.BOOL_HASH_CRC));
        hashCheckBoxes[HASH_MD5].setSelected(options.getBoolean(Options.BOOL_HASH_MD5));
        hashCheckBoxes[HASH_SHA1].setSelected(options.getBoolean(Options.BOOL_HASH_SHA));
        hashCheckBoxes[HASH_TTH].setSelected(options.getBoolean(Options.BOOL_HASH_TTH));
        storePasswordCheckBox.setSelected(options.getBoolean(Options.BOOL_STORE_PASSWORD));
        autoLoadDatabaseCheckBox.setSelected(options.getBoolean(Options.BOOL_AUTO_LOAD_DATABASE));
        autoLogCheckBox.setSelected(options.getBoolean(Options.BOOL_AUTO_LOGIN));
        autoSaveCheckBox.setSelected(options.getBoolean(Options.BOOL_AUTO_SAVE));
        autoRenameCheckBox.setSelected(options.getBoolean(Options.BOOL_AUTO_RENAME));

        hashDirectoriesField.setText(options.getString(Options.STR_HASH_DIRECTORY));
        browserPathField.setText(options.getString(Options.STR_BROWSER));
        databaseUrlField.setText(options.getString(Options.STR_DATABASE_URL));
        logFilePathField.setText(options.getString(Options.STR_LOG_FILE));
    }

    public LinkedHashMap<String, DiskIOManager.ChecksumData> getChecksums() {
        try {
            LinkedHashMap<String, DiskIOManager.ChecksumData> checksums = new LinkedHashMap<>();

            if (hashCheckBoxes[HASH_ED2K].isSelected()) {
                checksums.put("ed2k", new DiskIOManager.ChecksumData("ed2k", new jonelo.jacksum.algorithm.Edonkey()));
            }
            if (hashCheckBoxes[HASH_CRC32].isSelected()) {
                checksums.put("crc32", new DiskIOManager.ChecksumData("crc32", new jonelo.jacksum.algorithm.Crc32()));
            }
            if (hashCheckBoxes[HASH_MD5].isSelected()) {
                checksums.put("md5", new DiskIOManager.ChecksumData("md5", new com.twmacinta.util.MD5()));
            }
            if (hashCheckBoxes[HASH_SHA1].isSelected()) {
                checksums.put("sha1", new DiskIOManager.ChecksumData("sha1", new jonelo.jacksum.algorithm.MD("SHA-1")));
            }
            if (hashCheckBoxes[HASH_TTH].isSelected()) {
                checksums.put("tth", new DiskIOManager.ChecksumData("tth", new TTH()));
            }
            return checksums;
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
