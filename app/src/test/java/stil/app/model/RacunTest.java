package stil.app.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testovi za model {@link stil.app.model.Racun}.
 * Testira izračun ukupnog iznosa, PDV izračun, statusne enum vrijednosti
 * i getere/setere.
 */
class RacunTest {

    private Racun racun;

    @BeforeEach
    void setUp() {
        racun = new Racun();
        racun.setId(1);
        racun.setBrojRacuna(1);
        racun.setOznakaRacuna("1-PP1-1");
        racun.setVrijemeIzdavanja(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        racun.setNacinPlacanja(Racun.NacinPlacanja.GOTOVINA);
        racun.setStatus(Racun.Status.KREIRAN);
    }

    @Test
    void getUkupnoEmptyStavke() {
        assertEquals(0.0, racun.getUkupno(), 0.001);
    }

    @Test
    void getUkupnoWithOneStavka() {
        Artikl a = new Artikl("Majica", null, 20.00, 25.0, 5);
        a.setId(1);
        racun.getStavke().add(new StavkaRacuna(a, 2, 0.0));
        assertEquals(40.00, racun.getUkupno(), 0.001);
    }

    @Test
    void getUkupnoWithMultipleStavke() {
        Artikl a1 = new Artikl("Majica", null, 20.00, 25.0, 5);
        a1.setId(1);
        Artikl a2 = new Artikl("Hlače", null, 50.00, 25.0, 3);
        a2.setId(2);
        racun.getStavke().add(new StavkaRacuna(a1, 2, 0.0));  // 40.00
        racun.getStavke().add(new StavkaRacuna(a2, 1, 10.0)); // 45.00
        assertEquals(85.00, racun.getUkupno(), 0.001);
    }

    @Test
    void getUkupnoPdvCalculation() {
        // Bruto 100 EUR, PDV 25% => PDV = 100 * 25 / (100+25) = 20.00
        Artikl a = new Artikl("Test", null, 100.00, 25.0, 1);
        a.setId(1);
        racun.getStavke().add(new StavkaRacuna(a, 1, 0.0));
        assertEquals(20.00, racun.getUkupnoPdv(), 0.001);
    }

    @Test
    void getUkupnoPdvWithDiscount() {
        // Bruto 80 EUR (100 * 0.8), PDV = 80 * 25 / 125 = 16.00
        Artikl a = new Artikl("Test", null, 100.00, 25.0, 1);
        a.setId(1);
        racun.getStavke().add(new StavkaRacuna(a, 1, 20.0));
        assertEquals(16.00, racun.getUkupnoPdv(), 0.001);
    }

    @Test
    void statusEnum() {
        racun.setStatus(Racun.Status.FISKALIZIRAN);
        assertEquals(Racun.Status.FISKALIZIRAN, racun.getStatus());
        racun.setStatus(Racun.Status.STORNIRAN);
        assertEquals(Racun.Status.STORNIRAN, racun.getStatus());
    }

    @Test
    void nacinPlacanjaEnum() {
        racun.setNacinPlacanja(Racun.NacinPlacanja.KARTICA);
        assertEquals(Racun.NacinPlacanja.KARTICA, racun.getNacinPlacanja());
    }

    @Test
    void settersAndGettersWork() {
        racun.setZki("abc123def456abc123def456abc12345");
        racun.setJir("550e8400-e29b-41d4-a716-446655440000");
        assertEquals("abc123def456abc123def456abc12345", racun.getZki());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", racun.getJir());
    }

    @Test
    void setStavkeReplacesList() {
        Artikl a = new Artikl("X", null, 10.0, 25.0, 1);
        a.setId(1);
        StavkaRacuna s = new StavkaRacuna(a, 1, 0.0);
        racun.setStavke(List.of(s));
        assertEquals(1, racun.getStavke().size());
    }

    @Test
    void vrijemeIzdavanjaIsSet() {
        LocalDateTime dt = LocalDateTime.of(2024, 6, 1, 12, 0, 0);
        racun.setVrijemeIzdavanja(dt);
        assertEquals(dt, racun.getVrijemeIzdavanja());
    }
}
