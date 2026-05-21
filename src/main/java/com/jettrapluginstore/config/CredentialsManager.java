package com.jettrapluginstore.config;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Arrays;

public class CredentialsManager {

    private static final String FILE_NAME = "jettraappstore.md";
    private static final String ALGORITHM = "AES";

    private static SecretKeySpec generateKey(String secret) throws Exception {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); 
        return new SecretKeySpec(key, ALGORITHM);
    }

    public static String encrypt(String strToEncrypt, String secret) {
        try {
            SecretKeySpec secretKey = generateKey(secret);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            System.err.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    public static String decrypt(String strToDecrypt, String secret) {
        try {
            SecretKeySpec secretKey = generateKey(secret);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) {
            System.err.println("Error while decrypting: " + e.toString());
        }
        return null;
    }

    public static void saveCredentials(String user, String password, String secret) {
        try {
            String content = "user " + user + "\npassword " + password + "\n";
            String encrypted = encrypt(content, secret);
            Files.write(Paths.get(FILE_NAME), encrypted.getBytes(StandardCharsets.UTF_8));
            System.out.println("Credentials saved to " + FILE_NAME);
        } catch (Exception e) {
            System.err.println("Failed to save credentials: " + e.getMessage());
        }
    }

    public static String[] loadCredentials(String secret) {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return null;
        }
        try {
            String encrypted = new String(Files.readAllBytes(Paths.get(FILE_NAME)), StandardCharsets.UTF_8);
            String decrypted = decrypt(encrypted, secret);
            if (decrypted == null) {
                return null;
            }
            String[] lines = decrypted.split("\n");
            String user = null;
            String pass = null;
            for (String line : lines) {
                if (line.startsWith("user ")) {
                    user = line.substring(5).trim();
                } else if (line.startsWith("password ")) {
                    pass = line.substring(9).trim();
                }
            }
            if (user != null && pass != null) {
                return new String[]{user, pass};
            }
        } catch (Exception e) {
            System.err.println("Failed to load credentials: " + e.getMessage());
        }
        return null;
    }
}
