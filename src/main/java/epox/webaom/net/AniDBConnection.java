// Copyright (C) 2005-2006 epoximator
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

/*
 * Created on 22.01.05
 *
 * @version 	1.09, 1.07, 1.06, 1.05, 1.03, 1.00
 * @author 		epoximator
 */
package epox.webaom.net;

import epox.swing.Log;
import epox.util.UserPass;
import epox.webaom.AppContext;
import epox.webaom.ui.JDialogLogin;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.Timer;

public class AniDBConnection implements ActionListener {
    public static final String DEFAULT_HOST = "api.anidb.net";
    public static final int DEFAULT_REMOTE_PORT = 9000;
    public static final int DEFAULT_LOCAL_PORT = 45678;

    public static boolean shutdown = false;
    private static int remainingLoginAttempts = 2;
    private final int tagLength = 5;
    private final int keepAliveInterval = 3 * 1000 * 60 * 10; // 30 min
    private final Timer keepAliveTimer;
    private final AniDBConnectionSettings settings;
    private final Log log;
    public boolean authenticated = false;
    protected String session = null;
    protected String currentTag = null;
    private int tagCounter = 0;
    private boolean connected = false;
    private UserPass userPass = null;
    private long timeUsed = 0;
    private long timestamp = 0;
    private int remainingAuthAttempts = 3;
    private String lastError = "Not Initialized.";
    private SecretKeySpec encryptionKey = null;
    private Cipher cipher;
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private String encoding = "ascii";

    public AniDBConnection(Log log, AniDBConnectionSettings settings) {
        this.log = log;
        this.settings = settings;
        generateTag();
        keepAliveTimer = new Timer(keepAliveInterval, this); // +1000
    }

    private void generateTag() {
        tagCounter++;
        currentTag = "" + tagCounter;
        while (currentTag.length() < tagLength) {
            currentTag = "0" + currentTag;
        }
        currentTag = 't' + currentTag;
    }

