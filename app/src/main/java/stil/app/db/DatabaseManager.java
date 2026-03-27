package stil.app.db;

import stil.app.model.Artikl;
import stil.app.model.Config;
import stil.app.model.Dobavljac;
import stil.app.model.IzvjestajPodaci;
import stil.app.model.KomisijaStavka;
import stil.app.model.Nabava;
import stil.app.model.PovratRobe;
import stil.app.model.Racun;
import stil.app.model.StavkaRacuna;
import stil.app.model.Zaklucnica;
import stil.app.util.CryptoUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton klasa koja upravlja svim operacijama s SQLite bazom podataka.
 *
 * Baza se kreira kao {@code stil.db} u radnom direktoriju aplikacije.
 * Schema se automatski inicijalizira pri prvom pokretanju putem {@code CREATE TABLE IF NOT EXISTS}.
 *
 * Tablice:
 * - {@code config}        — key-value konfiguracijske postavke
 * - {@code artikl}        — katalog artikala s cijenama i zalihama
 * - {@code racun}         — zaglavlja računa s fiskalizacijskim podacima
 * - {@code stavka_racuna} — stavke (linije) pojedinih računa
 * - {@code dobavljac}     — katalog dobavljača robe
 * - {@code povrat_robe}   — evidencija povrata robe (od kupca ili dobavljaču)
 * - {@code nabava}        — evidencija nabave robe od dobavljača
 * - {@code artikl_dobavljac} — many-to-many veza artikla i dobavljača
 *
 * Sve operacije pisanja koje mijenjaju više tablica izvršavaju se u transakciji
 * kako bi se osigurala konzistentnost podataka.
 */
public class DatabaseManager {

    static String DB_URL = "jdbc:sqlite:stil.db";
    private static DatabaseManager instance;
    private Connection connection;
    // Keširani prepared statementi za česte upite
    private PreparedStatement psGetArtikli;
    private PreparedStatement psGetArtiklById;
    private PreparedStatement psUpdateKolicina;
    private PreparedStatement psGetSljedeciBroj;

    private DatabaseManager() throws SQLException {
        connection = DriverManager.getConnection(DB_URL);
        connection.createStatement().execute("PRAGMA foreign_keys = ON");
        initSchema();
    }

