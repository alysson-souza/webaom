/*
 * Created on 24.feb.2006 22:21:45
 * Filename: Setup.java
 */
package epox.webaom.net;

/**
 * AniDB connection settings. Holds configuration for UDP API communication.
 */
public class AniDBConnectionSettings {

    /** AniDB API server hostname */
    public String host;

    /** Remote server port */
    public int remotePort;

    /** Local port to bind for UDP communication */
    public int localPort;

    /** Connection timeout in milliseconds */
    public int timeoutMillis;

    /** Maximum number of timeout retries before giving up */
    public int maxTimeouts;

    /** Delay between packets in milliseconds */
    public int packetDelay;

    /** Whether NAT traversal check is enabled */
    public boolean natEnabled;

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
