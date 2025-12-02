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

package epox.webaom.net;

/**
 * AniDB connection settings. Holds configuration for UDP API communication.
 */
public class AniDBConnectionSettings {

    /** AniDB API server hostname */
    public final String host;

    /** Remote server port */
    public final int remotePort;

    /** Local port to bind for UDP communication */
    public final int localPort;

    /** Connection timeout in milliseconds */
    public final int timeoutMillis;

    /** Maximum number of timeout retries before giving up */
    public final int maxTimeouts;

    /** Delay between packets in milliseconds */
    public int packetDelay;

    /** Whether NAT traversal check is enabled */
    public final boolean natEnabled;

    /**
     * Constructor for AniDB connection settings.
     *
     * @param host
     *            AniDB API server hostname
     * @param remotePort
     *            Remote server port
     * @param localPort
     *            Local port to bind for UDP communication
     * @param timeoutSeconds
     *            Connection timeout in seconds (will be converted to milliseconds)
     * @param packetDelay
     *            Delay between packets in milliseconds
     * @param maxTimeouts
     *            Maximum number of timeout retries
     * @param natEnabled
     *            Whether to enable NAT traversal check
     */
    public AniDBConnectionSettings(
            String host,
            int remotePort,
            int localPort,
            int timeoutSeconds,
            int packetDelay,
            int maxTimeouts,
            boolean natEnabled) {
        this.host = host;
        this.remotePort = remotePort;
        this.localPort = localPort;
        this.timeoutMillis = timeoutSeconds * 1000;
        this.packetDelay = packetDelay;
        this.maxTimeouts = maxTimeouts;
        this.natEnabled = natEnabled;
    }
}
