package stil.app.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Pomoćna klasa za AES-256-GCM enkripciju osjetljivih podataka u bazi (PIN, lozinka certifikata).
 *
 * Ključ se izvodi iz korisničkog imena OS-a i hostnamea računala — enkriptirani podaci
 * nisu prenosivi između različitih računala, što je primjerena zaštita za lokalni POS.
 *
 * Format pohrane: {@code ENC:<Base64(IV + ciphertext + GCM_tag)>}
 * Vrijednosti bez prefiksa {@code ENC:} tretiraju se kao nešifrirani (backwards compatibility).
 */
public class CryptoUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LEN = 12;
    static final String PREFIX = "ENC:";

    private static SecretKey cachedKey;

    /**
     * Enkriptira plaintext. Null/prazni string → null. Već enkriptirano → vraća nepromijenjeno.
     *
     * @param plaintext tekst za enkripciju
     * @return enkriptirana vrijednost s prefiksom ENC:, ili null
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return null;
        if (plaintext.startsWith(PREFIX)) return plaintext;
        try {
            SecretKey key = getKey();
            byte[] iv = Arrays.copyOf(key.getEncoded(), IV_LEN);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[IV_LEN + ct.length];
            System.arraycopy(iv, 0, out, 0, IV_LEN);
            System.arraycopy(ct, 0, out, IV_LEN, ct.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            return plaintext; // fallback ako JCE nije dostupan
        }
    }

    /**
     * Dekriptira vrijednost enkriptiranu s {@link #encrypt}.
     * Vrijednosti bez prefiksa ENC: vraćaju se nepromijenjene (stare nešifrirane vrijednosti).
     *
     * @param encrypted enkriptirana vrijednost iz baze
     * @return dekriptirani plaintext
     */
    public static String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) return encrypted;
        if (!encrypted.startsWith(PREFIX)) return encrypted;
        try {
            byte[] data = Base64.getDecoder().decode(encrypted.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOf(data, IV_LEN);
            byte[] ct = Arrays.copyOfRange(data, IV_LEN, data.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return encrypted;
        }
    }

    /** Izvodi AES-256 ključ iz machine-specific podataka. Kešira nakon prvog poziva. */
    static SecretKey getKey() throws Exception {
        if (cachedKey != null) return cachedKey;
        String seed = System.getProperty("user.name", "stil") + "@"
                    + java.net.InetAddress.getLocalHost().getHostName();
        byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                                       .digest(seed.getBytes(StandardCharsets.UTF_8));
        cachedKey = new SecretKeySpec(keyBytes, "AES");
        return cachedKey;
    }

    /** Resetira keširani ključ — samo za testove. */
    static void resetCache() { cachedKey = null; }
}
