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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

public final class DialogHelper {
    static final String CLOSE_ON_ESCAPE_ACTION_KEY = "dialog.closeOnEscape";

    private DialogHelper() {
        // utility class
    }

    public static void bindEscapeToClose(JRootPane rootPane, Runnable closeAction) {
        Objects.requireNonNull(rootPane);
        Objects.requireNonNull(closeAction);

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CLOSE_ON_ESCAPE_ACTION_KEY);
        actionMap.put(CLOSE_ON_ESCAPE_ACTION_KEY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                closeAction.run();
            }
        });
    }

    public static void showMessageDialog(Component parentComponent, Object message, String title, int messageType) {
        invokeOnEventDispatchThread(() -> {
            JOptionPane optionPane = new JOptionPane(message, messageType);
            JDialog dialog = createDialog(optionPane, parentComponent, title);
            dialog.setVisible(true);
            return null;
        });
    }

    public static int showOptionDialog(
            Component parentComponent,
            Object message,
            String title,
            int optionType,
            int messageType,
            Icon icon,
            Object[] options,
            Object initialValue) {
        return invokeOnEventDispatchThread(() -> {
            JOptionPane optionPane = new JOptionPane(message, messageType, optionType, icon, options, initialValue);
            JDialog dialog = createDialog(optionPane, parentComponent, title);
            dialog.setVisible(true);
            return resolveOptionResult(optionPane.getValue(), options);
        });
    }

    public static String showInputDialog(Component parentComponent, Object message, Object initialSelectionValue) {
        return invokeOnEventDispatchThread(() -> {
            JOptionPane optionPane =
                    new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            optionPane.setWantsInput(true);
            optionPane.setInitialSelectionValue(initialSelectionValue);

            String title = UIManager.getString("OptionPane.inputDialogTitle", optionPane.getLocale());
            JDialog dialog = createDialog(optionPane, parentComponent, title);
            dialog.setVisible(true);

            Object inputValue = optionPane.getInputValue();
            if (inputValue == null || inputValue == JOptionPane.UNINITIALIZED_VALUE) {
                return null;
            }
            return inputValue.toString();
        });
    }

    static int resolveOptionResult(Object selectedValue, Object[] options) {
        if (selectedValue == null || selectedValue == JOptionPane.UNINITIALIZED_VALUE) {
            return JOptionPane.CLOSED_OPTION;
        }
        if (options == null) {
            return selectedValue instanceof Integer integerValue ? integerValue : JOptionPane.CLOSED_OPTION;
        }
        for (int index = 0; index < options.length; index++) {
            if (selectedValue.equals(options[index])) {
                return index;
            }
        }
        return JOptionPane.CLOSED_OPTION;
    }

    private static JDialog createDialog(JOptionPane optionPane, Component parentComponent, String title) {
        JDialog dialog = optionPane.createDialog(parentComponent, title);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        bindEscapeToClose(dialog.getRootPane(), dialog::dispose);
        return dialog;
    }

    private static <T> T invokeOnEventDispatchThread(Supplier<T> supplier) {
        if (SwingUtilities.isEventDispatchThread()) {
            return supplier.get();
        }

        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result.set(supplier.get());
                } catch (RuntimeException exception) {
                    failure.set(exception);
                }
            });
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to run dialog operation on the event dispatch thread.", exception);
        }

        if (failure.get() != null) {
            throw failure.get();
        }
        return result.get();
    }
}
