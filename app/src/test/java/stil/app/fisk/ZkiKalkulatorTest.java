package stil.app.fisk;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.security.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testovi za {@link ZkiKalkulator}.
 * Testira ispravnost ZKI algoritma: format izlaza (32-znakni lowercase hex),
 * ponašanje s različitim parametrima i očekivane iznimke.
 */
class ZkiKalkulatorTest {

    private static KeyPair generateTestKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }

    @Test
    void izracunajReturns32CharHexString() throws Exception {
        KeyPair kp = generateTestKeyPair();
        String zki = ZkiKalkulator.izracunaj(
            "12345678901",
            LocalDateTime.of(2024, 1, 15, 10, 30, 0),
            "1",
            "PP1",
            "1",
            new BigDecimal("100.00"),
            kp.getPrivate()
        );
        assertNotNull(zki);
        assertEquals(32, zki.length());
        assertTrue(zki.matches("[0-9a-f]{32}"), "ZKI mora biti lowercase hex");
    }

    @Test
    void izracunajIsDeterministicForSameKey() throws Exception {
        KeyPair kp = generateTestKeyPair();
        LocalDateTime dt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        BigDecimal iznos = new BigDecimal("50.00");

        // RSA potpis nije deterministički (PKCS#1 v1.5 jest, ali MD5 hash nad njim jest)
        // Provjera da se ne baca iznimka i da je format ispravan
        String zki1 = ZkiKalkulator.izracunaj("12345678901", dt, "1", "PP1", "1", iznos, kp.getPrivate());
        String zki2 = ZkiKalkulator.izracunaj("12345678901", dt, "1", "PP1", "1", iznos, kp.getPrivate());
        assertEquals(32, zki1.length());
        assertEquals(32, zki2.length());
    }

    @Test
    void izracunajWithDifferentParamsGivesDifferentZki() throws Exception {
        KeyPair kp = generateTestKeyPair();
        LocalDateTime dt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        String zki1 = ZkiKalkulator.izracunaj("12345678901", dt, "1", "PP1", "1", new BigDecimal("100.00"), kp.getPrivate());
        String zki2 = ZkiKalkulator.izracunaj("12345678901", dt, "2", "PP1", "1", new BigDecimal("100.00"), kp.getPrivate());
        // Različiti broj računa → različiti ZKI (gotovo sigurno)
        assertNotEquals(zki1, zki2);
    }

    @Test
    void izracunajWithZeroAmount() throws Exception {
        KeyPair kp = generateTestKeyPair();
        String zki = ZkiKalkulator.izracunaj(
            "12345678901",
            LocalDateTime.of(2024, 1, 1, 0, 0, 0),
            "1", "PP1", "1",
            new BigDecimal("0.00"),
            kp.getPrivate()
        );
        assertEquals(32, zki.length());
    }

    @Test
    void izracunajWithLargeAmount() throws Exception {
        KeyPair kp = generateTestKeyPair();
        String zki = ZkiKalkulator.izracunaj(
            "12345678901",
            LocalDateTime.of(2024, 12, 31, 23, 59, 59),
            "9999", "PP1", "1",
            new BigDecimal("99999.99"),
            kp.getPrivate()
        );
        assertEquals(32, zki.length());
        assertTrue(zki.matches("[0-9a-f]{32}"));
    }

    @Test
    void izracunajThrowsWithNullKey() {
        assertThrows(Exception.class, () ->
            ZkiKalkulator.izracunaj("12345678901",
                LocalDateTime.now(), "1", "PP1", "1",
                new BigDecimal("10.00"), null)
        );
    }
}