    public void actionPerformed(ActionEvent event) {
        if (!settings.natEnabled) {
            return;
        }
        try {
            long now = System.currentTimeMillis();
            if ((now - timestamp) > keepAliveInterval) {
                ping();
            }
        } catch (SocketTimeoutException ex) {
            debug("! No response from server.");
            keepAliveTimer.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /////////////////////////////////// PASSW////////////////////////////////////
    public void set(String username, String password, String apiKey) {
        userPass = new UserPass(username, password, apiKey);
    }

    private void decrementLoginAttempts() {
        remainingLoginAttempts--;
    }

    /////////////////////////////////// DEBUG////////////////////////////////////
    protected void error(String message) {
        debug(message);
        lastError = message;
        if (log == null) {
            return;
        }
        log.println("<font color=#FF0000>" + message + "</font>");
        log.status1(message);
    }

    protected void debug(String message) {
        System.out.println(message);
    }

    public String getLastError() {
        return lastError;
    }

    ////////////////////////////// BASIC COMMANDS////////////////////////////////
    public boolean isLoggedIn() {
        return authenticated;
    }

    public int ping() throws Exception {
        sendRaw("PING", true);
        return (int) timeUsed;
    }

    public int encrypt() throws Exception {
        if (userPass.apiKey == null || userPass.apiKey.isEmpty()) {
            return ping();
        }
        AniDBConnectionResponse response = sendWithSession("ENCRYPT", "user=" + userPass.username + "&type=1", true);
        if (response.code == AniDBConnectionResponse.ENCRYPTION_ENABLED) {
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                digest.update(userPass.apiKey.getBytes());
                digest.update(response.data.getBytes());
                byte[] keyBytes = digest.digest();
                encryptionKey = new SecretKeySpec(keyBytes, "AES");
                cipher = Cipher.getInstance("AES");
                return (int) timeUsed;
            } catch (Exception ex) {
                ex.printStackTrace();
                encryptionKey = null;
                cipher = null;
            }
        } else if (response.code == AniDBConnectionResponse.API_PASSWORD_NOT_DEFINED) {
            throw new AniDBException(AniDBException.ENCRYPTION, "AniPass not defined. Check your profile settings.");
        } else if (response.code == AniDBConnectionResponse.NO_SUCH_USER) {
            throw new AniDBException(AniDBException.ENCRYPTION, "No such user. Check username.");
        }
        return -1;
    }

    private boolean showLoginDialog() throws AniDBException {
        if (remainingLoginAttempts <= 0) {
            throw new AniDBException(AniDBException.CLIENT_USER, "Access Denied! No more login attempts allowed.");
        }
        decrementLoginAttempts();
        error("Logins left: " + remainingLoginAttempts);
        userPass = (new JDialogLogin()).getPass();
        return userPass != null;
    }

    public boolean login() throws AniDBException {
        if (userPass == null) {
            if (!showLoginDialog()) {
                error("User Abort");
                return false;
            }
        }
        if (remainingAuthAttempts <= 0) {
            throw new AniDBException(AniDBException.CLIENT_BUG, "Invalid session.");
        }
        String versionParams = "&protover=3&client=webaom&clientver=119&nat=1&comp=1&enc=utf8";
        AniDBConnectionResponse response =
                send("AUTH", "user=" + userPass.username + "&pass=" + userPass.password + versionParams, true);
        remainingAuthAttempts--;
        switch (response.code) {
            case AniDBConnectionResponse.LOGIN_ACCEPTED_NEW_VER:
                AppContext.dialog("Note", AniDBException.defaultMsg(AniDBException.CLIENT_OUTDATED));
            case AniDBConnectionResponse.LOGIN_ACCEPTED: {
                session = response.data;
                if (response.data.length() > 5) {
                    session = response.data.substring(0, 5);
                    if (settings.natEnabled) {
                        try {
                            int port = Integer.parseInt(response.data.substring(1 + response.data.lastIndexOf(':')));
                            if (port != settings.localPort) {
                                // 3 min
                                int natKeepAliveInterval = 3 * 1000 * 60;
                                keepAliveTimer.setDelay(natKeepAliveInterval);
                                debug("! Nat detected.");
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                } else {
                    session = response.data;
                }
                authenticated = true;
                remainingAuthAttempts = 2;
                return true;
            }
            case AniDBConnectionResponse.LOGIN_FAILED:
                error("Login Failed");
                if (showLoginDialog()) {
                    return login();
                }
                return false;
            default:
                error(response.message);
        }
        return false;
    }

    public boolean logout() throws AniDBException {
        if (shutdown) {
            if (authenticated && session != null) { // last chance logout
                String command = "LOGOUT s=" + session;
                authenticated = false;
                try {
                    sendRaw(command, false);
                } catch (IOException ex) {
                    // don't care
                }
            }
            return true;
        }
        AniDBConnectionResponse response = send("LOGOUT", null, true);
        if (response == null) {
            return false;
        }
        switch (response.code) {
            case AniDBConnectionResponse.LOGGED_OUT:
            case AniDBConnectionResponse.NOT_LOGGED_IN:
            case AniDBConnectionResponse.INVALID_SESSION:
            case AniDBConnectionResponse.LOGIN_FIRST:
                authenticated = false;
                encoding = "ascii";
                return true;
            default:
                error(response.message);
        }
        return false;
    }

    //////////////////////////////////// CORE////////////////////////////////////
    public boolean connect() {
        try {
            socket = new DatagramSocket(settings.localPort);
            socket.setSoTimeout(settings.timeoutMillis);
            serverAddress = InetAddress.getByName(settings.host);
            serverAddress.getHostAddress();
            connected = true;
            return true;
        } catch (SocketException ex) {
            ex.printStackTrace();
            error("SocketException: " + ex.getMessage());
        } catch (UnknownHostException ex) {
            error("Unknown Host: " + settings.host);
        }
        return false;
    }

    public void disconnect() {
        if (connected) {
            keepAliveTimer.stop();
            socket.disconnect();
            socket.close();
            socket = null;
            serverAddress = null;
            connected = false;
            authenticated = false;
        }
    }

    public synchronized AniDBConnectionResponse send(String operation, String param, boolean wait)
            throws AniDBException {
        if (operation == null) {
            throw new AniDBException(AniDBException.CLIENT_BUG);
        }
        return sendWithRetry(operation, param, wait);
    }

    public synchronized String send(String command, boolean wait) throws AniDBException {
        if (command == null) {
            throw new AniDBException(AniDBException.CLIENT_BUG);
        }

        String[] parts = command.split(" ", 2);
        AniDBConnectionResponse response = sendWithRetry(parts[0], parts.length > 1 ? parts[1] : null, wait);
        return response.code + " " + response.message + (response.data != null ? "\n" + response.data : "");
    }

    private AniDBConnectionResponse sendWithRetry(String operation, String param, boolean wait) throws AniDBException {
        int timeoutCount = 0;
        AniDBConnectionResponse response;
        while (timeoutCount++ < settings.maxTimeouts && !shutdown) {
            try {
                response = sendWithSession(operation, param, wait);
                if (!operation.equals("LOGOUT")
                        && (response.code == AniDBConnectionResponse.LOGIN_FIRST
                                || response.code == AniDBConnectionResponse.INVALID_SESSION)) {
                    login();
                    return sendWithSession(operation, param, wait);
                }
                return response;
            } catch (SocketTimeoutException ex) {
                generateTag();
                error("Operation Failed: TIMEOUT or SERVER BUSY. Try #" + timeoutCount);
                settings.packetDelay += 100;
                // keepAliveTimer.start();
            } catch (IOException ex) {
                ex.printStackTrace();
                error("Operation Failed: IOEXCEPT: " + ex.getMessage());
            }
        }
        throw new AniDBException(AniDBException.ANIDB_UNREACHABLE, getLastError());
    }

    private AniDBConnectionResponse sendWithSession(String operation, String param, boolean wait)
            throws IOException, AniDBException {
        if (param != null) {
            if (session != null) {
                param += "&s=" + session;
            }
        } else if (session != null) {
            param = "s=" + session;
        }
        generateTag();
        if (param != null) {
            param += "&tag=" + currentTag;
        } else {
            param = "tag=" + currentTag;
        }
        return sendRaw(operation + " " + param, wait);
    }

    private AniDBConnectionResponse sendRaw(String command, boolean wait) throws IOException, AniDBException {
        keepAliveTimer.stop();
        if (wait) {
            try {
                long timeDelta = System.currentTimeMillis() - timestamp;
                timeDelta = (timeDelta / 100) * 100; // round down to nearest 100
                if (timeDelta < settings.packetDelay) {
                    debug("- Sleep: " + (settings.packetDelay - timeDelta));
                    Thread.sleep(settings.packetDelay - timeDelta);
                }
            } catch (InterruptedException ex) {
                throw new AniDBException(AniDBException.CLIENT_SYSTEM, "Java: " + ex.getMessage());
            }
        }
        String censoredCommand = command;
        int passwordIndex = censoredCommand.indexOf("pass=");
        if (passwordIndex > 0) {
            int ampersandIndex = censoredCommand.indexOf("&", passwordIndex);
            if (ampersandIndex > 0) {
                censoredCommand = censoredCommand.substring(0, passwordIndex + 5) + "xxxxx"
                        + censoredCommand.substring(ampersandIndex);
            }
        }
        debug("> " + censoredCommand);
        byte[] outData = command.getBytes(encoding);
        int length = outData.length;

        if (encryptionKey != null) {
            try {
                cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
                outData = cipher.doFinal(outData);
                length = outData.length;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        DatagramPacket outPacket = new DatagramPacket(outData, length, serverAddress, settings.remotePort);
        timestamp = System.currentTimeMillis();

        socket.send(outPacket);

        int encIndex = command.indexOf("&enc=");
        if (encIndex > 0) {
            encIndex += 5;
            int endIndex = command.indexOf('&', encIndex);
            if (endIndex < 0) {
                endIndex = command.length();
            }
            encoding = command.substring(encIndex, endIndex);
        }

        if (!shutdown) { // wait
            AniDBConnectionResponse response = receive();
            keepAliveTimer.start();
            return response;
        }
        return null;
    }

    private AniDBConnectionResponse receive() throws IOException, AniDBException {
        if (shutdown) {
            return null;
        }
        int bufferSize = 2048 * 2;
        byte[] buffer = new byte[bufferSize];
        DatagramPacket inPacket = new DatagramPacket(buffer, bufferSize);

        socket.receive(inPacket);
        timeUsed = System.currentTimeMillis() - timestamp;
        timestamp += timeUsed / 2; // test, share used time

        int length = inPacket.getLength();

        if (encryptionKey != null) {
            try {
                cipher.init(Cipher.DECRYPT_MODE, encryptionKey);
                buffer = cipher.doFinal(buffer, 0, length);
                length = buffer.length;
            } catch (Exception ex) {
                debug("! Decryption failed: " + ex.getMessage());
                encryptionKey = null;
                cipher = null;
                throw new AniDBException(AniDBException.ENCRYPTION);
            }
        }
        if (buffer.length > 1 && buffer[0] == 0 && buffer[1] == 0) {
            try {
                Inflater decompressor = new Inflater();
                decompressor.setInput(buffer, 2, length - 2);
                byte[] result = new byte[length * 3];
                length = decompressor.inflate(result);
                decompressor.end();
                buffer = result;
            } catch (DataFormatException ex) {
                ex.printStackTrace();
            }
        }
        try {
            byte[] rawData = new byte[length];
            System.arraycopy(buffer, 0, rawData, 0, length);
            String responseString = new String(rawData, encoding);
            responseString = responseString.substring(0, responseString.length() - 1);
            debug("< " + responseString);
            return new AniDBConnectionResponse(currentTag, tagLength, responseString);
        } catch (TagMismatchException ex) {
            debug("! Wrong tag! Should be: " + currentTag);
            return receive();
        }
    }
}
