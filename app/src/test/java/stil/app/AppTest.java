package stil.app;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test koji provjerava da je aplikacijski paket dostupan na classpath-u.
 */
class AppTest {
    @Test
    void appPackageExists() {
        // App.java ovisi o Swing UI — testiramo samo da paket postoji
        assertDoesNotThrow(() -> Class.forName("stil.app.model.Artikl"));
    }
}
