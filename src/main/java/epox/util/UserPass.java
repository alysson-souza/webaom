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
 * Created on 18.des.2005 12:36:32
 * Filename: UserPass.java
 */
package epox.util;

import com.bitzi.util.Base32;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Container for user credentials (username, password, API key) with AES encryption support.
 * Used for storing and retrieving AniDB login credentials securely.
 */
public class UserPass {
	/** The username for authentication */
	public String username;
	/** The password for authentication */
	public String password;
	/** The optional API key for encryption */
	public String apiKey;

	public UserPass(String username, String password, String apiKey) {
		this.username = username;
		this.password = password;
		this.apiKey = apiKey;
	}

	/**
	 * Parses and sets credentials from an encoded string (format: "user:encpass:enckey").
	 *
	 * @param encoded
	 *            the encoded credentials string
	 */
	public void set(String encoded) {
		try {
			String[] parts = U.split(encoded, ':');
			username = parts[0];
			if (parts.length >= 2) {
				password = decrypt(parts[1]);
			} else {
				password = null;
			}
			if (parts.length >= 3) {
				apiKey = decrypt(parts[2]);
			} else {
				apiKey = null;
			}
		} catch (Exception ex) {
			// Silently fail on parse errors
		}
	}

	/**
	 * Returns the credentials as a string.
	 *
	 * @param includePassword
	 *            if true, includes encrypted password and API key
	 * @return the credentials string
	 */
	public String get(boolean includePassword) {
		if (!includePassword) {
			return username;
		}
		return username + ":" + encryptIfNotEmpty(password) + ":" + encryptIfNotEmpty(apiKey);
	}

	/**
	 * Encrypts a string if it's not null or empty.
	 *
	 * @param value
	 *            the value to encrypt
	 * @return the encrypted value, or empty string if null/empty
	 */
	private static String encryptIfNotEmpty(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		return encrypt(value);
	}

	/**
	 * Generates an encryption key based on environment properties.
	 * This makes the encrypted passwords machine-specific.
	 *
	 * @return 16-byte MD5 hash of environment properties
	 */
	private static byte[] generateEnvironmentKey() throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("MD5");
		digest.update(System.getProperty("os.name", "").getBytes());
		digest.update(System.getProperty("os.version", "").getBytes());
		digest.update(System.getProperty("os.arch", "").getBytes());
		digest.update(System.getProperty("user.name", "").getBytes());
		digest.update(System.getProperty("user.home", "").getBytes());
		digest.update(System.getProperty("user.language", "").getBytes());
		return digest.digest();
	}

	/**
	 * Encrypts a plain text string using AES and encodes as Base32.
	 *
	 * @param plainText
	 *            the text to encrypt
	 * @return the Base32-encoded encrypted text, or null on error
	 */
	private static String encrypt(String plainText) {
		try {
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(generateEnvironmentKey(), "AES"));
			return Base32.encode(cipher.doFinal(plainText.getBytes()));
		} catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Decrypts a Base32-encoded AES-encrypted string.
	 *
	 * @param cipherText
	 *            the Base32-encoded encrypted text
	 * @return the decrypted plain text, or null on error
	 */
	private static String decrypt(String cipherText) {
		try {
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(generateEnvironmentKey(), "AES"));
			return new String(cipher.doFinal(Base32.decode(cipherText)));
		} catch (Exception ex) {
			return null;
		}
	}
}
