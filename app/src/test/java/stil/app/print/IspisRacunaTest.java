package stil.app.print;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import stil.app.model.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testovi za {@link IspisRacuna}.
 * Testira PDF generiranje i robusnost spremanja.
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

        Artikl b = new Artikl("Hlace", "456", 49.99, 25.0, 5);
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

    @TempDir
    Path tempDir;

    @Test
    void spremiRacun_KreiraDatetekuSTimestampom() throws Exception {
        IspisRacuna ispis = new IspisRacuna(dummyRacun(), dummyConfig());
        Path rezultat = ispis.spremiRacun(tempDir.toString());
        assertTrue(Files.exists(rezultat));
        assertTrue(rezultat.getFileName().toString().startsWith("racun_"));
        assertTrue(rezultat.getFileName().toString().endsWith(".pdf"));
    }

    @Test
    void spremiRacun_DatotekaImaSmislenuVelicinu() throws Exception {
        IspisRacuna ispis = new IspisRacuna(dummyRacun(), dummyConfig());
        Path rezultat = ispis.spremiRacun(tempDir.toString());
        assertTrue(Files.size(rezultat) > 200);
    }

    @Test
    void spremiRacun_KreiraPoddirektorijAkoNePostoji() throws Exception {
        Path poddir = tempDir.resolve("novi").resolve("poddir");
        IspisRacuna ispis = new IspisRacuna(dummyRacun(), dummyConfig());
        Path rezultat = ispis.spremiRacun(poddir.toString());
        assertTrue(Files.exists(rezultat));
    }

    @Test
    void spremiRacun_NazivSadrziTimestamp() throws Exception {
        Racun r = dummyRacun();
        r.setVrijemeIzdavanja(LocalDateTime.of(2024, 6, 15, 14, 30, 45));
        IspisRacuna ispis = new IspisRacuna(r, dummyConfig());
        Path rezultat = ispis.spremiRacun(tempDir.toString());
        assertTrue(rezultat.getFileName().toString().contains("20240615_143045"));
    }

    @Test
    void constructorDoesNotThrow() {
        assertDoesNotThrow(() -> new IspisRacuna(dummyRacun(), dummyConfig()));
    }

    @Test
    void constructorWithNullConfigFields() {
        Config c = new Config();
        assertDoesNotThrow(() -> new IspisRacuna(dummyRacun(), c));
    }

    @Test
    void racunWithZkiAndJir() throws Exception {
        Racun r = dummyRacun();
        r.setZki("abcdef1234567890abcdef1234567890");
        r.setJir("550e8400-e29b-41d4-a716-446655440000");
        r.setStatus(Racun.Status.FISKALIZIRAN);
        IspisRacuna ispis = new IspisRacuna(r, dummyConfig());
        Path rezultat = ispis.spremiRacun(tempDir.toString());
        assertTrue(Files.exists(rezultat));
    }

    @Test
    void racunWithKarticaPlacanje() throws Exception {
        Racun r = dummyRacun();
        r.setNacinPlacanja(Racun.NacinPlacanja.KARTICA);
        IspisRacuna ispis = new IspisRacuna(r, dummyConfig());
        Path rezultat = ispis.spremiRacun(tempDir.toString());
        assertTrue(Files.exists(rezultat));
    }

    @Test
    void racunWithLongArtiklNaziv() throws Exception {
        Racun r = new Racun();
        r.setId(1); r.setBrojRacuna(1); r.setOznakaRacuna("1-PP1-1");
        r.setVrijemeIzdavanja(LocalDateTime.now());
        r.setNacinPlacanja(Racun.NacinPlacanja.GOTOVINA);
        r.setStatus(Racun.Status.KREIRAN);
        Artikl a = new Artikl("Ovo je jako dugacak naziv artikla koji prelazi 20 znaka", "LONG", 10.0, 25.0, 1);
        a.setId(1);
        r.getStavke().add(new StavkaRacuna(a, 1, 0.0));
        IspisRacuna ispis = new IspisRacuna(r, dummyConfig());
        Path rezultat = ispis.spremiRacun(tempDir.toString());
        assertTrue(Files.exists(rezultat));
    }

    @Test
    void racunWithPopust() throws Exception {
        IspisRacuna ispis = new IspisRacuna(dummyRacun(), dummyConfig());
        Path rezultat = ispis.spremiRacun(tempDir.toString());
        assertTrue(Files.exists(rezultat));
    }
}
