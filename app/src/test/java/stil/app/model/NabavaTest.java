package stil.app.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testovi za {@link Nabava} model.
 * Pokriva konstruktor, izračun ukupnog troška i sve getere/setere.
 */
class NabavaTest {

    @Test
    void konstruktorPostavlja_SvaPoljaTocno() {
        Nabava n = new Nabava(5, "Majica", 10, 8.50, "test napomena");
        assertEquals(5, n.getArtiklId());
        assertEquals("Majica", n.getArtiklNaziv());
        assertEquals(10, n.getKolicina());
        assertEquals(8.50, n.getNabavnaCijena());
        assertEquals("test napomena", n.getNapomena());
        assertNotNull(n.getVrijemeNabave());
    }

    @Test
    void getUkupniTrosak_IzracunaTocno() {
        Nabava n = new Nabava(1, "Hlace", 5, 12.00, null);
        assertEquals(60.00, n.getUkupniTrosak(), 0.001);
    }

    @Test
    void getUkupniTrosak_NulaKolicina() {
        Nabava n = new Nabava(1, "Test", 0, 10.00, null);
        assertEquals(0.0, n.getUkupniTrosak(), 0.001);
    }

    @Test
    void getUkupniTrosak_NulaCijena() {
        Nabava n = new Nabava(1, "Test", 5, 0.0, null);
        assertEquals(0.0, n.getUkupniTrosak(), 0.001);
    }

    @Test
    void setteri_RadeTocno() {
        Nabava n = new Nabava();
        n.setId(42);
        n.setArtiklId(7);
        n.setArtiklNaziv("Jakna");
        n.setDobavljacId(3);
        n.setDobavljacNaziv("Tekstil d.o.o.");
        n.setKolicina(20);
        n.setNabavnaCijena(15.99);
        n.setNapomena("Napomena");
        LocalDateTime dt = LocalDateTime.of(2024, 3, 15, 9, 0, 0);
        n.setVrijemeNabave(dt);

        assertEquals(42, n.getId());
        assertEquals(7, n.getArtiklId());
        assertEquals("Jakna", n.getArtiklNaziv());
        assertEquals(3, n.getDobavljacId());
        assertEquals("Tekstil d.o.o.", n.getDobavljacNaziv());
        assertEquals(20, n.getKolicina());
        assertEquals(15.99, n.getNabavnaCijena());
        assertEquals("Napomena", n.getNapomena());
        assertEquals(dt, n.getVrijemeNabave());
    }

    @Test
    void napomenaNull_Dozvoljena() {
        Nabava n = new Nabava(1, "Test", 1, 5.0, null);
        assertNull(n.getNapomena());
    }

    @Test
    void getUkupniTrosak_VelikaKolicina() {
        Nabava n = new Nabava(1, "Test", 1000, 99.99, null);
        assertEquals(99990.0, n.getUkupniTrosak(), 0.01);
    }
}
