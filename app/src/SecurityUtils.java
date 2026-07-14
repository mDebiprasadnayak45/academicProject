import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Security and Encryption Utilities for the Face Recognition Attendance System.
 * Provides secure password hashing using SHA-256 with salt, OTP generation,
 * and basic encryption/decryption capabilities.
 * 
 * @author Face Recognition Team
 * @version 2.0
 */
public class SecurityUtils {

    /**
     * Hashes a password using SHA-256 with a random salt.
     * The salt is prepended to the hash for storage and later verification.
     * 
     * @param password The plain text password to hash
     * @return Base64-encoded string containing salt + hash
     * @throws RuntimeException if SHA-256 algorithm is not available
     */
    public static String hashPassword(String password) {
        try {
            SecureRandom sr = new SecureRandom();
            byte[] salt = new byte[16];
            sr.nextBytes(salt);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());

            // Combine salt + hash
            byte[] saltAndHash = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, saltAndHash, 0, salt.length);
            System.arraycopy(hashedPassword, 0, saltAndHash, salt.length, hashedPassword.length);

            return Base64.getEncoder().encodeToString(saltAndHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies a password against a stored hash.
     * Extracts the salt from the hash and recomputes the hash to compare.
     * 
     * @param password The plain text password to verify
     * @param hash     The stored Base64-encoded salt+hash
     * @return true if password matches the hash, false otherwise
     * @throws RuntimeException if SHA-256 algorithm is not available
     */
    public static boolean verifyPassword(String password, String hash) {

        if (password == null || hash == null) {
            return false;
        }
        try {
            byte[] decodedHash = Base64.getDecoder().decode(hash);
            byte[] salt = new byte[16];
            System.arraycopy(decodedHash, 0, salt, 0, 16);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());

            // Compare hashes
            boolean match = true;
            for (int i = 0; i < hashedPassword.length; i++) {
                match &= (hashedPassword[i] == decodedHash[i + 16]);
            }
            return match;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generates a random 6-digit One-Time Password (OTP).
     * 
     * @return 6-digit OTP as a string
     */
    public static String generateOTP() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // 6 digits
        return String.valueOf(otp);
    }

    /**
     * Generates a random alphanumeric captcha string.
     *
     * @param length Desired captcha length
     * @return Uppercase alphanumeric captcha
     */
    public static String generateCaptcha(int length) {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        int safeLength = Math.max(4, length);
        SecureRandom random = new SecureRandom();
        StringBuilder captcha = new StringBuilder(safeLength);
        for (int i = 0; i < safeLength; i++) {
            captcha.append(chars.charAt(random.nextInt(chars.length())));
        }
        return captcha.toString();
    }

    /**
     * Generates a random token for QR code generation.
     * 
     * @return URL-safe Base64-encoded random token
     */
    public static String generateQRToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Encrypts sensitive data using basic XOR cipher.
     * 
     * ⚠️ SECURITY WARNING: This is a WEAK encryption method for educational
     * purposes only!
     * XOR cipher provides NO real security and can be easily broken.
     * 
     * DO NOT USE IN PRODUCTION. For production systems, use:
     * - AES-256 with GCM mode (javax.crypto.Cipher)
     * - Proper key derivation (PBKDF2, Argon2)
     * - Authenticated encryption with associated data (AEAD)
     * 
     * This implementation is included ONLY for academic demonstration.
     * 
     * @param data The data to encrypt
     * @param key  The encryption key
     * @return Base64-encoded encrypted data
     * @deprecated Use proper encryption library (AES) for production
     */
    @Deprecated
    public static String encrypt(String data, String key) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < data.length(); i++) {
            result.append((char) (data.charAt(i) ^ key.charAt(i % key.length())));
        }
        return Base64.getEncoder().encodeToString(result.toString().getBytes());
    }

    /**
     * Decrypts data encrypted with the encrypt method.
     * 
     * ⚠️ SECURITY WARNING: XOR cipher is NOT secure. See warning in encrypt()
     * method.
     * 
     * @param encryptedData Base64-encoded encrypted data
     * @param key           The decryption key (must match encryption key)
     * @return Decrypted plain text
     * @deprecated Use proper encryption library (AES) for production
     */
    @Deprecated
    public static String decrypt(String encryptedData, String key) {
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        String data = new String(decodedBytes);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < data.length(); i++) {
            result.append((char) (data.charAt(i) ^ key.charAt(i % key.length())));
        }
        return result.toString();
    }

    /**
     * Validates email format using regex pattern.
     * 
     * @param email The email address to validate
     * @return true if email format is valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    /**
     * Validates password strength requirements.
     * Password must be at least 8 characters long and contain:
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     * 
     * @param password The password to validate
     * @return true if password meets strength requirements, false otherwise
     */
    public static boolean isStrongPassword(String password) {
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*\\d.*") &&
                password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};:'\",.<>?/\\\\|`~].*");
    }
}
