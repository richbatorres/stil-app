package stil.app.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testovi za {@link CryptoUtil}.
 * Pokriva enkripciju, dekripciju, null vrijednosti, prazne stringove,
 * backwards compatibility s nešifriranim vrijednostima i idempotentnost.
 */
class CryptoUtilTest {

    @BeforeEach
    void resetKey() {
        CryptoUtil.resetCache();
    }

    @Test
    void encrypt_NullVraca_Null() {
        assertNull(CryptoUtil.encrypt(null));
    }

    @Test
    void encrypt_PrazanStringVraca_Null() {
        assertNull(CryptoUtil.encrypt(""));
    }

    @Test
    void encrypt_VracaVrijednostSPrefiksom() {
        String enc = CryptoUtil.encrypt("tajni_pin");
        assertNotNull(enc);
        assertTrue(enc.startsWith(CryptoUtil.PREFIX));
    }

    @Test
    void decrypt_NullVraca_Null() {
        assertNull(CryptoUtil.decrypt(null));
    }

    @Test
    void decrypt_PrazanStringVraca_PrazanString() {
        assertEquals("", CryptoUtil.decrypt(""));
    }

    @Test
    void encryptDecrypt_RoundTrip() {
        String original = "moja_lozinka_123!";
        String enc = CryptoUtil.encrypt(original);
        String dec = CryptoUtil.decrypt(enc);
        assertEquals(original, dec);
    }

    @Test
    void encryptDecrypt_PinRoundTrip() {
        String pin = "1234";
        assertEquals(pin, CryptoUtil.decrypt(CryptoUtil.encrypt(pin)));
    }

    @Test
    void decrypt_BackwardsCompatibility_NesifriranaVrijednost() {
        // Stare vrijednosti bez prefiksa ENC: moraju se vratiti nepromijenjene
        String stara = "plaintext_lozinka";
        assertEquals(stara, CryptoUtil.decrypt(stara));
    }

    @Test
    void encrypt_Idempotentno_VecEnkriptiranoNeEnkriptiraOpet() {
        String enc1 = CryptoUtil.encrypt("test");
        String enc2 = CryptoUtil.encrypt(enc1);
        assertEquals(enc1, enc2); // ne smije dvostruko enkriptirati
    }

    @Test
    void encryptDecrypt_SpecijalnZnakovi() {
        String special = "lozinka@#$%^&*()_+{}|:<>?";
        assertEquals(special, CryptoUtil.decrypt(CryptoUtil.encrypt(special)));
    }

    @Test
    void encryptDecrypt_DugaVrijednost() {
        String duga = "a".repeat(500);
        assertEquals(duga, CryptoUtil.decrypt(CryptoUtil.encrypt(duga)));
    }
}
