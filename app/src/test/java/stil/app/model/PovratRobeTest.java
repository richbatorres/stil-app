package stil.app.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testovi za model {@link PovratRobe}.
 * Testira konstruktore, izračune iznosa i promjene zalihe.
 */
class PovratRobeTest {

    @Test
    void defaultConstructorCreatesEmptyPovrat() {
        PovratRobe p = new PovratRobe();
        assertEquals(0, p.getId());
        assertEquals(0, p.getArtiklId());
        assertNull(p.getArtiklNaziv());
        assertEquals(0, p.getKolicina());
        assertEquals(0.0, p.getCijenaPoKomadu());
        assertNull(p.getTipPovrata());
    }

    @Test
    void fullConstructorSetsAllFields() {
        PovratRobe p = new PovratRobe(1, "Majica", 3, 29.99,
            PovratRobe.TipPovrata.OD_KUPCA, "Pogrešna veličina");
        assertEquals(1, p.getArtiklId());
        assertEquals("Majica", p.getArtiklNaziv());
        assertEquals(3, p.getKolicina());
        assertEquals(29.99, p.getCijenaPoKomadu());
        assertEquals(PovratRobe.TipPovrata.OD_KUPCA, p.getTipPovrata());
        assertEquals("Pogrešna veličina", p.getRazlog());
        assertNotNull(p.getVrijemePovrata());
    }

    @Test
    void getUkupniIznosCalculatesCorrectly() {
        PovratRobe p = new PovratRobe(1, "Hlače", 2, 49.99,
            PovratRobe.TipPovrata.DOBAVLJACU, null);
        // 2 * 49.99 = 99.98
        assertEquals(99.98, p.getUkupniIznos(), 0.001);
    }

    @Test
    void getUkupniIznosSingleItem() {
        PovratRobe p = new PovratRobe(1, "Jakna", 1, 99.99,
            PovratRobe.TipPovrata.OD_KUPCA, null);
        assertEquals(99.99, p.getUkupniIznos(), 0.001);
    }

    @Test
    void getPromjenaZaliheOdKupcaIsPositive() {
        PovratRobe p = new PovratRobe(1, "Majica", 3, 20.0,
            PovratRobe.TipPovrata.OD_KUPCA, null);
        // Kupac vraća robu → zaliha raste
        assertEquals(3, p.getPromjenaZalihe());
    }

    @Test
    void getPromjenaZaliheDobavljacuIsNegative() {
        PovratRobe p = new PovratRobe(1, "Majica", 2, 20.0,
            PovratRobe.TipPovrata.DOBAVLJACU, null);
        // Vraćamo dobavljaču → zaliha pada
        assertEquals(-2, p.getPromjenaZalihe());
    }

    @Test
    void settersAndGettersWork() {
        PovratRobe p = new PovratRobe();
        LocalDateTime dt = LocalDateTime.of(2024, 3, 15, 14, 30, 0);
        p.setId(5);
        p.setArtiklId(2);
        p.setArtiklNaziv("Šal");
        p.setDobavljacId(1);
        p.setDobavljacNaziv("Tekstil d.o.o.");
        p.setKolicina(4);
        p.setCijenaPoKomadu(15.00);
        p.setTipPovrata(PovratRobe.TipPovrata.DOBAVLJACU);
        p.setVrijemePovrata(dt);
        p.setRazlog("Oštećena roba");

        assertEquals(5, p.getId());
        assertEquals(2, p.getArtiklId());
        assertEquals("Šal", p.getArtiklNaziv());
        assertEquals(1, p.getDobavljacId());
        assertEquals("Tekstil d.o.o.", p.getDobavljacNaziv());
        assertEquals(4, p.getKolicina());
        assertEquals(15.00, p.getCijenaPoKomadu());
        assertEquals(PovratRobe.TipPovrata.DOBAVLJACU, p.getTipPovrata());
        assertEquals(dt, p.getVrijemePovrata());
        assertEquals("Oštećena roba", p.getRazlog());
    }

    @Test
    void tipPovrataBothValuesExist() {
        assertNotNull(PovratRobe.TipPovrata.OD_KUPCA);
        assertNotNull(PovratRobe.TipPovrata.DOBAVLJACU);
        assertEquals(2, PovratRobe.TipPovrata.values().length);
    }

    @Test
    void razlogCanBeNull() {
        PovratRobe p = new PovratRobe(1, "Test", 1, 10.0,
            PovratRobe.TipPovrata.OD_KUPCA, null);
        assertNull(p.getRazlog());
    }

    @Test
    void getUkupniIznosWithZeroPrice() {
        PovratRobe p = new PovratRobe(1, "Uzorak", 5, 0.0,
            PovratRobe.TipPovrata.DOBAVLJACU, null);
        assertEquals(0.0, p.getUkupniIznos(), 0.001);
    }
}
