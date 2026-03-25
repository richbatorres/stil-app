package stil.app.fisk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testovi za {@link FiskalizacijaZahtjev} DTO.
 * Testira da su svi geteri/seteri ispravni i da su početne vrijednosti null.
 */
class FiskalizacijaZahtjevTest {

    @Test
    void settersAndGettersWork() {
        FiskalizacijaZahtjev z = new FiskalizacijaZahtjev();
        z.setOib("12345678901");
        z.setDatumVrijemePoruke("15.01.2024T10:30:00");
        z.setDatumVrijemeRacuna("15.01.2024T10:30:00");
        z.setBrojRacuna("1");
        z.setOznakaPosProstora("PP1");
        z.setOznakaUredaja("1");
        z.setIznosUkupno("100.00");
        z.setOsnovica("80.00");
        z.setIznosPdv("20.00");
        z.setNacinPlacanja("G");
        z.setZki("abc123def456abc123def456abc12345");

        assertEquals("12345678901", z.getOib());
        assertEquals("15.01.2024T10:30:00", z.getDatumVrijemePoruke());
        assertEquals("15.01.2024T10:30:00", z.getDatumVrijemeRacuna());
        assertEquals("1", z.getBrojRacuna());
        assertEquals("PP1", z.getOznakaPosProstora());
        assertEquals("1", z.getOznakaUredaja());
        assertEquals("100.00", z.getIznosUkupno());
        assertEquals("80.00", z.getOsnovica());
        assertEquals("20.00", z.getIznosPdv());
        assertEquals("G", z.getNacinPlacanja());
        assertEquals("abc123def456abc123def456abc12345", z.getZki());
    }

    @Test
    void allFieldsNullByDefault() {
        FiskalizacijaZahtjev z = new FiskalizacijaZahtjev();
        assertNull(z.getOib());
        assertNull(z.getDatumVrijemePoruke());
        assertNull(z.getDatumVrijemeRacuna());
        assertNull(z.getBrojRacuna());
        assertNull(z.getOznakaPosProstora());
        assertNull(z.getOznakaUredaja());
        assertNull(z.getIznosUkupno());
        assertNull(z.getOsnovica());
        assertNull(z.getIznosPdv());
        assertNull(z.getNacinPlacanja());
        assertNull(z.getZki());
    }

    @Test
    void nacinPlacanjaKartica() {
        FiskalizacijaZahtjev z = new FiskalizacijaZahtjev();
        z.setNacinPlacanja("K");
        assertEquals("K", z.getNacinPlacanja());
    }
}
