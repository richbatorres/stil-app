package stil.app.db;

import org.junit.jupiter.api.*;
import stil.app.model.*;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integracijski testovi za {@link DatabaseManager}.
 * Svaki test dobiva svježu izoliranu SQLite bazu putem refleksije
 * (zamjena connection fielda s privremenom test bazom).
 * Pokriva CRUD operacije za config, artikl, račun, dobavljača i povrat robe.
 */
class DatabaseManagerTest {

    private static final String TEST_DB = "jdbc:sqlite::memory:";

    @BeforeEach
    void setUp() throws Exception {
        // Resetiraj singleton
        Field instanceField = DatabaseManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        DatabaseManager existing = (DatabaseManager) instanceField.get(null);
        if (existing != null) {
            Field connField = DatabaseManager.class.getDeclaredField("connection");
            connField.setAccessible(true);
            Connection conn = (Connection) connField.get(existing);
            if (conn != null && !conn.isClosed()) conn.close();
            instanceField.set(null, null);
        }

        // Postavi URL na test bazu
        Field urlField = DatabaseManager.class.getDeclaredField("DB_URL");
        urlField.setAccessible(true);
        // DB_URL je static final — ne možemo ga promijeniti direktno
        // Umjesto toga, kreiramo instancu ručno s test konekcijom
        Connection testConn = DriverManager.getConnection(TEST_DB);
        testConn.createStatement().execute("PRAGMA foreign_keys = ON");

        // Obrisi sve podatke (redoslijed bitan zbog FK)
        testConn.createStatement().execute("DROP TABLE IF EXISTS stavka_racuna");
        testConn.createStatement().execute("DROP TABLE IF EXISTS racun");
        testConn.createStatement().execute("DROP TABLE IF EXISTS povrat_robe");
        testConn.createStatement().execute("DROP TABLE IF EXISTS nabava");
        testConn.createStatement().execute("DROP TABLE IF EXISTS artikl_dobavljac");
        testConn.createStatement().execute("DROP TABLE IF EXISTS artikl");
        testConn.createStatement().execute("DROP TABLE IF EXISTS dobavljac");
        testConn.createStatement().execute("DROP TABLE IF EXISTS config");

        // Kreiraj novu instancu s test konekcijom
        var ctor = DatabaseManager.class.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        // Konstruktor poziva DriverManager.getConnection(DB_URL) — ne možemo ga zaobići
        // Koristimo drugi pristup: kreiramo instancu i odmah zamjenjujemo connection

        // Privremeno postavi DB_URL na test bazu putem System property
        // Jedini siguran način: koristiti reflection na connection field NAKON što se instanca kreira
        // ali konstruktor će pokušati otvoriti stil.db

        // Rješenje: kreiramo "praznu" instancu bez pozivanja konstruktora
        // koristeći sun.misc.Unsafe ili Objenesis — nije dostupno
        // Koristimo jednostavniji pristup: direktno manipuliramo connection fieldom

        DatabaseManager dm = (DatabaseManager) ctor.newInstance(); // otvara stil.db
        Field connField = DatabaseManager.class.getDeclaredField("connection");
        connField.setAccessible(true);
        Connection oldConn = (Connection) connField.get(dm);
        if (oldConn != null && !oldConn.isClosed()) oldConn.close();
        connField.set(dm, testConn);

        // Reinicijaliziraj shemu na test bazi
        java.lang.reflect.Method init = DatabaseManager.class.getDeclaredMethod("initSchema");
        init.setAccessible(true);
        init.invoke(dm);

        instanceField.set(null, dm);
    }

    @AfterEach
    void tearDown() throws Exception {
        Field instanceField = DatabaseManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        DatabaseManager existing = (DatabaseManager) instanceField.get(null);
        if (existing != null) {
            Field connField = DatabaseManager.class.getDeclaredField("connection");
            connField.setAccessible(true);
            Connection conn = (Connection) connField.get(existing);
            if (conn != null && !conn.isClosed()) conn.close();
        }
        instanceField.set(null, null);
    }

    private DatabaseManager db() throws SQLException {
        return DatabaseManager.getInstance();
    }

    // -------------------------------------------------------------------------
    // CONFIG testovi
    // -------------------------------------------------------------------------

    @Test
    void loadConfigReturnsDefaultsWhenEmpty() throws Exception {
        Config config = db().loadConfig();
        assertNotNull(config);
        assertNull(config.getNazivTvrtke());
        assertTrue(config.isFiskTestMode());
    }

    @Test
    void saveAndLoadConfig() throws Exception {
        Config config = new Config();
        config.setNazivTvrtke("STIL A j.d.o.o.");
        config.setOib("12345678901");
        config.setAdresa("Ilica 1, Zagreb");
        config.setOznakaPosProstora("PP1");
        config.setOznakaUredaja("1");
        config.setPutanjaCertifikata("/cert.p12");
        config.setLozinkaCertifikata("lozinka");
        config.setPin("1234");
        config.setFiskTestMode(false);

        db().saveConfig(config);
        Config loaded = db().loadConfig();

        assertEquals("STIL A j.d.o.o.", loaded.getNazivTvrtke());
        assertEquals("12345678901", loaded.getOib());
        assertEquals("Ilica 1, Zagreb", loaded.getAdresa());
        assertEquals("PP1", loaded.getOznakaPosProstora());
        assertEquals("1", loaded.getOznakaUredaja());
        assertEquals("/cert.p12", loaded.getPutanjaCertifikata());
        assertEquals("lozinka", loaded.getLozinkaCertifikata());
        assertEquals("1234", loaded.getPin());
        assertFalse(loaded.isFiskTestMode());
    }

