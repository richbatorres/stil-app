package stil.app.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testovi za {@link IzvjestajPodaci} DTO.
 * Pokriva izračune bruto marže, neto prometa i sve getere/setere.
 */
class IzvjestajPodaciTest {

    @Test
    void getBrutoMarza_IzracunaTocno() {
        IzvjestajPodaci p = new IzvjestajPodaci();
        p.setUkupnaProdaja(1000.00);
        p.setUkupniPdvProdaja(200.00);  // neto prodaja = 800
        p.setUkupnaNabava(500.00);
        // marža = (1000 - 200) - 500 = 300
        assertEquals(300.00, p.getBrutoMarza(), 0.001);
    }

    @Test
    void getNetoPromet_IzracunaTocno() {
        IzvjestajPodaci p = new IzvjestajPodaci();
        p.setUkupnaProdaja(1000.00);
        p.setIznosPovratOdKupca(150.00);
        // neto = 1000 - 150 = 850
        assertEquals(850.00, p.getNetoPromet(), 0.001);
    }

    @Test
    void getBrutoMarza_NultiPodaci() {
        IzvjestajPodaci p = new IzvjestajPodaci();
        assertEquals(0.0, p.getBrutoMarza(), 0.001);
    }

    @Test
    void getNetoPromet_BezPovrata() {
        IzvjestajPodaci p = new IzvjestajPodaci();
        p.setUkupnaProdaja(500.00);
        assertEquals(500.00, p.getNetoPromet(), 0.001);
    }

    @Test
    void getBrutoMarza_NegativnaMarza() {
        // Nabava veća od prodaje bez PDV-a
        IzvjestajPodaci p = new IzvjestajPodaci();
        p.setUkupnaProdaja(100.00);
        p.setUkupniPdvProdaja(20.00);
        p.setUkupnaNabava(200.00);
        assertEquals(-120.00, p.getBrutoMarza(), 0.001);
    }

    @Test
    void setteri_RadeTocno() {
        IzvjestajPodaci p = new IzvjestajPodaci();
        p.setRazdobljeOd("2024-01-01T00:00:00");
        p.setRazdobljeDo("2024-01-31T23:59:59");
        p.setBrojRacuna(42);
        p.setBrojNabava(10);
        p.setBrojPovratOdKupca(3);
        p.setBrojPovratDobavljacu(1);
        p.setProdajaGotovina(600.00);
        p.setProdajaKartica(400.00);
        p.setIznosPovratDobavljacu(50.00);

        assertEquals("2024-01-01T00:00:00", p.getRazdobljeOd());
        assertEquals("2024-01-31T23:59:59", p.getRazdobljeDo());
        assertEquals(42, p.getBrojRacuna());
        assertEquals(10, p.getBrojNabava());
        assertEquals(3, p.getBrojPovratOdKupca());
        assertEquals(1, p.getBrojPovratDobavljacu());
        assertEquals(600.00, p.getProdajaGotovina(), 0.001);
        assertEquals(400.00, p.getProdajaKartica(), 0.001);
        assertEquals(50.00, p.getIznosPovratDobavljacu(), 0.001);
    }
}
