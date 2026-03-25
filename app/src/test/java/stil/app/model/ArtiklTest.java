package stil.app.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testovi za model {@link stil.app.model.Artikl}.
 * Testira konstruktore, getere/setere i toString metodu.
 */
class ArtiklTest {

    @Test
    void defaultConstructorCreatesEmptyArtikl() {
        Artikl a = new Artikl();
        assertEquals(0, a.getId());
        assertNull(a.getNaziv());
        assertNull(a.getBarkod());
        assertEquals(0.0, a.getCijena());
        assertEquals(0.0, a.getPdvStopa());
        assertEquals(0, a.getKolicinaNaSkladistu());
    }

    @Test
    void fullConstructorSetsAllFields() {
        Artikl a = new Artikl("Majica", "1234567890123", 29.99, 25.0, 10);
        assertEquals("Majica", a.getNaziv());
        assertEquals("1234567890123", a.getBarkod());
        assertEquals(29.99, a.getCijena());
        assertEquals(25.0, a.getPdvStopa());
        assertEquals(10, a.getKolicinaNaSkladistu());
    }

    @Test
    void settersAndGettersWork() {
        Artikl a = new Artikl();
        a.setId(5);
        a.setNaziv("Hlače");
        a.setBarkod("9876543210987");
        a.setCijena(49.99);
        a.setPdvStopa(25.0);
        a.setKolicinaNaSkladistu(3);

        assertEquals(5, a.getId());
        assertEquals("Hlače", a.getNaziv());
        assertEquals("9876543210987", a.getBarkod());
        assertEquals(49.99, a.getCijena());
        assertEquals(25.0, a.getPdvStopa());
        assertEquals(3, a.getKolicinaNaSkladistu());
    }

    @Test
    void toStringReturnsNaziv() {
        Artikl a = new Artikl("Jakna", null, 99.99, 25.0, 5);
        assertEquals("Jakna", a.toString());
    }

    @Test
    void nullBarkodIsAllowed() {
        Artikl a = new Artikl("Šal", null, 15.0, 25.0, 20);
        assertNull(a.getBarkod());
    }

    @Test
    void zeroPriceIsAllowed() {
        Artikl a = new Artikl("Uzorak", "000", 0.0, 25.0, 1);
        assertEquals(0.0, a.getCijena());
    }
}