    /**
     * Vraća singleton instancu. Kreira je pri prvom pozivu.
     *
     * @return jedina instanca DatabaseManagera
     * @throws SQLException ako se ne može uspostaviti veza s bazom
     */
    public static DatabaseManager getInstance() throws SQLException {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    /**
     * Kreira sve tablice ako ne postoje.
     * Sigurno za pozivanje pri svakom pokretanju — ne briše postojeće podatke.
     */
    private void initSchema() throws SQLException {
        Statement st = connection.createStatement();
        st.execute("""
            CREATE TABLE IF NOT EXISTS config (
                kljuc TEXT PRIMARY KEY,
                vrijednost TEXT
            )""");
        st.execute("""
            CREATE TABLE IF NOT EXISTS artikl (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                naziv TEXT NOT NULL,
                barkod TEXT UNIQUE,
                cijena REAL NOT NULL,
                pdv_stopa REAL NOT NULL DEFAULT 25.0,
                kolicina_na_skladistu INTEGER NOT NULL DEFAULT 0
            )""");
        st.execute("""
            CREATE TABLE IF NOT EXISTS racun (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                broj_racuna INTEGER NOT NULL,
                oznaka_racuna TEXT NOT NULL,
                vrijeme_izdavanja TEXT NOT NULL,
                nacin_placanja TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'KREIRAN',
                zki TEXT,
                jir TEXT
            )""");
        st.execute("""
            CREATE TABLE IF NOT EXISTS stavka_racuna (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                racun_id INTEGER NOT NULL REFERENCES racun(id),
                artikl_id INTEGER REFERENCES artikl(id),
                artikl_naziv TEXT NOT NULL,
                cijena REAL NOT NULL,
                kolicina INTEGER NOT NULL,
                popust REAL NOT NULL DEFAULT 0.0,
                pdv_stopa REAL NOT NULL
            )""");
        st.execute("""
            CREATE TABLE IF NOT EXISTS dobavljac (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                naziv TEXT NOT NULL,
                oib TEXT,
                adresa TEXT,
                email TEXT,
                telefon TEXT,
                napomena TEXT
            )""");
        st.execute("""
            CREATE TABLE IF NOT EXISTS povrat_robe (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                artikl_id INTEGER REFERENCES artikl(id),
                artikl_naziv TEXT NOT NULL,
                dobavljac_id INTEGER REFERENCES dobavljac(id),
                dobavljac_naziv TEXT,
                kolicina INTEGER NOT NULL,
                cijena_po_komadu REAL NOT NULL,
                tip_povrata TEXT NOT NULL,
                vrijeme_povrata TEXT NOT NULL,
                razlog TEXT
            )""");
        st.execute("""
            CREATE TABLE IF NOT EXISTS nabava (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                artikl_id INTEGER REFERENCES artikl(id),
                artikl_naziv TEXT NOT NULL,
                dobavljac_id INTEGER REFERENCES dobavljac(id),
                dobavljac_naziv TEXT,
                kolicina INTEGER NOT NULL,
                nabavna_cijena REAL NOT NULL,
                vrijeme_nabave TEXT NOT NULL,
                napomena TEXT
            )""");
        st.execute("""
            CREATE TABLE IF NOT EXISTS artikl_dobavljac (
                artikl_id INTEGER NOT NULL REFERENCES artikl(id) ON DELETE CASCADE,
                dobavljac_id INTEGER NOT NULL REFERENCES dobavljac(id) ON DELETE CASCADE,
                PRIMARY KEY (artikl_id, dobavljac_id)
            )""");
        st.execute("""
            CREATE TABLE IF NOT EXISTS komisija_obracun (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                dobavljac_id INTEGER NOT NULL REFERENCES dobavljac(id),
                dobavljac_naziv TEXT NOT NULL,
                godina INTEGER NOT NULL,
                mjesec INTEGER NOT NULL,
                artikl_id INTEGER NOT NULL REFERENCES artikl(id),
                artikl_naziv TEXT NOT NULL,
                nabavljeno INTEGER NOT NULL,
                prodano INTEGER NOT NULL,
                ostalo INTEGER NOT NULL,
                nabavna_cijena REAL NOT NULL,
                UNIQUE(dobavljac_id, godina, mjesec, artikl_id)
            )""");
        st.execute("""
            CREATE TABLE IF NOT EXISTS zaklucnica (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                vrijeme_generiranja TEXT NOT NULL,
                dan_od TEXT NOT NULL,
                dan_do TEXT NOT NULL,
                broj_racuna INTEGER NOT NULL,
                ukupno_eur REAL NOT NULL,
                gotovina_eur REAL NOT NULL,
                kartica_eur REAL NOT NULL,
                putanja_pdf TEXT
            )""");
        // Dodaj komisijski_model kolonu ako ne postoji (migracija za postojece baze)
        try {
            st.execute("ALTER TABLE dobavljac ADD COLUMN komisijski_model INTEGER NOT NULL DEFAULT 0");
        } catch (SQLException ignored) {} // kolona vec postoji
    }

    // -------------------------------------------------------------------------
    // CONFIG
    // -------------------------------------------------------------------------

    /**
     * Učitava sve konfiguracijske postavke iz baze u Config objekt.
     * Nepostojući ključevi ostavljaju polja na null/default vrijednostima.
     *
     * @return Config objekt s učitanim postavkama
     * @throws SQLException ako dođe do greške pri čitanju
     */
    public Config loadConfig() throws SQLException {
        Config config = new Config();
        ResultSet rs = connection.createStatement().executeQuery("SELECT kljuc, vrijednost FROM config");
        while (rs.next()) {
            String v = rs.getString("vrijednost");
            switch (rs.getString("kljuc")) {
                case "naziv_tvrtke"        -> config.setNazivTvrtke(v);
                case "oib"                 -> config.setOib(v);
                case "adresa"              -> config.setAdresa(v);
                case "oznaka_pos_prostora" -> config.setOznakaPosProstora(v);
                case "oznaka_uredaja"      -> config.setOznakaUredaja(v);
                case "putanja_certifikata" -> config.setPutanjaCertifikata(v);
                case "naziv_printera"      -> config.setNazivPrintera(v);
                // Osjetljivi podaci — dekriptiraju se pri čitanju
                case "lozinka_certifikata" -> config.setLozinkaCertifikata(CryptoUtil.decrypt(v));
                case "pin"                 -> config.setPin(CryptoUtil.decrypt(v));
                case "fisk_test_mode"      -> config.setFiskTestMode("true".equals(v));
            }
        }
        return config;
    }

    /**
     * Sprema sve postavke iz Config objekta koristeći UPSERT (INSERT OR UPDATE).
     *
     * @param config Config objekt s postavkama za pohraniti
     * @throws SQLException ako dođe do greške pri pisanju
     */
    public void saveConfig(Config config) throws SQLException {
        saveConfigValue("naziv_tvrtke", config.getNazivTvrtke());
        saveConfigValue("oib", config.getOib());
        saveConfigValue("adresa", config.getAdresa());
        saveConfigValue("oznaka_pos_prostora", config.getOznakaPosProstora());
        saveConfigValue("oznaka_uredaja", config.getOznakaUredaja());
        saveConfigValue("putanja_certifikata", config.getPutanjaCertifikata());
        saveConfigValue("naziv_printera", config.getNazivPrintera());
        // Osjetljivi podaci — enkriptiraju se pri pohrani
        saveConfigValue("lozinka_certifikata", CryptoUtil.encrypt(config.getLozinkaCertifikata()));
        saveConfigValue("pin", CryptoUtil.encrypt(config.getPin()));
        saveConfigValue("fisk_test_mode", String.valueOf(config.isFiskTestMode()));
    }

    /**
     * UPSERT jednog config key-value para.
     *
     * @param kljuc     ključ postavke
     * @param vrijednost vrijednost postavke (može biti null)
     * @throws SQLException ako dođe do greške
     */
    private void saveConfigValue(String kljuc, String vrijednost) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO config(kljuc, vrijednost) VALUES(?,?) ON CONFLICT(kljuc) DO UPDATE SET vrijednost=excluded.vrijednost");
        ps.setString(1, kljuc);
        ps.setString(2, vrijednost);
        ps.executeUpdate();
    }

    // -------------------------------------------------------------------------
    // ARTIKL
    // -------------------------------------------------------------------------

    /**
     * Vraća sve artikle sortirane po nazivu.
     *
     * @return lista svih artikala
     * @throws SQLException ako dođe do greške pri čitanju
     */
    public List<Artikl> getArtikli() throws SQLException {
        if (psGetArtikli == null || psGetArtikli.isClosed())
            psGetArtikli = connection.prepareStatement("SELECT * FROM artikl ORDER BY naziv");
        List<Artikl> lista = new ArrayList<>();
        ResultSet rs = psGetArtikli.executeQuery();
        while (rs.next()) lista.add(mapArtikl(rs));
        return lista;
    }

