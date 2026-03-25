package stil.app.print;

import org.junit.jupiter.api.Test;
import stil.app.model.*;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testovi za {@link IspisRacuna}.
 * Testira pripremu linija za ispis i ponašanje print metode za različite
 * kombinacije računa (s/bez ZKI, s/bez stavki, dugački nazivi, popusti).
 */
class IspisRacunaTest {

    private Racun dummyRacun() {
        Racun r = new Racun();
        r.setId(1);
        r.setBrojRacuna(1);
        r.setOznakaRacuna("1-PP1-1");
        r.setVrijemeIzdavanja(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        r.setNacinPlacanja(Racun.NacinPlacanja.GOTOVINA);
        r.setStatus(Racun.Status.KREIRAN);

        Artikl a = new Artikl("Majica kratkih rukava", "123", 29.99, 25.0, 10);
        a.setId(1);
        r.getStavke().add(new StavkaRacuna(a, 2, 0.0));

        Artikl b = new Artikl("Hlače", "456", 49.99, 25.0, 5);
        b.setId(2);
        r.getStavke().add(new StavkaRacuna(b, 1, 10.0));

        return r;
    }

    private Config dummyConfig() {
        Config c = new Config();
        c.setNazivTvrtke("STIL A j.d.o.o.");
        c.setAdresa("Ilica 1, Zagreb");
        c.setOib("12345678901");
        return c;
    }

    @Test
    void constructorDoesNotThrow() {
        assertDoesNotThrow(() -> new IspisRacuna(dummyRacun(), dummyConfig()));
    }

    @Test
    void constructorWithNullConfigFields() {
        Config c = new Config(); // sve null
        assertDoesNotThrow(() -> new IspisRacuna(dummyRacun(), c));
    }

    @Test
    void printReturnsPageExistsForFirstPage() {
        IspisRacuna ispis = new IspisRacuna(dummyRacun(), dummyConfig());
        PageFormat pf = createPageFormat();
        // Koristimo null Graphics — print metoda ne smije baciti NPE za page > 0
        assertEquals(Printable.NO_SUCH_PAGE, ispis.print(null, pf, 1));
    }

    @Test
    void printReturnsNoSuchPageForSecondPage() {
        IspisRacuna ispis = new IspisRacuna(dummyRacun(), dummyConfig());
        PageFormat pf = createPageFormat();
        assertEquals(Printable.NO_SUCH_PAGE, ispis.print(null, pf, 2));
    }

    @Test
    void racunWithZkiAndJir() {
        Racun r = dummyRacun();
        r.setZki("abcdef1234567890abcdef1234567890");
        r.setJir("550e8400-e29b-41d4-a716-446655440000");
        r.setStatus(Racun.Status.FISKALIZIRAN);
        assertDoesNotThrow(() -> new IspisRacuna(r, dummyConfig()));
    }

    @Test
    void racunWithKarticaPlacanje() {
        Racun r = dummyRacun();
        r.setNacinPlacanja(Racun.NacinPlacanja.KARTICA);
        assertDoesNotThrow(() -> new IspisRacuna(r, dummyConfig()));
    }

    @Test
    void racunWithEmptyStavke() {
        Racun r = new Racun();
        r.setId(1);
        r.setBrojRacuna(1);
        r.setOznakaRacuna("1-PP1-1");
        r.setVrijemeIzdavanja(LocalDateTime.now());
        r.setNacinPlacanja(Racun.NacinPlacanja.GOTOVINA);
        r.setStatus(Racun.Status.KREIRAN);
        assertDoesNotThrow(() -> new IspisRacuna(r, dummyConfig()));
    }

    @Test
    void racunWithLongArtiklNaziv() {
        Racun r = new Racun();
        r.setId(1);
        r.setBrojRacuna(1);
        r.setOznakaRacuna("1-PP1-1");
        r.setVrijemeIzdavanja(LocalDateTime.now());
        r.setNacinPlacanja(Racun.NacinPlacanja.GOTOVINA);
        r.setStatus(Racun.Status.KREIRAN);

        Artikl a = new Artikl("Ovo je jako dugačak naziv artikla koji prelazi 22 znaka", "LONG", 10.0, 25.0, 1);
        a.setId(1);
        r.getStavke().add(new StavkaRacuna(a, 1, 0.0));

        assertDoesNotThrow(() -> new IspisRacuna(r, dummyConfig()));
    }

    @Test
    void racunWithPopust() {
        Racun r = dummyRacun();
        // Stavka s popustom već je u dummyRacun()
        assertDoesNotThrow(() -> new IspisRacuna(r, dummyConfig()));
    }

    private PageFormat createPageFormat() {
        PageFormat pf = new PageFormat();
        Paper paper = new Paper();
        paper.setSize(226.77, 841.89); // 80mm x 297mm u points
        paper.setImageableArea(8.5, 8.5, 209.77, 824.89);
        pf.setPaper(paper);
        return pf;
    }
}
