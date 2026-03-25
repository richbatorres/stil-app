package stil.app.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testovi za model {@link stil.app.model.Config}.
 * Testira početne vrijednosti, getere/setere i toggle fiskTestMode zastavice.
 */
class ConfigTest {

    @Test
    void defaultFiskTestModeIsTrue() {
        Config c = new Config();
        assertTrue(c.isFiskTestMode());
    }

    @Test
    void settersAndGettersWork() {
        Config c = new Config();
        c.setNazivTvrtke("STIL A j.d.o.o.");
        c.setOib("12345678901");
        c.setAdresa("Ulica 1, Zagreb");
        c.setOznakaPosProstora("PP1");
        c.setOznakaUredaja("1");
        c.setPutanjaCertifikata("/path/to/cert.p12");
        c.setLozinkaCertifikata("tajnalozinka");
        c.setPin("1234");
        c.setFiskTestMode(false);

        assertEquals("STIL A j.d.o.o.", c.getNazivTvrtke());
        assertEquals("12345678901", c.getOib());
        assertEquals("Ulica 1, Zagreb", c.getAdresa());
        assertEquals("PP1", c.getOznakaPosProstora());
        assertEquals("1", c.getOznakaUredaja());
        assertEquals("/path/to/cert.p12", c.getPutanjaCertifikata());
        assertEquals("tajnalozinka", c.getLozinkaCertifikata());
        assertEquals("1234", c.getPin());
        assertFalse(c.isFiskTestMode());
    }

    @Test
    void allFieldsNullByDefault() {
        Config c = new Config();
        assertNull(c.getNazivTvrtke());
        assertNull(c.getOib());
        assertNull(c.getAdresa());
        assertNull(c.getOznakaPosProstora());
        assertNull(c.getOznakaUredaja());
        assertNull(c.getPutanjaCertifikata());
        assertNull(c.getLozinkaCertifikata());
        assertNull(c.getPin());
    }

    @Test
    void fiskTestModeCanBeToggled() {
        Config c = new Config();
        assertTrue(c.isFiskTestMode());
        c.setFiskTestMode(false);
        assertFalse(c.isFiskTestMode());
        c.setFiskTestMode(true);
        assertTrue(c.isFiskTestMode());
    }

    @Test
    void pinCanBeNull() {
        Config c = new Config();
        c.setPin(null);
        assertNull(c.getPin());
    }
}