    @Test
    void saveConfigUpsertUpdatesExistingValue() throws Exception {
        Config c1 = new Config();
        c1.setNazivTvrtke("Stari naziv");
        db().saveConfig(c1);

        Config c2 = new Config();
        c2.setNazivTvrtke("Novi naziv");
        db().saveConfig(c2);

        assertEquals("Novi naziv", db().loadConfig().getNazivTvrtke());
    }

    @Test
    void configFiskTestModeTrue() throws Exception {
        Config c = new Config();
        c.setFiskTestMode(true);
        db().saveConfig(c);
        assertTrue(db().loadConfig().isFiskTestMode());
    }

    // -------------------------------------------------------------------------
    // ARTIKL testovi
    // -------------------------------------------------------------------------

    @Test
    void getArtikliReturnsEmptyListInitially() throws Exception {
        assertTrue(db().getArtikli().isEmpty());
    }

    @Test
    void saveArtiklInsertsNewArtikl() throws Exception {
        Artikl a = new Artikl("Majica Test", "TEST001", 29.99, 25.0, 10);
        db().saveArtikl(a);
        assertTrue(a.getId() > 0);
        assertEquals(1, db().getArtikli().size());
        assertEquals("Majica Test", db().getArtikli().get(0).getNaziv());
    }

    @Test
    void saveArtiklUpdatesExistingArtikl() throws Exception {
        Artikl a = new Artikl("Stara Majica", "UPD001", 20.00, 25.0, 5);
        db().saveArtikl(a);
        a.setNaziv("Nova Majica");
        a.setCijena(25.00);
        db().saveArtikl(a);

        List<Artikl> lista = db().getArtikli();
        assertEquals(1, lista.size());
        assertEquals("Nova Majica", lista.get(0).getNaziv());
        assertEquals(25.00, lista.get(0).getCijena());
    }

    @Test
    void getArtiklByBarkodReturnsCorrectArtikl() throws Exception {
        Artikl a = new Artikl("Hlace Barkod", "BARKOD123", 49.99, 25.0, 3);
        db().saveArtikl(a);

        Artikl found = db().getArtiklByBarkod("BARKOD123");
        assertNotNull(found);
        assertEquals("Hlace Barkod", found.getNaziv());
    }

    @Test
    void getArtiklByBarkodReturnsNullForUnknown() throws Exception {
        assertNull(db().getArtiklByBarkod("NEPOSTOJECI_XYZ"));
    }

    @Test
    void deleteArtiklRemovesFromDb() throws Exception {
        Artikl a = new Artikl("Za Brisanje", "DEL001", 10.00, 25.0, 1);
        db().saveArtikl(a);
        db().deleteArtikl(a.getId());
        assertTrue(db().getArtikli().isEmpty());
    }

    @Test
    void updateKolicinaPovecava() throws Exception {
        Artikl a = new Artikl("Kolicina Test", "KOL001", 15.00, 25.0, 5);
        db().saveArtikl(a);
        db().updateKolicina(a.getId(), 3);
        assertEquals(8, db().getArtikli().get(0).getKolicinaNaSkladistu());
    }

    @Test
    void updateKolicinaSmanji() throws Exception {
        Artikl a = new Artikl("Kolicina Minus", "KOL002", 15.00, 25.0, 10);
        db().saveArtikl(a);
        db().updateKolicina(a.getId(), -4);
        assertEquals(6, db().getArtikli().get(0).getKolicinaNaSkladistu());
    }

    @Test
    void getArtikliSortedByNaziv() throws Exception {
        db().saveArtikl(new Artikl("Zimska jakna", "Z001", 99.0, 25.0, 2));
        db().saveArtikl(new Artikl("Abrigos", "A001", 79.0, 25.0, 1));
        db().saveArtikl(new Artikl("Majica kratka", "M001", 19.0, 25.0, 5));

        List<Artikl> lista = db().getArtikli();
        assertEquals(3, lista.size());
        for (int i = 0; i < lista.size() - 1; i++) {
            assertTrue(lista.get(i).getNaziv().compareToIgnoreCase(lista.get(i + 1).getNaziv()) <= 0);
        }
    }

    // -------------------------------------------------------------------------
    // RACUN testovi
    // -------------------------------------------------------------------------

    @Test
    void getSljedeciBrojRacunaStartsAt1() throws Exception {
        assertEquals(1, db().getSljedeciBrojRacuna());
    }

    @Test
    void saveRacunPersistsRacunAndStavke() throws Exception {
        Artikl a = new Artikl("Racun Artikl", "RAC001", 20.00, 25.0, 10);
        db().saveArtikl(a);

        Racun r = buildRacun(1, Racun.NacinPlacanja.GOTOVINA);
        r.getStavke().add(new StavkaRacuna(a, 2, 0.0));
        db().saveRacun(r);

        assertTrue(r.getId() > 0);
    }

    @Test
    void saveRacunDecreasesKolicina() throws Exception {
        Artikl a = new Artikl("Zaliha Test", "ZAL001", 30.00, 25.0, 15);
        db().saveArtikl(a);

        Racun r = buildRacun(1, Racun.NacinPlacanja.KARTICA);
        r.getStavke().add(new StavkaRacuna(a, 3, 0.0));
        db().saveRacun(r);

        assertEquals(12, db().getArtikli().get(0).getKolicinaNaSkladistu());
    }