    /**
     * Traži artikl po barkodu. Koristi se pri skeniranju barkoda u prodajnom ekranu.
     *
     * @param barkod EAN barkod artikla
     * @return pronađeni artikl ili null ako ne postoji
     * @throws SQLException ako dođe do greške pri čitanju
     */
    public Artikl getArtiklByBarkod(String barkod) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT * FROM artikl WHERE barkod = ?");
        ps.setString(1, barkod);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? mapArtikl(rs) : null;
    }

    /**
     * Traži artikl po ID-u. Koristi se za provjeru svježe zalihe prije naplate.
     *
     * @param id ID artikla
     * @return pronađeni artikl ili null ako ne postoji
     * @throws SQLException ako dođe do greške
     */
    public Artikl getArtiklById(int id) throws SQLException {
        if (psGetArtiklById == null || psGetArtiklById.isClosed())
            psGetArtiklById = connection.prepareStatement("SELECT * FROM artikl WHERE id = ?");
        psGetArtiklById.setInt(1, id);
        ResultSet rs = psGetArtiklById.executeQuery();
        return rs.next() ? mapArtikl(rs) : null;
    }

    /**
     * Sprema artikl — INSERT ako je id == 0, UPDATE ako id > 0.
     * Nakon INSERT-a postavlja generirani id na objekt.
     *
     * @param a artikl za pohraniti
     * @throws SQLException ako dođe do greške (npr. dupli barkod)
     */
    public void saveArtikl(Artikl a) throws SQLException {
        if (a.getId() == 0) {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO artikl(naziv, barkod, cijena, pdv_stopa, kolicina_na_skladistu) VALUES(?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, a.getNaziv());
            ps.setString(2, a.getBarkod());
            ps.setDouble(3, a.getCijena());
            ps.setDouble(4, a.getPdvStopa());
            ps.setInt(5, a.getKolicinaNaSkladistu());
            ps.executeUpdate();
            a.setId((int) ps.getGeneratedKeys().getLong(1));
        } else {
            PreparedStatement ps = connection.prepareStatement(
                "UPDATE artikl SET naziv=?, barkod=?, cijena=?, pdv_stopa=?, kolicina_na_skladistu=? WHERE id=?");
            ps.setString(1, a.getNaziv());
            ps.setString(2, a.getBarkod());
            ps.setDouble(3, a.getCijena());
            ps.setDouble(4, a.getPdvStopa());
            ps.setInt(5, a.getKolicinaNaSkladistu());
            ps.setInt(6, a.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Briše artikl po ID-u.
     * Ne briše povijesne stavke računa ni povrate (artikl_naziv je denormaliziran).
     *
     * @param id ID artikla za brisanje
     * @throws SQLException ako dođe do greške
     */
    public void deleteArtikl(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM artikl WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    /**
     * Mijenja količinu na skladištu za zadanu vrijednost.
     * Ako bi promjena rezultirala negativnom zalihom, baca SQLException.
     * Ovo sprječava prodaju ili povrat dobavljaču više robe nego što postoji na skladištu.
     *
     * @param artiklId ID artikla
     * @param promjena pozitivna za primku/povrat od kupca, negativna za prodaju/povrat dobavljaču
     * @throws SQLException ako bi zaliha postala negativna, ili ako dođe do DB greške
     */
    public void updateKolicina(int artiklId, int promjena) throws SQLException {
        if (promjena < 0) {
            if (psGetArtiklById == null || psGetArtiklById.isClosed())
                psGetArtiklById = connection.prepareStatement("SELECT kolicina_na_skladistu FROM artikl WHERE id=?");
            psGetArtiklById.setInt(1, artiklId);
            ResultSet rs = psGetArtiklById.executeQuery();
            if (rs.next()) {
                int trenutna = rs.getInt(1);
                if (trenutna + promjena < 0)
                    throw new SQLException("Nedovoljna zaliha: na skladištu je " + trenutna +
                        " kom, a traži se " + Math.abs(promjena) + " kom.");
            }
        }
        if (psUpdateKolicina == null || psUpdateKolicina.isClosed())
            psUpdateKolicina = connection.prepareStatement(
                "UPDATE artikl SET kolicina_na_skladistu = kolicina_na_skladistu + ? WHERE id=?");
        psUpdateKolicina.setInt(1, promjena);
        psUpdateKolicina.setInt(2, artiklId);
        psUpdateKolicina.executeUpdate();
    }

    // -------------------------------------------------------------------------
    // RACUN
    // -------------------------------------------------------------------------

    /**
     * Vraća sljedeći redni broj računa (MAX + 1).
     * Koristi se pri kreiranju novog računa.
     *
     * @return sljedeći redni broj
     * @throws SQLException ako dođe do greške
     */
    public int getSljedeciBrojRacuna() throws SQLException {
        if (psGetSljedeciBroj == null || psGetSljedeciBroj.isClosed())
            psGetSljedeciBroj = connection.prepareStatement(
                "SELECT COALESCE(MAX(broj_racuna), 0) + 1 FROM racun");
        ResultSet rs = psGetSljedeciBroj.executeQuery();
        return rs.getInt(1);
    }

    /**
     * Sprema račun i sve njegove stavke u jednoj transakciji.
     * Istovremeno smanjuje zalihe za svaku prodanu stavku.
     * U slučaju greške, cijela transakcija se poništava (rollback).
     *
     * @param r račun s popunjenim stavkama
     * @throws SQLException ako dođe do greške — zalihe i račun ostaju nepromijenjeni
     */
    public void saveRacun(Racun r) throws SQLException {
        connection.setAutoCommit(false);
        try {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO racun(broj_racuna, oznaka_racuna, vrijeme_izdavanja, nacin_placanja, status, zki, jir) VALUES(?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, r.getBrojRacuna());
            ps.setString(2, r.getOznakaRacuna());
            ps.setString(3, r.getVrijemeIzdavanja().toString());
            ps.setString(4, r.getNacinPlacanja().name());
            ps.setString(5, r.getStatus().name());
            ps.setString(6, r.getZki());
            ps.setString(7, r.getJir());
            ps.executeUpdate();
            r.setId((int) ps.getGeneratedKeys().getLong(1));

            for (StavkaRacuna s : r.getStavke()) {
                PreparedStatement ps2 = connection.prepareStatement(
                    "INSERT INTO stavka_racuna(racun_id, artikl_id, artikl_naziv, cijena, kolicina, popust, pdv_stopa) VALUES(?,?,?,?,?,?,?)");
                ps2.setInt(1, r.getId());
                ps2.setInt(2, s.getArtiklId());
                ps2.setString(3, s.getArtiklNaziv());
                ps2.setDouble(4, s.getCijena());
                ps2.setInt(5, s.getKolicina());
                ps2.setDouble(6, s.getPopust());
                ps2.setDouble(7, s.getPdvStopa());
                ps2.executeUpdate();
                updateKolicina(s.getArtiklId(), -s.getKolicina());
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Ažurira ZKI, JIR i status računa na FISKALIZIRAN nakon uspješnog odgovora od Fine.
     *
     * @param racunId ID računa u bazi
     * @param zki     Zaštitni Kod Izdavatelja
     * @param jir     Jedinstveni Identifikator Računa od Fine
     * @throws SQLException ako dođe do greške
     */
    public void updateRacunFiskalizacija(int racunId, String zki, String jir) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "UPDATE racun SET zki=?, jir=?, status='FISKALIZIRAN' WHERE id=?");
        ps.setString(1, zki);
        ps.setString(2, jir);
        ps.setInt(3, racunId);
        ps.executeUpdate();
    }

    /**
     * Stornira račun postavljanjem statusa na STORNIRAN.
     * Stornirani računi se ne ubrajaju u izvještaje prodaje.
     * Ne vraća zalihe — storniranje je samo evidencijsko (za povrat robe koristiti savePovrat).
     *
     * @param racunId ID računa za storniranje
     * @throws SQLException ako dođe do greške
     */
    public void stornirajRacun(int racunId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "UPDATE racun SET status='STORNIRAN' WHERE id=?");
        ps.setInt(1, racunId);
        ps.executeUpdate();
    }

    // -------------------------------------------------------------------------
    // ARTIKL — DOBAVLJAČ VEZA
    // -------------------------------------------------------------------------

    /**
     * Vraća listu dobavljača vezanih uz određeni artikl.
     *
     * @param artiklId ID artikla
     * @return lista dobavljača koji isporučuju taj artikl
     * @throws SQLException ako dođe do greške
     */
    public List<Dobavljac> getDobavljaciZaArtikl(int artiklId) throws SQLException {
        List<Dobavljac> lista = new ArrayList<>();
        PreparedStatement ps = connection.prepareStatement(
            "SELECT d.* FROM dobavljac d JOIN artikl_dobavljac ad ON d.id = ad.dobavljac_id " +
            "WHERE ad.artikl_id = ? ORDER BY d.naziv");
        ps.setInt(1, artiklId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) lista.add(mapDobavljac(rs));
        return lista;
    }

    /**
     * Postavlja listu dobavljača za artikl (zamjenjuje postojeće veze).
     * Operacija je atomarna — ili se sve veze postave ili nijedna.
     *
     * @param artiklId     ID artikla
     * @param dobavljacIds lista ID-ova dobavljača (može biti prazna za brisanje svih veza)
     * @throws SQLException ako dođe do greške
     */
    public void setDobavljaciZaArtikl(int artiklId, List<Integer> dobavljacIds) throws SQLException {
        connection.setAutoCommit(false);
        try {
            PreparedStatement del = connection.prepareStatement(
                "DELETE FROM artikl_dobavljac WHERE artikl_id = ?");
            del.setInt(1, artiklId);
            del.executeUpdate();
            PreparedStatement ins = connection.prepareStatement(
                "INSERT INTO artikl_dobavljac(artikl_id, dobavljac_id) VALUES(?,?)");
            for (int dobId : dobavljacIds) {
                ins.setInt(1, artiklId);
                ins.setInt(2, dobId);
                ins.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    // -------------------------------------------------------------------------
    // DOBAVLJAC
    // -------------------------------------------------------------------------

    /**
     * Vraća sve dobavljače sortirane po nazivu.
     *
     * @return lista svih dobavljača
     * @throws SQLException ako dođe do greške pri čitanju
     */
    public List<Dobavljac> getDobavljaci() throws SQLException {
        List<Dobavljac> lista = new ArrayList<>();
        ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM dobavljac ORDER BY naziv");
        while (rs.next()) lista.add(mapDobavljac(rs));
        return lista;
    }

    /**
     * Sprema dobavljača — INSERT ako je id == 0, UPDATE ako id > 0.
     * Nakon INSERT-a postavlja generirani id na objekt.
     *
     * @param d dobavljač za pohraniti
     * @throws SQLException ako dođe do greške
     */
    public void saveDobavljac(Dobavljac d) throws SQLException {
        if (d.getId() == 0) {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO dobavljac(naziv, oib, adresa, email, telefon, napomena, komisijski_model) VALUES(?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, d.getNaziv());
            ps.setString(2, d.getOib());
            ps.setString(3, d.getAdresa());
            ps.setString(4, d.getEmail());
            ps.setString(5, d.getTelefon());
            ps.setString(6, d.getNapomena());
            ps.setInt(7, d.isKomisijskiModel() ? 1 : 0);
            ps.executeUpdate();
            d.setId((int) ps.getGeneratedKeys().getLong(1));
        } else {
            PreparedStatement ps = connection.prepareStatement(
                "UPDATE dobavljac SET naziv=?, oib=?, adresa=?, email=?, telefon=?, napomena=?, komisijski_model=? WHERE id=?");
            ps.setString(1, d.getNaziv());
            ps.setString(2, d.getOib());
            ps.setString(3, d.getAdresa());
            ps.setString(4, d.getEmail());
            ps.setString(5, d.getTelefon());
            ps.setString(6, d.getNapomena());
            ps.setInt(7, d.isKomisijskiModel() ? 1 : 0);
            ps.setInt(8, d.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Briše dobavljača po ID-u.
     * Ne briše povijesne povrate robe (dobavljac_naziv je denormaliziran).
     *
     * @param id ID dobavljača za brisanje
     * @throws SQLException ako dođe do greške
     */
    public void deleteDobavljac(int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM dobavljac WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // -------------------------------------------------------------------------
    // POVRAT ROBE
    // -------------------------------------------------------------------------

    /**
     * Vraća sve povrate robe sortirane od najnovijeg prema najstarijem.
     *
     * @return lista svih povrata robe
     * @throws SQLException ako dođe do greške pri čitanju
     */
    public List<PovratRobe> getPovrati() throws SQLException {
        List<PovratRobe> lista = new ArrayList<>();
        ResultSet rs = connection.createStatement().executeQuery(
            "SELECT * FROM povrat_robe ORDER BY vrijeme_povrata DESC");
        while (rs.next()) lista.add(mapPovrat(rs));
        return lista;
    }

    /**
     * Vraća povrate robe za određeni artikl.
     *
     * @param artiklId ID artikla
     * @return lista povrata za taj artikl
     * @throws SQLException ako dođe do greške
     */
    public List<PovratRobe> getPovratByArtikl(int artiklId) throws SQLException {
        List<PovratRobe> lista = new ArrayList<>();
        PreparedStatement ps = connection.prepareStatement(
            "SELECT * FROM povrat_robe WHERE artikl_id = ? ORDER BY vrijeme_povrata DESC");
        ps.setInt(1, artiklId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) lista.add(mapPovrat(rs));
        return lista;
    }

    /**
     * Sprema povrat robe i ažurira zalihu artikla u jednoj transakciji.
     * Smjer promjene zalihe ovisi o tipu povrata:
     * - OD_KUPCA: zaliha se povećava za kolicinu
     * - DOBAVLJACU: zaliha se smanjuje za kolicinu
     *
     * Nakon INSERT-a postavlja generirani id na objekt.
     *
     * @param p povrat robe za pohraniti
     * @throws SQLException ako dođe do greške — zaliha ostaje nepromijenjena
     */
    public void savePovrat(PovratRobe p) throws SQLException {
        connection.setAutoCommit(false);
        try {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO povrat_robe(artikl_id, artikl_naziv, dobavljac_id, dobavljac_naziv, " +
                "kolicina, cijena_po_komadu, tip_povrata, vrijeme_povrata, razlog) VALUES(?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, p.getArtiklId());
            ps.setString(2, p.getArtiklNaziv());
            if (p.getDobavljacId() > 0) ps.setInt(3, p.getDobavljacId());
            else ps.setNull(3, Types.INTEGER);
            ps.setString(4, p.getDobavljacNaziv());
            ps.setInt(5, p.getKolicina());
            ps.setDouble(6, p.getCijenaPoKomadu());
            ps.setString(7, p.getTipPovrata().name());
            ps.setString(8, p.getVrijemePovrata().toString());
            ps.setString(9, p.getRazlog());
            ps.executeUpdate();
            p.setId((int) ps.getGeneratedKeys().getLong(1));

            // Ažuriraj zalihu prema tipu povrata
            updateKolicina(p.getArtiklId(), p.getPromjenaZalihe());
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    // -------------------------------------------------------------------------
    // NABAVA
    // -------------------------------------------------------------------------

    /**
     * Vraća sve nabave sortirane od najnovije prema najstarijoj.
     *
     * @return lista svih nabava
     * @throws SQLException ako dođe do greške pri čitanju
     */
    public List<Nabava> getNabave() throws SQLException {
        List<Nabava> lista = new ArrayList<>();
        ResultSet rs = connection.createStatement().executeQuery(
            "SELECT * FROM nabava ORDER BY vrijeme_nabave DESC");
        while (rs.next()) lista.add(mapNabava(rs));
        return lista;
    }

    /**
     * Vraća nabave u zadanom vremenskom rasponu (uključivo granice).
     *
     * @param od  početak raspona (ISO format, npr. "2024-01-01T00:00:00")
     * @param do_ kraj raspona (ISO format, npr. "2024-01-31T23:59:59")
     * @return lista nabava u rasponu
     * @throws SQLException ako dođe do greške
     */
    public List<Nabava> getNabaveURasponu(String od, String do_) throws SQLException {
        List<Nabava> lista = new ArrayList<>();
        PreparedStatement ps = connection.prepareStatement(
            "SELECT * FROM nabava WHERE vrijeme_nabave >= ? AND vrijeme_nabave <= ? ORDER BY vrijeme_nabave DESC");
        ps.setString(1, od);
        ps.setString(2, do_);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) lista.add(mapNabava(rs));
        return lista;
    }

    /**
     * Sprema nabavu i povećava zalihu artikla u jednoj transakciji.
     * U slučaju greške, cijela transakcija se poništava — zaliha ostaje nepromijenjena.
     * Nakon INSERT-a postavlja generirani id na objekt.
     *
     * @param n nabava za pohraniti
     * @throws SQLException ako dođe do greške
     */
    public void saveNabava(Nabava n) throws SQLException {
        connection.setAutoCommit(false);
        try {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO nabava(artikl_id, artikl_naziv, dobavljac_id, dobavljac_naziv, " +
                "kolicina, nabavna_cijena, vrijeme_nabave, napomena) VALUES(?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, n.getArtiklId());
            ps.setString(2, n.getArtiklNaziv());
            if (n.getDobavljacId() > 0) ps.setInt(3, n.getDobavljacId());
            else ps.setNull(3, Types.INTEGER);
            ps.setString(4, n.getDobavljacNaziv());
            ps.setInt(5, n.getKolicina());
            ps.setDouble(6, n.getNabavnaCijena());
            ps.setString(7, n.getVrijemeNabave().toString());
            ps.setString(8, n.getNapomena());
            ps.executeUpdate();
            n.setId((int) ps.getGeneratedKeys().getLong(1));
            // Nabava povećava zalihu
            updateKolicina(n.getArtiklId(), n.getKolicina());
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    // -------------------------------------------------------------------------
    // IZVJEŠTAJI
    // -------------------------------------------------------------------------

    /**
     * Generira agregirane podatke za izvještaj u zadanom vremenskom rasponu.
     * Sve vrijednosti se računaju direktno u SQL-u radi točnosti i performansi.
     * Raspon je uključiv na obje granice.
     *
     * @param od  početak raspona (ISO format LocalDateTime, npr. "2024-01-01T00:00:00")
     * @param do_ kraj raspona (ISO format LocalDateTime, npr. "2024-01-31T23:59:59")
     * @return IzvjestajPodaci s agregiranim vrijednostima
     * @throws SQLException ako dođe do greške
     */
    public IzvjestajPodaci generirajIzvjestaj(String od, String do_) throws SQLException {
        IzvjestajPodaci p = new IzvjestajPodaci();
        p.setRazdobljeOd(od);
        p.setRazdobljeDo(do_);

        // Prodaja — agregiraj po načinu plaćanja
        PreparedStatement ps = connection.prepareStatement("""
            SELECT
                COUNT(DISTINCT r.id) AS br_racuna,
                COALESCE(SUM(s.cijena * s.kolicina * (1 - s.popust/100.0)), 0) AS ukupno,
                COALESCE(SUM(s.cijena * s.kolicina * (1 - s.popust/100.0) * s.pdv_stopa / (100 + s.pdv_stopa)), 0) AS ukupno_pdv,
                COALESCE(SUM(CASE WHEN r.nacin_placanja='GOTOVINA' THEN s.cijena * s.kolicina * (1 - s.popust/100.0) ELSE 0 END), 0) AS gotovina,
                COALESCE(SUM(CASE WHEN r.nacin_placanja='KARTICA'  THEN s.cijena * s.kolicina * (1 - s.popust/100.0) ELSE 0 END), 0) AS kartica
            FROM racun r
            JOIN stavka_racuna s ON s.racun_id = r.id
            WHERE r.status != 'STORNIRAN'
              AND r.vrijeme_izdavanja >= ? AND r.vrijeme_izdavanja <= ?
            """);
        ps.setString(1, od);
        ps.setString(2, do_);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            p.setBrojRacuna(rs.getInt("br_racuna"));
            p.setUkupnaProdaja(rs.getDouble("ukupno"));
            p.setUkupniPdvProdaja(rs.getDouble("ukupno_pdv"));
            p.setProdajaGotovina(rs.getDouble("gotovina"));
            p.setProdajaKartica(rs.getDouble("kartica"));
        }

        // Nabava
        PreparedStatement ps2 = connection.prepareStatement("""
            SELECT COUNT(*) AS br, COALESCE(SUM(kolicina * nabavna_cijena), 0) AS ukupno
            FROM nabava WHERE vrijeme_nabave >= ? AND vrijeme_nabave <= ?
            """);
        ps2.setString(1, od);
        ps2.setString(2, do_);
        ResultSet rs2 = ps2.executeQuery();
        if (rs2.next()) {
            p.setBrojNabava(rs2.getInt("br"));
            p.setUkupnaNabava(rs2.getDouble("ukupno"));
        }

        // Povrati
        PreparedStatement ps3 = connection.prepareStatement("""
            SELECT tip_povrata,
                   COUNT(*) AS br,
                   COALESCE(SUM(kolicina * cijena_po_komadu), 0) AS iznos
            FROM povrat_robe
            WHERE vrijeme_povrata >= ? AND vrijeme_povrata <= ?
            GROUP BY tip_povrata
            """);
        ps3.setString(1, od);
        ps3.setString(2, do_);
        ResultSet rs3 = ps3.executeQuery();
        while (rs3.next()) {
            String tip = rs3.getString("tip_povrata");
            int br = rs3.getInt("br");
            double iznos = rs3.getDouble("iznos");
            if ("OD_KUPCA".equals(tip)) {
                p.setBrojPovratOdKupca(br);
                p.setIznosPovratOdKupca(iznos);
            } else {
                p.setBrojPovratDobavljacu(br);
                p.setIznosPovratDobavljacu(iznos);
            }
        }

        return p;
    }

    /**
     * Vraća listu računa s ukupnim iznosima u zadanom vremenskom rasponu.
     * Koristi se za prikaz detalja prometa u IzvjestajiPanel.
     *
     * @param od  početak raspona (ISO format)
     * @param do_ kraj raspona (ISO format)
     * @return lista računa (bez stavki — samo zaglavlja s ukupnim iznosom)
     * @throws SQLException ako dođe do greške
     */
    public List<Racun> getRacuniURasponu(String od, String do_) throws SQLException {
        List<Racun> lista = new ArrayList<>();
        PreparedStatement ps = connection.prepareStatement("""
            SELECT r.*, COALESCE(SUM(s.cijena * s.kolicina * (1 - s.popust/100.0)), 0) AS ukupno_iznos
            FROM racun r
            LEFT JOIN stavka_racuna s ON s.racun_id = r.id
            WHERE r.vrijeme_izdavanja >= ? AND r.vrijeme_izdavanja <= ?
            GROUP BY r.id
            ORDER BY r.vrijeme_izdavanja DESC
            """);
        ps.setString(1, od);
        ps.setString(2, do_);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Racun r = new Racun();
            r.setId(rs.getInt("id"));
            r.setBrojRacuna(rs.getInt("broj_racuna"));
            r.setOznakaRacuna(rs.getString("oznaka_racuna"));
            r.setVrijemeIzdavanja(LocalDateTime.parse(rs.getString("vrijeme_izdavanja")));
            r.setNacinPlacanja(Racun.NacinPlacanja.valueOf(rs.getString("nacin_placanja")));
            r.setStatus(Racun.Status.valueOf(rs.getString("status")));
            r.setZki(rs.getString("zki"));
            r.setJir(rs.getString("jir"));
            // Dodaj sintetičku stavku samo za prikaz ukupnog iznosa
            StavkaRacuna s = new StavkaRacuna();
            s.setCijena(rs.getDouble("ukupno_iznos"));
            s.setKolicina(1);
            r.getStavke().add(s);
            lista.add(r);
        }
        return lista;
    }

    // -------------------------------------------------------------------------
    // KOMISIJA
    // -------------------------------------------------------------------------

    /**
     * Vraća dobavljače koji nude komisijski model prodaje.
     *
     * @return lista komisijskih dobavljača sortirana po nazivu
     * @throws SQLException ako dođe do greške
     */
    public List<Dobavljac> getKomisijskiDobavljaci() throws SQLException {
        List<Dobavljac> lista = new ArrayList<>();
        ResultSet rs = connection.createStatement().executeQuery(
            "SELECT * FROM dobavljac WHERE komisijski_model = 1 ORDER BY naziv");
        while (rs.next()) lista.add(mapDobavljac(rs));
        return lista;
    }

    /**
     * Generira komisijski obračun za zadanog dobavljača i mjesec.
     * Za svaki artikl koji je nabavljen od tog dobavljača u zadanom mjesecu
     * izračunava koliko je prodano (iz stavki računa) i koliko je ostalo.
     *
     * Prodano = sve stavke računa za taj artikl u tom mjesecu (neovisno o dobavljaču).
     * Ostalo = nabavljeno - prodano (ne može biti negativno).
     *
     * @param dobavljacId ID dobavljača
     * @param godina      godina obračuna
     * @param mjesec      mjesec obračuna (1-12)
     * @return lista stavki obračuna, jedna po artiklu
     * @throws SQLException ako dođe do greške
     */
    public List<KomisijaStavka> generirajKomisijaObracun(int dobavljacId, int godina, int mjesec)
            throws SQLException {
        String od = String.format("%04d-%02d-01T00:00:00", godina, mjesec);
        // Zadnji dan u mjesecu
        java.time.YearMonth ym = java.time.YearMonth.of(godina, mjesec);
        String do_ = String.format("%04d-%02d-%02dT23:59:59", godina, mjesec, ym.lengthOfMonth());

        // Nabave od tog dobavljača u tom mjesecu, grupirane po artiklu
        PreparedStatement psNab = connection.prepareStatement("""
            SELECT artikl_id, artikl_naziv,
                   SUM(kolicina) AS ukupno_nabavljeno,
                   AVG(nabavna_cijena) AS avg_cijena
            FROM nabava
            WHERE dobavljac_id = ?
              AND vrijeme_nabave >= ? AND vrijeme_nabave <= ?
            GROUP BY artikl_id, artikl_naziv
            """);
        psNab.setInt(1, dobavljacId);
        psNab.setString(2, od);
        psNab.setString(3, do_);
        ResultSet rsNab = psNab.executeQuery();

        List<KomisijaStavka> stavke = new ArrayList<>();
        while (rsNab.next()) {
            int artiklId = rsNab.getInt("artikl_id");
            String artiklNaziv = rsNab.getString("artikl_naziv");
            int nabavljeno = rsNab.getInt("ukupno_nabavljeno");
            double nabavnaCijena = rsNab.getDouble("avg_cijena");

            // Prodano u tom mjesecu za taj artikl (iz svih računa, ne storniranih)
            PreparedStatement psProd = connection.prepareStatement("""
                SELECT COALESCE(SUM(s.kolicina), 0) AS prodano
                FROM stavka_racuna s
                JOIN racun r ON r.id = s.racun_id
                WHERE s.artikl_id = ?
                  AND r.status != 'STORNIRAN'
                  AND r.vrijeme_izdavanja >= ? AND r.vrijeme_izdavanja <= ?
                """);
            psProd.setInt(1, artiklId);
            psProd.setString(2, od);
            psProd.setString(3, do_);
            ResultSet rsProd = psProd.executeQuery();
            int prodano = rsProd.next() ? rsProd.getInt("prodano") : 0;
            // Prodano ne može biti veće od nabavljenog u komisiju
            prodano = Math.min(prodano, nabavljeno);

            stavke.add(new KomisijaStavka(artiklId, artiklNaziv, nabavljeno, prodano, nabavnaCijena));
        }
        return stavke;
    }

    /**
     * Sprema generirani komisijski obračun u bazu (UPSERT po dobavljač+godina+mjesec+artikl).
     * Postojeći obračun za isti period se zamjenjuje.
     *
     * @param dobavljac dobavljač za kojeg se sprema obračun
     * @param godina    godina obračuna
     * @param mjesec    mjesec obračuna (1-12)
     * @param stavke    lista stavki obračuna
     * @throws SQLException ako dođe do greške
     */
    public void spremiKomisijaObracun(Dobavljac dobavljac, int godina, int mjesec,
                                      List<KomisijaStavka> stavke) throws SQLException {
        connection.setAutoCommit(false);
        try {
            // Obrisi stari obracun za isti period
            PreparedStatement del = connection.prepareStatement(
                "DELETE FROM komisija_obracun WHERE dobavljac_id=? AND godina=? AND mjesec=?");
            del.setInt(1, dobavljac.getId());
            del.setInt(2, godina);
            del.setInt(3, mjesec);
            del.executeUpdate();

            PreparedStatement ins = connection.prepareStatement("""
                INSERT INTO komisija_obracun
                    (dobavljac_id, dobavljac_naziv, godina, mjesec,
                     artikl_id, artikl_naziv, nabavljeno, prodano, ostalo, nabavna_cijena)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """);
            for (KomisijaStavka s : stavke) {
                ins.setInt(1, dobavljac.getId());
                ins.setString(2, dobavljac.getNaziv());
                ins.setInt(3, godina);
                ins.setInt(4, mjesec);
                ins.setInt(5, s.getArtiklId());
                ins.setString(6, s.getArtiklNaziv());
                ins.setInt(7, s.getNabavljeno());
                ins.setInt(8, s.getProdano());
                ins.setInt(9, s.getOstalo());
                ins.setDouble(10, s.getNabavnaCijena());
                ins.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Vraća sve snimljene komisijske obračune za zadanog dobavljača, sortirane od najnovijeg.
     *
     * @param dobavljacId ID dobavljača
     * @return lista stavki svih obračuna
     * @throws SQLException ako dođe do greške
     */
    public List<KomisijaStavka> getSpremiKomisijaObracun(int dobavljacId, int godina, int mjesec)
            throws SQLException {
        List<KomisijaStavka> lista = new ArrayList<>();
        PreparedStatement ps = connection.prepareStatement("""
            SELECT * FROM komisija_obracun
            WHERE dobavljac_id=? AND godina=? AND mjesec=?
            ORDER BY artikl_naziv
            """);
        ps.setInt(1, dobavljacId);
        ps.setInt(2, godina);
        ps.setInt(3, mjesec);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            KomisijaStavka s = new KomisijaStavka();
            s.setArtiklId(rs.getInt("artikl_id"));
            s.setArtiklNaziv(rs.getString("artikl_naziv"));
            s.setNabavljeno(rs.getInt("nabavljeno"));
            s.setProdano(rs.getInt("prodano"));
            s.setNabavnaCijena(rs.getDouble("nabavna_cijena"));
            lista.add(s);
        }
        return lista;
    }

    // -------------------------------------------------------------------------
    // ZAKLUCNICA
    // -------------------------------------------------------------------------

    /**
     * Generira podatke za zaključnicu za zadani dan iz baze.
     *
     * @param dan datum radnog dana
     * @return Zaklucnica s agregiranim podacima
     * @throws SQLException ako dođe do greške
     */
    public Zaklucnica generirajZaklucnicu(java.time.LocalDate dan) throws SQLException {
        String od = dan.atStartOfDay().toString();
        String do_ = dan.atTime(23, 59, 59).toString();

        PreparedStatement ps = connection.prepareStatement("""
            SELECT
                COUNT(DISTINCT r.id) AS br_racuna,
                COALESCE(SUM(s.cijena * s.kolicina * (1 - s.popust/100.0)), 0) AS ukupno,
                COALESCE(SUM(CASE WHEN r.nacin_placanja='GOTOVINA' THEN s.cijena * s.kolicina * (1 - s.popust/100.0) ELSE 0 END), 0) AS gotovina,
                COALESCE(SUM(CASE WHEN r.nacin_placanja='KARTICA'  THEN s.cijena * s.kolicina * (1 - s.popust/100.0) ELSE 0 END), 0) AS kartica
            FROM racun r
            JOIN stavka_racuna s ON s.racun_id = r.id
            WHERE r.status != 'STORNIRAN'
              AND r.vrijeme_izdavanja >= ? AND r.vrijeme_izdavanja <= ?
            """);
        ps.setString(1, od);
        ps.setString(2, do_);
        ResultSet rs = ps.executeQuery();

        Zaklucnica z = new Zaklucnica();
        z.setVrijemeGeneriranja(LocalDateTime.now());
        z.setDanOd(dan.atStartOfDay());
        z.setDanDo(dan.atTime(23, 59, 59));
        if (rs.next()) {
            z.setBrojRacuna(rs.getInt("br_racuna"));
            z.setUkupnoEur(rs.getDouble("ukupno"));
            z.setGotovinaEur(rs.getDouble("gotovina"));
            z.setKarticaEur(rs.getDouble("kartica"));
        }
        return z;
    }

    /**
     * Sprema zaključnicu u bazu. Postavlja generirani id na objekt.
     *
     * @param z zaključnica za pohraniti
     * @throws SQLException ako dođe do greške
     */
    public void saveZaklucnica(Zaklucnica z) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO zaklucnica(vrijeme_generiranja, dan_od, dan_do, broj_racuna, " +
            "ukupno_eur, gotovina_eur, kartica_eur, putanja_pdf) VALUES(?,?,?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, z.getVrijemeGeneriranja().toString());
        ps.setString(2, z.getDanOd().toString());
        ps.setString(3, z.getDanDo().toString());
        ps.setInt(4, z.getBrojRacuna());
        ps.setDouble(5, z.getUkupnoEur());
        ps.setDouble(6, z.getGotovinaEur());
        ps.setDouble(7, z.getKarticaEur());
        ps.setString(8, z.getPutanjaPdf());
        ps.executeUpdate();
        z.setId((int) ps.getGeneratedKeys().getLong(1));
    }

    // -------------------------------------------------------------------------
    // MAPPING HELPERS
    // -------------------------------------------------------------------------

    /**
     * Mapira jedan redak ResultSeta u Artikl objekt.
     *
     * @param rs ResultSet pozicioniran na redak
     * @return Artikl objekt s podacima iz retka
     * @throws SQLException ako dođe do greške pri čitanju
     */
    private Artikl mapArtikl(ResultSet rs) throws SQLException {
        Artikl a = new Artikl();
        a.setId(rs.getInt("id"));
        a.setNaziv(rs.getString("naziv"));
        a.setBarkod(rs.getString("barkod"));
        a.setCijena(rs.getDouble("cijena"));
        a.setPdvStopa(rs.getDouble("pdv_stopa"));
        a.setKolicinaNaSkladistu(rs.getInt("kolicina_na_skladistu"));
        return a;
    }

    /**
     * Mapira jedan redak ResultSeta u Dobavljac objekt.
     *
     * @param rs ResultSet pozicioniran na redak
     * @return Dobavljac objekt s podacima iz retka
     * @throws SQLException ako dođe do greške pri čitanju
     */
    private Dobavljac mapDobavljac(ResultSet rs) throws SQLException {
        Dobavljac d = new Dobavljac();
        d.setId(rs.getInt("id"));
        d.setNaziv(rs.getString("naziv"));
        d.setOib(rs.getString("oib"));
        d.setAdresa(rs.getString("adresa"));
        d.setEmail(rs.getString("email"));
        d.setTelefon(rs.getString("telefon"));
        d.setNapomena(rs.getString("napomena"));
        d.setKomisijskiModel(rs.getInt("komisijski_model") == 1);
        return d;
    }

    /**
     * Mapira jedan redak ResultSeta u Nabava objekt.
     *
     * @param rs ResultSet pozicioniran na redak
     * @return Nabava objekt s podacima iz retka
     * @throws SQLException ako dođe do greške pri čitanju
     */
    private Nabava mapNabava(ResultSet rs) throws SQLException {
        Nabava n = new Nabava();
        n.setId(rs.getInt("id"));
        n.setArtiklId(rs.getInt("artikl_id"));
        n.setArtiklNaziv(rs.getString("artikl_naziv"));
        n.setDobavljacId(rs.getInt("dobavljac_id"));
        n.setDobavljacNaziv(rs.getString("dobavljac_naziv"));
        n.setKolicina(rs.getInt("kolicina"));
        n.setNabavnaCijena(rs.getDouble("nabavna_cijena"));
        n.setVrijemeNabave(LocalDateTime.parse(rs.getString("vrijeme_nabave")));
        n.setNapomena(rs.getString("napomena"));
        return n;
    }

    /**
     * Mapira jedan redak ResultSeta u PovratRobe objekt.
     *
     * @param rs ResultSet pozicioniran na redak
     * @return PovratRobe objekt s podacima iz retka
     * @throws SQLException ako dođe do greške pri čitanju
     */
    private PovratRobe mapPovrat(ResultSet rs) throws SQLException {
        PovratRobe p = new PovratRobe();
        p.setId(rs.getInt("id"));
        p.setArtiklId(rs.getInt("artikl_id"));
        p.setArtiklNaziv(rs.getString("artikl_naziv"));
        p.setDobavljacId(rs.getInt("dobavljac_id"));
        p.setDobavljacNaziv(rs.getString("dobavljac_naziv"));
        p.setKolicina(rs.getInt("kolicina"));
        p.setCijenaPoKomadu(rs.getDouble("cijena_po_komadu"));
        p.setTipPovrata(PovratRobe.TipPovrata.valueOf(rs.getString("tip_povrata")));
        p.setVrijemePovrata(LocalDateTime.parse(rs.getString("vrijeme_povrata")));
        p.setRazlog(rs.getString("razlog"));
        return p;
    }
}
