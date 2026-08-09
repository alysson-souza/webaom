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
 */

package epox.webaom.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import epox.webaom.ui.MainPanel;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class PingerTest {
    @Test
    void successfulPingReportsLatencyAndDisconnects() throws Exception {
        MainPanel panel = mock(MainPanel.class);
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(panel.createConnection()).thenReturn(connection);
        when(connection.connect()).thenReturn(true);
        when(connection.ping()).thenReturn(1_500);

        Pinger pinger = runToCompletion(panel);

        assertFalse(pinger.isAlive());
        InOrder order = inOrder(panel, connection);
        order.verify(panel).handleFatalError(true);
        order.verify(connection).connect();
        order.verify(panel).println("PING...");
        order.verify(panel).println("PONG (in 1.5 seconds).");
        order.verify(panel).showMessage("PONG (in 1.5 seconds).");
        order.verify(connection).disconnect();
        order.verify(panel).handleFatalError(false);
    }

    @Test
    void refusedConnectionReportsTheCurrentConnectionError() throws Exception {
        MainPanel panel = mock(MainPanel.class);
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(panel.createConnection()).thenReturn(connection);
        when(connection.connect()).thenReturn(false);
        when(connection.getLastError()).thenReturn("Cannot bind");

        runToCompletion(panel);

        verify(panel).showMessage("Cannot bind.");
        verify(connection).disconnect();
        verify(panel).handleFatalError(false);
    }

    @Test
    void timeoutReportsAniDbAsUnreachable() throws Exception {
        MainPanel panel = mock(MainPanel.class);
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(panel.createConnection()).thenReturn(connection);
        when(connection.connect()).thenReturn(true);
        when(connection.ping()).thenThrow(new SocketTimeoutException("timeout"));

        runToCompletion(panel);

        verify(panel).println("<span class=\"log-warning\">AniDB is not reachable.</span>");
        verify(panel).showMessage("AniDB is not reachable");
        verify(connection).disconnect();
    }

    @Test
    void invalidConnectionNumberIsReportedWithoutCreatingAConnection() throws Exception {
        MainPanel panel = mock(MainPanel.class);
        when(panel.createConnection()).thenThrow(new NumberFormatException(" bad port"));

        runToCompletion(panel);

        verify(panel).showMessage("Invalid number. bad port");
        verify(panel).handleFatalError(false);
    }

    @Test
    void unexpectedFailureIsRenderedAndAlwaysRestoresTheUi() throws Exception {
        MainPanel panel = mock(MainPanel.class);
        when(panel.createConnection()).thenThrow(new IllegalStateException("broken"));

        runToCompletion(panel);

        verify(panel).println("<span class=\"log-warning\">broken.</span>");
        verify(panel).showMessage("broken.");
        verify(panel).handleFatalError(false);
    }

    private static Pinger runToCompletion(MainPanel panel) throws InterruptedException {
        Pinger pinger = new Pinger(panel);
        pinger.join(2_000);
        return pinger;
    }
}