    @Test
    void updateRacunFiskalizacija() throws Exception {
        Artikl a = new Artikl("Fisk Test", "FISK001", 50.00, 25.0, 5);
        db().saveArtikl(a);

        Racun r = buildRacun(1, Racun.NacinPlacanja.GOTOVINA);
        r.getStavke().add(new StavkaRacuna(a, 1, 0.0));
        db().saveRacun(r);

        assertDoesNotThrow(() -> db().updateRacunFiskalizacija(r.getId(),
            "abcdef1234567890abcdef1234567890",
            "550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    void getSljedeciBrojRacunaIncrementsAfterSave() throws Exception {
        Artikl a = new Artikl("Inkrement Test", "INC001", 10.00, 25.0, 5);
        db().saveArtikl(a);

        Racun r = buildRacun(1, Racun.NacinPlacanja.GOTOVINA);
        r.getStavke().add(new StavkaRacuna(a, 1, 0.0));
        db().saveRacun(r);

        assertEquals(2, db().getSljedeciBrojRacuna());
    }

    @Test
    void saveRacunWithMultipleStavke() throws Exception {
        Artikl a1 = new Artikl("Artikl 1", "A1", 10.00, 25.0, 10);
        Artikl a2 = new Artikl("Artikl 2", "A2", 20.00, 25.0, 10);
        db().saveArtikl(a1);
        db().saveArtikl(a2);

        Racun r = buildRacun(1, Racun.NacinPlacanja.GOTOVINA);
        r.getStavke().add(new StavkaRacuna(a1, 2, 0.0));
        r.getStavke().add(new StavkaRacuna(a2, 1, 10.0));
        db().saveRacun(r);

        assertTrue(r.getId() > 0);
        List<Artikl> lista = db().getArtikli();
        Artikl ua1 = lista.stream().filter(x -> x.getId() == a1.getId()).findFirst().orElseThrow();
        Artikl ua2 = lista.stream().filter(x -> x.getId() == a2.getId()).findFirst().orElseThrow();
        assertEquals(8, ua1.getKolicinaNaSkladistu());
        assertEquals(9, ua2.getKolicinaNaSkladistu());
    }

    private Racun buildRacun(int broj, Racun.NacinPlacanja nacin) {
        Racun r = new Racun();
        r.setBrojRacuna(broj);
        r.setOznakaRacuna(broj + "-PP1-1");
        r.setVrijemeIzdavanja(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        r.setNacinPlacanja(nacin);
        r.setStatus(Racun.Status.KREIRAN);
        return r;
    }

    // -------------------------------------------------------------------------
    // DOBAVLJAC testovi
    // -------------------------------------------------------------------------

    @Test
    void getDobavljaciReturnsEmptyListInitially() throws Exception {
        assertTrue(db().getDobavljaci().isEmpty());
    }

    @Test
    void saveDobavljacInsertsNew() throws Exception {
        Dobavljac d = new Dobavljac("Tekstil d.o.o.", "12345678901", "Ilica 1", "info@t.hr", "01-111");
        db().saveDobavljac(d);
        assertTrue(d.getId() > 0);
        List<Dobavljac> lista = db().getDobavljaci();
        assertEquals(1, lista.size());
        assertEquals("Tekstil d.o.o.", lista.get(0).getNaziv());
    }

    @Test
    void saveDobavljacUpdatesExisting() throws Exception {
        Dobavljac d = new Dobavljac("Stari naziv", null, null, null, null);
        db().saveDobavljac(d);
        d.setNaziv("Novi naziv");
        d.setEmail("novi@email.hr");
        db().saveDobavljac(d);

        List<Dobavljac> lista = db().getDobavljaci();
        assertEquals(1, lista.size());
        assertEquals("Novi naziv", lista.get(0).getNaziv());
        assertEquals("novi@email.hr", lista.get(0).getEmail());
    }

    @Test
    void deleteDobavljacRemovesFromDb() throws Exception {
        Dobavljac d = new Dobavljac("Za brisanje", null, null, null, null);
        db().saveDobavljac(d);
        db().deleteDobavljac(d.getId());
        assertTrue(db().getDobavljaci().isEmpty());
    }

    @Test
    void getDobavljaciSortedByNaziv() throws Exception {
        db().saveDobavljac(new Dobavljac("Zeta d.o.o.", null, null, null, null));
        db().saveDobavljac(new Dobavljac("Alpha d.o.o.", null, null, null, null));
        db().saveDobavljac(new Dobavljac("Moda Export", null, null, null, null));

        List<Dobavljac> lista = db().getDobavljaci();
        assertEquals(3, lista.size());
        for (int i = 0; i < lista.size() - 1; i++) {
            assertTrue(lista.get(i).getNaziv().compareToIgnoreCase(lista.get(i + 1).getNaziv()) <= 0);
        }
    }

    @Test
    void saveDobavljacWithNullOptionalFields() throws Exception {
        Dobavljac d = new Dobavljac("Minimalni", null, null, null, null);
        db().saveDobavljac(d);
        Dobavljac loaded = db().getDobavljaci().get(0);
        assertNull(loaded.getOib());
        assertNull(loaded.getEmail());
        assertNull(loaded.getTelefon());
    }

    // -------------------------------------------------------------------------
    // POVRAT ROBE testovi
    // -------------------------------------------------------------------------

    @Test
    void getPovratReturnsEmptyListInitially() throws Exception {
        assertTrue(db().getPovrati().isEmpty());
    }

    @Test
    void savePovratOdKupcaPovecavaZalihu() throws Exception {
        Artikl a = new Artikl("Povrat Test", "POV001", 20.00, 25.0, 5);
        db().saveArtikl(a);

        PovratRobe p = new PovratRobe(a.getId(), a.getNaziv(), 2, 20.00,
            PovratRobe.TipPovrata.OD_KUPCA, "Pogrešna veličina");
        p.setVrijemePovrata(LocalDateTime.now());
        db().savePovrat(p);

        assertTrue(p.getId() > 0);
        // Zaliha treba biti 5 + 2 = 7
        assertEquals(7, db().getArtikli().get(0).getKolicinaNaSkladistu());
    }

    @Test
    void savePovratDobavljacuSmanjiZalihu() throws Exception {
        Artikl a = new Artikl("Povrat Dobavljac", "POV002", 30.00, 25.0, 10);
        db().saveArtikl(a);

        PovratRobe p = new PovratRobe(a.getId(), a.getNaziv(), 3, 30.00,
            PovratRobe.TipPovrata.DOBAVLJACU, "Oštećena roba");
        p.setVrijemePovrata(LocalDateTime.now());
        db().savePovrat(p);

        // Zaliha treba biti 10 - 3 = 7
        assertEquals(7, db().getArtikli().get(0).getKolicinaNaSkladistu());
    }

    @Test
    void savePovratSDobavljacem() throws Exception {
        Artikl a = new Artikl("Povrat s Dob", "POV003", 25.00, 25.0, 8);
        db().saveArtikl(a);
        Dobavljac d = new Dobavljac("Tekstil d.o.o.", null, null, null, null);
        db().saveDobavljac(d);

        PovratRobe p = new PovratRobe(a.getId(), a.getNaziv(), 1, 25.00,
            PovratRobe.TipPovrata.DOBAVLJACU, null);
        p.setDobavljacId(d.getId());
        p.setDobavljacNaziv(d.getNaziv());
        p.setVrijemePovrata(LocalDateTime.now());
        db().savePovrat(p);

        List<PovratRobe> povrati = db().getPovrati();
        assertEquals(1, povrati.size());
        assertEquals("Tekstil d.o.o.", povrati.get(0).getDobavljacNaziv());
    }

    @Test
    void getPovratByArtiklReturnsOnlyThatArtikl() throws Exception {
        Artikl a1 = new Artikl("Artikl A", "PA1", 10.00, 25.0, 10);
        Artikl a2 = new Artikl("Artikl B", "PA2", 20.00, 25.0, 10);
        db().saveArtikl(a1);
        db().saveArtikl(a2);

        PovratRobe p1 = new PovratRobe(a1.getId(), a1.getNaziv(), 1, 10.00,
            PovratRobe.TipPovrata.OD_KUPCA, null);
        p1.setVrijemePovrata(LocalDateTime.now());
        PovratRobe p2 = new PovratRobe(a2.getId(), a2.getNaziv(), 2, 20.00,
            PovratRobe.TipPovrata.OD_KUPCA, null);
        p2.setVrijemePovrata(LocalDateTime.now());
        db().savePovrat(p1);
        db().savePovrat(p2);

        List<PovratRobe> povrati = db().getPovratByArtikl(a1.getId());
        assertEquals(1, povrati.size());
        assertEquals(a1.getId(), povrati.get(0).getArtiklId());
    }

    @Test
    void getPovratSortedByVrijemeDesc() throws Exception {
        Artikl a = new Artikl("Sort Test", "SORT1", 10.00, 25.0, 20);
        db().saveArtikl(a);

        PovratRobe p1 = new PovratRobe(a.getId(), a.getNaziv(), 1, 10.00,
            PovratRobe.TipPovrata.OD_KUPCA, "prvi");
        p1.setVrijemePovrata(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        PovratRobe p2 = new PovratRobe(a.getId(), a.getNaziv(), 1, 10.00,
            PovratRobe.TipPovrata.OD_KUPCA, "drugi");
        p2.setVrijemePovrata(LocalDateTime.of(2024, 6, 1, 10, 0, 0));
        db().savePovrat(p1);
        db().savePovrat(p2);

        List<PovratRobe> povrati = db().getPovrati();
        assertEquals(2, povrati.size());
        // Najnoviji prvi
        assertTrue(povrati.get(0).getVrijemePovrata().isAfter(povrati.get(1).getVrijemePovrata()));
    }

    // -------------------------------------------------------------------------
    // NABAVA testovi
    // -------------------------------------------------------------------------

    @Test
    void getNabaveReturnsEmptyListInitially() throws Exception {
        assertTrue(db().getNabave().isEmpty());
    }

    @Test
    void saveNabavaPovecavaZalihu() throws Exception {
        Artikl a = new Artikl("Nabava Test", "NAB001", 20.00, 25.0, 5);
        db().saveArtikl(a);

        Nabava n = new Nabava(a.getId(), a.getNaziv(), 10, 12.00, null);
        n.setVrijemeNabave(LocalDateTime.of(2024, 3, 1, 9, 0, 0));
        db().saveNabava(n);

        assertTrue(n.getId() > 0);
        // Zaliha treba biti 5 + 10 = 15
        assertEquals(15, db().getArtikli().get(0).getKolicinaNaSkladistu());
    }

    @Test
    void saveNabavaPersistsAllFields() throws Exception {
        Artikl a = new Artikl("Nabava Polja", "NAB002", 30.00, 25.0, 0);
        db().saveArtikl(a);
        Dobavljac d = new Dobavljac("Dobavljac Test", null, null, null, null);
        db().saveDobavljac(d);

        Nabava n = new Nabava(a.getId(), a.getNaziv(), 7, 18.50, "Proljetna kolekcija");
        n.setDobavljacId(d.getId());
        n.setDobavljacNaziv(d.getNaziv());
        LocalDateTime dt = LocalDateTime.of(2024, 4, 10, 14, 30, 0);
        n.setVrijemeNabave(dt);
        db().saveNabava(n);

        List<Nabava> lista = db().getNabave();
        assertEquals(1, lista.size());
        Nabava loaded = lista.get(0);
        assertEquals(a.getId(), loaded.getArtiklId());
        assertEquals(a.getNaziv(), loaded.getArtiklNaziv());
        assertEquals(d.getId(), loaded.getDobavljacId());
        assertEquals(d.getNaziv(), loaded.getDobavljacNaziv());
        assertEquals(7, loaded.getKolicina());
        assertEquals(18.50, loaded.getNabavnaCijena(), 0.001);
        assertEquals("Proljetna kolekcija", loaded.getNapomena());
        assertEquals(dt, loaded.getVrijemeNabave());
    }

    @Test
    void saveNabavaBezDobavljaca() throws Exception {
        Artikl a = new Artikl("Nabava BezDob", "NAB003", 10.00, 25.0, 0);
        db().saveArtikl(a);

        Nabava n = new Nabava(a.getId(), a.getNaziv(), 3, 5.00, null);
        n.setVrijemeNabave(LocalDateTime.now());
        db().saveNabava(n);

        Nabava loaded = db().getNabave().get(0);
        assertEquals(0, loaded.getDobavljacId()); // NULL u bazi → 0
        assertNull(loaded.getDobavljacNaziv());
    }

    @Test
    void getNabaveURasponu_VracaTocneNabave() throws Exception {
        Artikl a = new Artikl("Raspon Test", "RAS001", 10.00, 25.0, 0);
        db().saveArtikl(a);

        Nabava n1 = new Nabava(a.getId(), a.getNaziv(), 5, 10.00, null);
        n1.setVrijemeNabave(LocalDateTime.of(2024, 1, 15, 10, 0, 0));
        Nabava n2 = new Nabava(a.getId(), a.getNaziv(), 3, 10.00, null);
        n2.setVrijemeNabave(LocalDateTime.of(2024, 3, 15, 10, 0, 0));
        Nabava n3 = new Nabava(a.getId(), a.getNaziv(), 2, 10.00, null);
        n3.setVrijemeNabave(LocalDateTime.of(2024, 6, 15, 10, 0, 0));
        db().saveNabava(n1);
        db().saveNabava(n2);
        db().saveNabava(n3);

        List<Nabava> uRasponu = db().getNabaveURasponu(
            "2024-01-01T00:00:00", "2024-04-30T23:59:59");
        assertEquals(2, uRasponu.size());
    }

    @Test
    void getNabaveURasponu_PrazanRaspon() throws Exception {
        Artikl a = new Artikl("Prazno", "PAZ001", 10.00, 25.0, 0);
        db().saveArtikl(a);
        Nabava n = new Nabava(a.getId(), a.getNaziv(), 1, 5.00, null);
        n.setVrijemeNabave(LocalDateTime.of(2024, 6, 1, 10, 0, 0));
        db().saveNabava(n);

        List<Nabava> uRasponu = db().getNabaveURasponu(
            "2023-01-01T00:00:00", "2023-12-31T23:59:59");
        assertTrue(uRasponu.isEmpty());
    }

    // -------------------------------------------------------------------------
    // IZVJEŠTAJI testovi
    // -------------------------------------------------------------------------

    @Test
    void generirajIzvjestaj_PraznaBasaVracaNule() throws Exception {
        IzvjestajPodaci p = db().generirajIzvjestaj(
            "2024-01-01T00:00:00", "2024-12-31T23:59:59");
        assertEquals(0, p.getBrojRacuna());
        assertEquals(0.0, p.getUkupnaProdaja(), 0.001);
        assertEquals(0, p.getBrojNabava());
        assertEquals(0.0, p.getUkupnaNabava(), 0.001);
        assertEquals(0, p.getBrojPovratOdKupca());
        assertEquals(0, p.getBrojPovratDobavljacu());
    }

    @Test
    void generirajIzvjestaj_TocnaProdaja() throws Exception {
        Artikl a = new Artikl("Izvj Artikl", "IZV001", 100.00, 25.0, 10);
        db().saveArtikl(a);

        Racun r = buildRacun(1, Racun.NacinPlacanja.GOTOVINA);
        r.setVrijemeIzdavanja(LocalDateTime.of(2024, 5, 10, 10, 0, 0));
        r.getStavke().add(new StavkaRacuna(a, 2, 0.0)); // 2 × 100 = 200
        db().saveRacun(r);

        IzvjestajPodaci p = db().generirajIzvjestaj(
            "2024-05-01T00:00:00", "2024-05-31T23:59:59");
        assertEquals(1, p.getBrojRacuna());
        assertEquals(200.00, p.getUkupnaProdaja(), 0.001);
        assertEquals(200.00, p.getProdajaGotovina(), 0.001);
        assertEquals(0.0, p.getProdajaKartica(), 0.001);
        // PDV = 200 × 25 / 125 = 40
        assertEquals(40.00, p.getUkupniPdvProdaja(), 0.001);
    }

    @Test
    void generirajIzvjestaj_TocnaNabava() throws Exception {
        Artikl a = new Artikl("Izvj Nabava", "IZV002", 50.00, 25.0, 0);
        db().saveArtikl(a);

        Nabava n = new Nabava(a.getId(), a.getNaziv(), 5, 20.00, null);
        n.setVrijemeNabave(LocalDateTime.of(2024, 5, 15, 9, 0, 0));
        db().saveNabava(n);

        IzvjestajPodaci p = db().generirajIzvjestaj(
            "2024-05-01T00:00:00", "2024-05-31T23:59:59");
        assertEquals(1, p.getBrojNabava());
        assertEquals(100.00, p.getUkupnaNabava(), 0.001); // 5 × 20
    }

    @Test
    void generirajIzvjestaj_TocniPovrati() throws Exception {
        Artikl a = new Artikl("Izvj Povrat", "IZV003", 30.00, 25.0, 10);
        db().saveArtikl(a);

        PovratRobe p1 = new PovratRobe(a.getId(), a.getNaziv(), 2, 30.00,
            PovratRobe.TipPovrata.OD_KUPCA, null);
        p1.setVrijemePovrata(LocalDateTime.of(2024, 5, 20, 10, 0, 0));
        db().savePovrat(p1);

        PovratRobe p2 = new PovratRobe(a.getId(), a.getNaziv(), 1, 30.00,
            PovratRobe.TipPovrata.DOBAVLJACU, null);
        p2.setVrijemePovrata(LocalDateTime.of(2024, 5, 21, 10, 0, 0));
        db().savePovrat(p2);

        IzvjestajPodaci izvj = db().generirajIzvjestaj(
            "2024-05-01T00:00:00", "2024-05-31T23:59:59");
        assertEquals(1, izvj.getBrojPovratOdKupca());
        assertEquals(60.00, izvj.getIznosPovratOdKupca(), 0.001); // 2 × 30
        assertEquals(1, izvj.getBrojPovratDobavljacu());
        assertEquals(30.00, izvj.getIznosPovratDobavljacu(), 0.001);
    }

    @Test
    void generirajIzvjestaj_StorniranRacunNijeUbrajan() throws Exception {
        Artikl a = new Artikl("Storno Test", "STO001", 50.00, 25.0, 10);
        db().saveArtikl(a);

        Racun r = buildRacun(1, Racun.NacinPlacanja.GOTOVINA);
        r.setVrijemeIzdavanja(LocalDateTime.of(2024, 5, 10, 10, 0, 0));
        r.setStatus(Racun.Status.STORNIRAN);
        r.getStavke().add(new StavkaRacuna(a, 1, 0.0));
        db().saveRacun(r);

        IzvjestajPodaci p = db().generirajIzvjestaj(
            "2024-05-01T00:00:00", "2024-05-31T23:59:59");
        // Stornirani račun ne smije biti ubrojan u prodaju
        assertEquals(0, p.getBrojRacuna());
        assertEquals(0.0, p.getUkupnaProdaja(), 0.001);
    }

    @Test
    void generirajIzvjestaj_VanRasponaSeNeUbraja() throws Exception {
        Artikl a = new Artikl("Van Raspon", "VAN001", 100.00, 25.0, 10);
        db().saveArtikl(a);

        Racun r = buildRacun(1, Racun.NacinPlacanja.GOTOVINA);
        r.setVrijemeIzdavanja(LocalDateTime.of(2023, 12, 31, 23, 59, 59));
        r.getStavke().add(new StavkaRacuna(a, 1, 0.0));
        db().saveRacun(r);

        IzvjestajPodaci p = db().generirajIzvjestaj(
            "2024-01-01T00:00:00", "2024-12-31T23:59:59");
        assertEquals(0, p.getBrojRacuna());
    }

    @Test
    void generirajIzvjestaj_MjesovitaNacinPlacanja() throws Exception {
        Artikl a = new Artikl("Mjes Placanje", "MJE001", 100.00, 25.0, 20);
        db().saveArtikl(a);

        Racun r1 = buildRacun(1, Racun.NacinPlacanja.GOTOVINA);
        r1.setVrijemeIzdavanja(LocalDateTime.of(2024, 5, 1, 10, 0, 0));
        r1.getStavke().add(new StavkaRacuna(a, 1, 0.0)); // 100 gotovina

        Racun r2 = buildRacun(2, Racun.NacinPlacanja.KARTICA);
        r2.setVrijemeIzdavanja(LocalDateTime.of(2024, 5, 2, 10, 0, 0));
        r2.getStavke().add(new StavkaRacuna(a, 2, 0.0)); // 200 kartica

        db().saveRacun(r1);
        db().saveRacun(r2);

        IzvjestajPodaci p = db().generirajIzvjestaj(
            "2024-05-01T00:00:00", "2024-05-31T23:59:59");
        assertEquals(2, p.getBrojRacuna());
        assertEquals(300.00, p.getUkupnaProdaja(), 0.001);
        assertEquals(100.00, p.getProdajaGotovina(), 0.001);
        assertEquals(200.00, p.getProdajaKartica(), 0.001);
    }

    @Test
    void getRacuniURasponu_VracaTocneRacune() throws Exception {
        Artikl a = new Artikl("Raspon Racun", "RR001", 50.00, 25.0, 10);
        db().saveArtikl(a);

        Racun r1 = buildRacun(1, Racun.NacinPlacanja.GOTOVINA);
        r1.setVrijemeIzdavanja(LocalDateTime.of(2024, 2, 10, 10, 0, 0));
        r1.getStavke().add(new StavkaRacuna(a, 1, 0.0));

        Racun r2 = buildRacun(2, Racun.NacinPlacanja.KARTICA);
        r2.setVrijemeIzdavanja(LocalDateTime.of(2024, 5, 10, 10, 0, 0));
        r2.getStavke().add(new StavkaRacuna(a, 1, 0.0));

        db().saveRacun(r1);
        db().saveRacun(r2);

        List<Racun> lista = db().getRacuniURasponu(
            "2024-02-01T00:00:00", "2024-02-28T23:59:59");
        assertEquals(1, lista.size());
        assertEquals(1, lista.get(0).getBrojRacuna());
    }

    @Test
    void getRacuniURasponu_PrazanRaspon() throws Exception {
        List<Racun> lista = db().getRacuniURasponu(
            "2024-01-01T00:00:00", "2024-12-31T23:59:59");
        assertTrue(lista.isEmpty());
    }

    // -------------------------------------------------------------------------
    // ROBUSNI testovi — konzistentnost pri greškama
    // -------------------------------------------------------------------------

    @Test
    void saveNabava_RollbackAkoArtiklNePostoji() throws Exception {
        // SQLite s uključenim FK-ovima baca grešku ako artikl_id ne postoji
        // Transakcija se rollbacka — nabava se NE sprema, zaliha ostaje nepromijenjena
        Nabava n = new Nabava(99999, "Nepostojeci", 5, 10.00, null);
        n.setVrijemeNabave(LocalDateTime.now());
        assertThrows(SQLException.class, () -> db().saveNabava(n));
        // Rollback — nabava nije pohranjena
        assertEquals(0, db().getNabave().size());
    }

    @Test
    void generirajIzvjestaj_SPopustom() throws Exception {
        Artikl a = new Artikl("Popust Test", "POP001", 100.00, 25.0, 10);
        db().saveArtikl(a);

        Racun r = buildRacun(1, Racun.NacinPlacanja.GOTOVINA);
        r.setVrijemeIzdavanja(LocalDateTime.of(2024, 5, 10, 10, 0, 0));
        r.getStavke().add(new StavkaRacuna(a, 2, 10.0)); // 2 × 100 × 0.9 = 180
        db().saveRacun(r);

        IzvjestajPodaci p = db().generirajIzvjestaj(
            "2024-05-01T00:00:00", "2024-05-31T23:59:59");
        assertEquals(180.00, p.getUkupnaProdaja(), 0.001);
        // PDV = 180 × 25 / 125 = 36
        assertEquals(36.00, p.getUkupniPdvProdaja(), 0.001);
    }

    // -------------------------------------------------------------------------
    // STORNIRANJE testovi
    // -------------------------------------------------------------------------

    @Test
    void stornirajRacun_MijenjaSatusNaStorniran() throws Exception {
        Artikl a = new Artikl("Storno Artikl", "STR001", 50.00, 25.0, 10);
        db().saveArtikl(a);
        Racun r = buildRacun(1, Racun.NacinPlacanja.GOTOVINA);
        r.setVrijemeIzdavanja(LocalDateTime.of(2024, 6, 1, 10, 0, 0));
        r.getStavke().add(new StavkaRacuna(a, 1, 0.0));
        db().saveRacun(r);

        db().stornirajRacun(r.getId());

        // Stornirani račun ne smije biti u izvještaju
        IzvjestajPodaci p = db().generirajIzvjestaj(
            "2024-06-01T00:00:00", "2024-06-30T23:59:59");
        assertEquals(0, p.getBrojRacuna());
        assertEquals(0.0, p.getUkupnaProdaja(), 0.001);
    }

    @Test
    void stornirajRacun_NePostojeciIdNeBacaGresku() throws Exception {
        assertDoesNotThrow(() -> db().stornirajRacun(99999));
    }

    // -------------------------------------------------------------------------
    // ARTIKL-DOBAVLJAČ VEZA testovi
    // -------------------------------------------------------------------------

    @Test
    void getDobavljaciZaArtikl_PraznoInitially() throws Exception {
        Artikl a = new Artikl("Veza Test", "VEZ001", 10.00, 25.0, 0);
        db().saveArtikl(a);
        assertTrue(db().getDobavljaciZaArtikl(a.getId()).isEmpty());
    }

    @Test
    void setDobavljaciZaArtikl_PostavljaVezu() throws Exception {
        Artikl a = new Artikl("Veza Artikl", "VEZ002", 20.00, 25.0, 0);
        db().saveArtikl(a);
        Dobavljac d = new Dobavljac("Veza Dob", null, null, null, null);
        db().saveDobavljac(d);

        db().setDobavljaciZaArtikl(a.getId(), List.of(d.getId()));

        List<Dobavljac> vezani = db().getDobavljaciZaArtikl(a.getId());
        assertEquals(1, vezani.size());
        assertEquals(d.getId(), vezani.get(0).getId());
    }

    @Test
    void setDobavljaciZaArtikl_ZamjenjujePostojeceVeze() throws Exception {
        Artikl a = new Artikl("Zamjena Artikl", "ZAM001", 20.00, 25.0, 0);
        db().saveArtikl(a);
        Dobavljac d1 = new Dobavljac("Dob 1", null, null, null, null);
        Dobavljac d2 = new Dobavljac("Dob 2", null, null, null, null);
        db().saveDobavljac(d1);
        db().saveDobavljac(d2);

        db().setDobavljaciZaArtikl(a.getId(), List.of(d1.getId(), d2.getId()));
        assertEquals(2, db().getDobavljaciZaArtikl(a.getId()).size());

        // Zamijeni samo s d2
        db().setDobavljaciZaArtikl(a.getId(), List.of(d2.getId()));
        List<Dobavljac> vezani = db().getDobavljaciZaArtikl(a.getId());
        assertEquals(1, vezani.size());
        assertEquals(d2.getId(), vezani.get(0).getId());
    }

    @Test
    void setDobavljaciZaArtikl_PraznaListaBriseSveVeze() throws Exception {
        Artikl a = new Artikl("Brisanje Veza", "BRV001", 20.00, 25.0, 0);
        db().saveArtikl(a);
        Dobavljac d = new Dobavljac("Dob Brisanje", null, null, null, null);
        db().saveDobavljac(d);

        db().setDobavljaciZaArtikl(a.getId(), List.of(d.getId()));
        db().setDobavljaciZaArtikl(a.getId(), List.of()); // obrisi sve
        assertTrue(db().getDobavljaciZaArtikl(a.getId()).isEmpty());
    }

    @Test
    void getDobavljaciZaArtikl_ViseArtikalaIstogDobavljaca() throws Exception {
        Artikl a1 = new Artikl("Multi A1", "MA1", 10.00, 25.0, 0);
        Artikl a2 = new Artikl("Multi A2", "MA2", 20.00, 25.0, 0);
        db().saveArtikl(a1);
        db().saveArtikl(a2);
        Dobavljac d = new Dobavljac("Multi Dob", null, null, null, null);
        db().saveDobavljac(d);

        db().setDobavljaciZaArtikl(a1.getId(), List.of(d.getId()));
        db().setDobavljaciZaArtikl(a2.getId(), List.of(d.getId()));

        assertEquals(1, db().getDobavljaciZaArtikl(a1.getId()).size());
        assertEquals(1, db().getDobavljaciZaArtikl(a2.getId()).size());
    }

    // -------------------------------------------------------------------------
    // ZAŠTITA OD NEGATIVNIH ZALIHA
    // -------------------------------------------------------------------------

    @Test
    void updateKolicina_BacaGreskuAkoZalihaPostaneNegativna() throws Exception {
        Artikl a = new Artikl("Zaliha Neg", "ZN001", 10.00, 25.0, 3);
        db().saveArtikl(a);
        // Pokušaj smanjiti za više nego što ima
        assertThrows(SQLException.class, () -> db().updateKolicina(a.getId(), -4));
        // Zaliha mora ostati nepromijenjena
        assertEquals(3, db().getArtikli().get(0).getKolicinaNaSkladistu());
    }

    @Test
    void updateKolicina_DozvoljavaTocnoRaspolozivuKolicinu() throws Exception {
        Artikl a = new Artikl("Zaliha Tocno", "ZT001", 10.00, 25.0, 5);
        db().saveArtikl(a);
        assertDoesNotThrow(() -> db().updateKolicina(a.getId(), -5));
        assertEquals(0, db().getArtikli().get(0).getKolicinaNaSkladistu());
    }

    @Test
    void saveRacun_BacaGreskuAkoNemaDovoljnoZalihe() throws Exception {
        Artikl a = new Artikl("Premalo Zalihe", "PZ001", 50.00, 25.0, 2);
        db().saveArtikl(a);
        Racun r = buildRacun(1, Racun.NacinPlacanja.GOTOVINA);
        r.setVrijemeIzdavanja(LocalDateTime.of(2024, 5, 1, 10, 0, 0));
        r.getStavke().add(new StavkaRacuna(a, 5, 0.0)); // traži 5, ima samo 2
        // Transakcija mora biti rollbackana
        assertThrows(SQLException.class, () -> db().saveRacun(r));
        // Zaliha mora ostati nepromijenjena
        assertEquals(2, db().getArtikli().get(0).getKolicinaNaSkladistu());
        // Račun ne smije biti pohranjen
        assertEquals(1, db().getSljedeciBrojRacuna());
    }

    @Test
    void savePovrat_BacaGreskuAkoDobavljacuNemaDovoljnoZalihe() throws Exception {
        Artikl a = new Artikl("Povrat Neg", "PN001", 20.00, 25.0, 1);
        db().saveArtikl(a);
        PovratRobe p = new PovratRobe(a.getId(), a.getNaziv(), 5, 20.00,
            PovratRobe.TipPovrata.DOBAVLJACU, null); // traži 5, ima samo 1
        p.setVrijemePovrata(LocalDateTime.now());
        assertThrows(SQLException.class, () -> db().savePovrat(p));
        // Zaliha mora ostati nepromijenjena
        assertEquals(1, db().getArtikli().get(0).getKolicinaNaSkladistu());
    }

    @Test
    void getArtiklById_VracaIspravniArtikl() throws Exception {
        Artikl a = new Artikl("ById Test", "BID001", 15.00, 25.0, 7);
        db().saveArtikl(a);
        Artikl found = db().getArtiklById(a.getId());
        assertNotNull(found);
        assertEquals("ById Test", found.getNaziv());
        assertEquals(7, found.getKolicinaNaSkladistu());
    }

    @Test
    void getArtiklById_VracaNullZaNepostojeci() throws Exception {
        assertNull(db().getArtiklById(99999));
    }
}
