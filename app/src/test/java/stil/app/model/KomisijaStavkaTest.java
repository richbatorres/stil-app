package stil.app.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testovi za {@link KomisijaStavka}.
 * Testira izračun ostalo, vrijednostProdano i vrijednostPovrat.
 */
class KomisijaStavkaTest {

    @Test
    void ostaloJeNabavljenoMinusProdano() {
        KomisijaStavka s = new KomisijaStavka(1, "Majica", 10, 6, 15.00);
        assertEquals(4, s.getOstalo());
    }

    @Test
    void vrijednostProdanoJeProdanoXCijena() {
        KomisijaStavka s = new KomisijaStavka(1, "Majica", 10, 6, 15.00);
        assertEquals(90.00, s.getVrijednostProdano(), 0.001);
    }

    @Test
    void vrijednostPovratJeOstaloXCijena() {
        KomisijaStavka s = new KomisijaStavka(1, "Majica", 10, 6, 15.00);
        assertEquals(60.00, s.getVrijednostPovrat(), 0.001);
    }

    @Test
    void sveProdanoOstaloJeNula() {
        KomisijaStavka s = new KomisijaStavka(1, "Hlače", 5, 5, 20.00);
        assertEquals(0, s.getOstalo());
        assertEquals(0.00, s.getVrijednostPovrat(), 0.001);
    }

    @Test
    void nistaProdanoOstaloJeNabavljeno() {
        KomisijaStavka s = new KomisijaStavka(1, "Jakna", 8, 0, 50.00);
        assertEquals(8, s.getOstalo());
        assertEquals(0.00, s.getVrijednostProdano(), 0.001);
        assertEquals(400.00, s.getVrijednostPovrat(), 0.001);
    }

    @Test
    void setNabavljenoAzuriraOstalo() {
        KomisijaStavka s = new KomisijaStavka(1, "Test", 5, 3, 10.00);
        s.setNabavljeno(10);
        assertEquals(7, s.getOstalo());
    }

    @Test
    void setProdanoAzuriraOstalo() {
        KomisijaStavka s = new KomisijaStavka(1, "Test", 10, 3, 10.00);
        s.setProdano(7);
        assertEquals(3, s.getOstalo());
    }
}
