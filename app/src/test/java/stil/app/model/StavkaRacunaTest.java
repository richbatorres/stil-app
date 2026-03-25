package stil.app.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testovi za model {@link stil.app.model.StavkaRacuna}.
 * Testira konstruktor iz Artikla, izračun ukupnog iznosa s popustom i bez,
 * te getere/setere.
 */
class StavkaRacunaTest {

    private Artikl dummyArtikl() {
        Artikl a = new Artikl("Majica", "123", 20.00, 25.0, 10);
        a.setId(1);
        return a;
    }

    @Test
    void constructorFromArtiklCopiesFields() {
        Artikl a = dummyArtikl();
        StavkaRacuna s = new StavkaRacuna(a, 2, 0.0);
        assertEquals(1, s.getArtiklId());
        assertEquals("Majica", s.getArtiklNaziv());
        assertEquals(20.00, s.getCijena());
        assertEquals(2, s.getKolicina());
        assertEquals(0.0, s.getPopust());
        assertEquals(25.0, s.getPdvStopa());
    }

    @Test
    void getUkupnoWithoutDiscount() {
        StavkaRacuna s = new StavkaRacuna(dummyArtikl(), 3, 0.0);
        assertEquals(60.00, s.getUkupno(), 0.001);
    }

    @Test
    void getUkupnoWithDiscount10Percent() {
        StavkaRacuna s = new StavkaRacuna(dummyArtikl(), 2, 10.0);
        // 20 * 2 * (1 - 0.10) = 36.00
        assertEquals(36.00, s.getUkupno(), 0.001);
    }

    @Test
    void getUkupnoWithFullDiscount() {
        StavkaRacuna s = new StavkaRacuna(dummyArtikl(), 1, 100.0);
        assertEquals(0.0, s.getUkupno(), 0.001);
    }

    @Test
    void getUkupnoSingleItem() {
        StavkaRacuna s = new StavkaRacuna(dummyArtikl(), 1, 0.0);
        assertEquals(20.00, s.getUkupno(), 0.001);
    }

    @Test
    void settersAndGettersWork() {
        StavkaRacuna s = new StavkaRacuna();
        s.setId(10);
        s.setRacunId(5);
        s.setArtiklId(3);
        s.setArtiklNaziv("Hlače");
        s.setCijena(49.99);
        s.setKolicina(2);
        s.setPopust(5.0);
        s.setPdvStopa(25.0);

        assertEquals(10, s.getId());
        assertEquals(5, s.getRacunId());
        assertEquals(3, s.getArtiklId());
        assertEquals("Hlače", s.getArtiklNaziv());
        assertEquals(49.99, s.getCijena());
        assertEquals(2, s.getKolicina());
        assertEquals(5.0, s.getPopust());
        assertEquals(25.0, s.getPdvStopa());
    }

    @Test
    void getUkupnoWithDiscount50Percent() {
        StavkaRacuna s = new StavkaRacuna(dummyArtikl(), 4, 50.0);
        // 20 * 4 * 0.5 = 40.00
        assertEquals(40.00, s.getUkupno(), 0.001);
    }
}
