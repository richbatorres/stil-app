package stil.app.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testovi za model {@link Dobavljac}.
 * Testira konstruktore, getere/setere i toString metodu.
 */
class DobavljacTest {

    @Test
    void defaultConstructorCreatesEmptyDobavljac() {
        Dobavljac d = new Dobavljac();
        assertEquals(0, d.getId());
        assertNull(d.getNaziv());
        assertNull(d.getOib());
        assertNull(d.getAdresa());
        assertNull(d.getEmail());
        assertNull(d.getTelefon());
        assertNull(d.getNapomena());
    }

    @Test
    void fullConstructorSetsAllFields() {
        Dobavljac d = new Dobavljac("Tekstil d.o.o.", "12345678901",
            "Ilica 1, Zagreb", "info@tekstil.hr", "01-234-5678");
        assertEquals("Tekstil d.o.o.", d.getNaziv());
        assertEquals("12345678901", d.getOib());
        assertEquals("Ilica 1, Zagreb", d.getAdresa());
        assertEquals("info@tekstil.hr", d.getEmail());
        assertEquals("01-234-5678", d.getTelefon());
    }

    @Test
    void settersAndGettersWork() {
        Dobavljac d = new Dobavljac();
        d.setId(3);
        d.setNaziv("Moda Export");
        d.setOib("98765432109");
        d.setAdresa("Savska 5, Zagreb");
        d.setEmail("moda@export.hr");
        d.setTelefon("091-111-2222");
        d.setNapomena("Plaćanje 30 dana");

        assertEquals(3, d.getId());
        assertEquals("Moda Export", d.getNaziv());
        assertEquals("98765432109", d.getOib());
        assertEquals("Savska 5, Zagreb", d.getAdresa());
        assertEquals("moda@export.hr", d.getEmail());
        assertEquals("091-111-2222", d.getTelefon());
        assertEquals("Plaćanje 30 dana", d.getNapomena());
    }

    @Test
    void toStringReturnsNaziv() {
        Dobavljac d = new Dobavljac("Tekstil d.o.o.", null, null, null, null);
        assertEquals("Tekstil d.o.o.", d.toString());
    }

    @Test
    void nullOibIsAllowed() {
        Dobavljac d = new Dobavljac("Strani dobavljač", null, null, null, null);
        assertNull(d.getOib());
    }

    @Test
    void napomenaCanBeSetAndCleared() {
        Dobavljac d = new Dobavljac();
        d.setNapomena("Rok isporuke 7 dana");
        assertEquals("Rok isporuke 7 dana", d.getNapomena());
        d.setNapomena(null);
        assertNull(d.getNapomena());
    }
}
