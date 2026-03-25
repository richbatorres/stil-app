package stil.app.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testovi za {@link Validator}.
 * Testira sve validacijske metode s ispravnim unosima, neispravnim unosima,
 * graničnim slučajevima i null vrijednostima.
 * Koristi null kao parent komponentu — JOptionPane se ne prikazuje u headless testu.
 */
class ValidatorTest {

    // -------------------------------------------------------------------------
    // parseDecimal
    // -------------------------------------------------------------------------

    @Test
    void parseDecimal_IspravniBroj() {
        assertEquals(12.50, Validator.parseDecimal(null, "12.50", "Cijena", true, false));
    }

    @Test
    void parseDecimal_ZarezKaoSeparator() {
        assertEquals(12.50, Validator.parseDecimal(null, "12,50", "Cijena", true, false));
    }

    @Test
    void parseDecimal_NulaDozvoljena() {
        assertEquals(0.0, Validator.parseDecimal(null, "0", "Cijena", true, false));
    }

    @Test
    void parseDecimal_NulaNijeDozvoljena() {
        assertNull(Validator.parseDecimal(null, "0", "Cijena", false, false));
    }

    @Test
    void parseDecimal_NegativniNijeDozvoljen() {
        assertNull(Validator.parseDecimal(null, "-5.00", "Cijena", true, false));
    }

    @Test
    void parseDecimal_NegativniDozvoljen() {
        assertEquals(-5.0, Validator.parseDecimal(null, "-5.00", "Cijena", true, true));
    }

    @Test
    void parseDecimal_TekstVracaNull() {
        assertNull(Validator.parseDecimal(null, "abc", "Cijena", true, false));
    }

    @Test
    void parseDecimal_PrazanStringVracaNull() {
        assertNull(Validator.parseDecimal(null, "", "Cijena", true, false));
    }

    @Test
    void parseDecimal_NullVracaNull() {
        assertNull(Validator.parseDecimal(null, null, "Cijena", true, false));
    }

    @Test
    void parseDecimal_MjesovitaTekstBrojVracaNull() {
        assertNull(Validator.parseDecimal(null, "12abc", "Cijena", true, false));
    }

    @Test
    void parseDecimal_SamoZarezVracaNull() {
        assertNull(Validator.parseDecimal(null, ",", "Cijena", true, false));
    }

    // -------------------------------------------------------------------------
    // parseInt
    // -------------------------------------------------------------------------

    @Test
    void parseInt_IspravniBroj() {
        assertEquals(5, Validator.parseInt(null, "5", "Količina", 1));
    }

    @Test
    void parseInt_MinimumDozvoljen() {
        assertEquals(1, Validator.parseInt(null, "1", "Količina", 1));
    }

    @Test
    void parseInt_IspadIspodMinimaVracaNull() {
        assertNull(Validator.parseInt(null, "0", "Količina", 1));
    }

    @Test
    void parseInt_NegativniVracaNull() {
        assertNull(Validator.parseInt(null, "-3", "Količina", 0));
    }

    @Test
    void parseInt_TekstVracaNull() {
        assertNull(Validator.parseInt(null, "pet", "Količina", 1));
    }

    @Test
    void parseInt_DecimalniVracaNull() {
        assertNull(Validator.parseInt(null, "3.5", "Količina", 1));
    }

    @Test
    void parseInt_PrazanStringVracaNull() {
        assertNull(Validator.parseInt(null, "", "Količina", 1));
    }

    // -------------------------------------------------------------------------
    // validateOib
    // -------------------------------------------------------------------------

    @Test
    void validateOib_Ispravan11Znamenki() {
        assertTrue(Validator.validateOib(null, "12345678901"));
    }

    @Test
    void validateOib_PrazanJeIspravan() {
        assertTrue(Validator.validateOib(null, ""));
        assertTrue(Validator.validateOib(null, null));
    }

    @Test
    void validateOib_Prekratak() {
        assertFalse(Validator.validateOib(null, "1234567890")); // 10 znamenki
    }

    @Test
    void validateOib_Predugacak() {
        assertFalse(Validator.validateOib(null, "123456789012")); // 12 znamenki
    }

    @Test
    void validateOib_SlovaNisuDozvoljena() {
        assertFalse(Validator.validateOib(null, "1234567890A"));
    }

    // -------------------------------------------------------------------------
    // validateEmail
    // -------------------------------------------------------------------------

    @Test
    void validateEmail_IspravniFormat() {
        assertTrue(Validator.validateEmail(null, "ime@tvrtka.hr"));
    }

    @Test
    void validateEmail_PrazanJeIspravan() {
        assertTrue(Validator.validateEmail(null, ""));
        assertTrue(Validator.validateEmail(null, null));
    }

    @Test
    void validateEmail_BezAt() {
        assertFalse(Validator.validateEmail(null, "imedomena.hr"));
    }

    @Test
    void validateEmail_BezDomene() {
        assertFalse(Validator.validateEmail(null, "ime@"));
    }

    @Test
    void validateEmail_SamoAt() {
        assertFalse(Validator.validateEmail(null, "@"));
    }

    // -------------------------------------------------------------------------
    // requireNonEmpty
    // -------------------------------------------------------------------------

    @Test
    void requireNonEmpty_NeprazanString() {
        assertTrue(Validator.requireNonEmpty(null, "Naziv", "Naziv"));
    }

    @Test
    void requireNonEmpty_PrazanString() {
        assertFalse(Validator.requireNonEmpty(null, "", "Naziv"));
    }

    @Test
    void requireNonEmpty_SamoRazmaci() {
        assertFalse(Validator.requireNonEmpty(null, "   ", "Naziv"));
    }

    @Test
    void requireNonEmpty_Null() {
        assertFalse(Validator.requireNonEmpty(null, null, "Naziv"));
    }
}
