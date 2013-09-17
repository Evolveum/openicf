package org.forgerock.openicf.connectors.rsaauthenticationmanager;

import org.identityconnectors.common.security.GuardedString;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.identityconnectors.common.logging.Log;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Utility class for the RSA Connector
 * 
 * @author Alex Babeanu (ababeanu@nulli.com)
 * www.nulli.com - Identity Solution Architects
 * 
 * @version 1.1
 * @since 1.0
 */
public class RSAAuthenticationManager8Utils {
 
    /**
     * Encryption/decryption password for the encryption utility below.
     * <br/>
     * This could be improved depending on the requirements of the implementation.
     * For example, the password could be passed as a system property. At the very least
     * this hard-coded password should be changed from an implementation to another.
     */
    private static final char[] PASSWORD = "enfldsabllx3gdlpsdqgm".toCharArray();
    /**
     * Salt for the encryption algorithm
     */
    private static final byte[] SALT = {
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
    };
    /**
     * logger
     */
    private static final Log logger = Log.getLog(RSAAuthenticationManager8Utils.class);
    
    public RSAAuthenticationManager8Utils() {
        super();
    }
    
    /**
     * Returns the plain password of a GuardedString
     * 
     * @param password the GuardedString storing the encrypted pwd to decrypt and return.
     * @return A String representing the clear text password.
     */
    public static String getPlainPassword(GuardedString password) {
        if (password == null) {
            return null;
        }
        final StringBuffer buf = new StringBuffer();
        password.access(new GuardedString.Accessor() {
            public void access(char[] clearChars) {
                buf.append(clearChars);
            }
        });
        return buf.toString();
    }

    /**
     * Encrypts the given string - assumed to be a config property- using the
     * constant pwd and salt using MD5 and DES.
     * 
     * @param property the String to encrypt
     * @return an encrypted representation of the string.
     * 
     * @throws GeneralSecurityException
     * @throws UnsupportedEncodingException 
     */
    public static String encrypt(String property) throws GeneralSecurityException, UnsupportedEncodingException {
        // DEBUG:
        //logger.info("Encrypting property: {0}.", property);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
        pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
        return base64Encode(pbeCipher.doFinal(property.getBytes("UTF-8")));
    }

    private static String base64Encode(byte[] bytes) {
        // DEBUG:
        //logger.info("Base 64 encoding bytes: {0}.", bytes.toString());
        return new BASE64Encoder().encode(bytes);
    }

    public static String decrypt(String property) throws GeneralSecurityException, IOException {
        // DEBUG:
        //logger.info("Decrypting property: {0}--.", property);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
        pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
        return new String(pbeCipher.doFinal(base64Decode(property)), "UTF-8");
    }

    private static byte[] base64Decode(String property) throws IOException {
        // DEBUG:
        //logger.info("Base 64 decoding property: {0}.", property);
        return new BASE64Decoder().decodeBuffer(property);
    }
}

