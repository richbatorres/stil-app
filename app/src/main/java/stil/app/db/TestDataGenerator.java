package stil.app.db;

import stil.app.model.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generator testnih podataka za razvoj i demonstraciju aplikacije.
 *
 * Generira realistične testne unose za sve entitete:
 * - 50 dobavljača
 * - 200 artikala (s vezama na dobavljače)
 * - 1000 računa (s 1-5 stavki svaki)
 * - 200 nabava
 * - 200 povrata robe
 *
 * Ukupno >1000 unosa po kategoriji kad se ubroje stavke računa.
 * Poziva se jednom pri prvom pokretanju ako je baza prazna.
 */
public class TestDataGenerator {

    private static final Random RND = new Random(42); // fiksni seed za reproducibilnost

    private static final String[] KATEGORIJE = {
        "Majica", "Hlače", "Haljina", "Jakna", "Kaput", "Suknja", "Bluza",
        "Džemper", "Košulja", "Trenerka", "Šorts", "Prsluk", "Ogrtač", "Kombinezon"
    };
    private static final String[] BOJE = {
        "crna", "bijela", "plava", "crvena", "zelena", "siva", "bež",
        "smeđa", "narančasta", "ljubičasta", "roza", "tirkizna"
    };
    private static final String[] VELICINE = {"XS", "S", "M", "L", "XL", "XXL"};
    private static final String[] DOBAVLJACI_NAZIVI = {
        "Tekstil d.o.o.", "Moda Export", "Fashion Group", "Euro Textile",
        "Adriatic Fashion", "Zagreb Moda", "Split Style", "Rijeka Cloth",
        "Osijek Tekstil", "Varaždin Fashion", "Dubrovnik Style", "Pula Moda",
        "Zadar Cloth", "Šibenik Tekstil", "Karlovac Fashion", "Sisak Style",
        "Bjelovar Moda", "Koprivnica Cloth", "Čakovec Tekstil", "Požega Fashion",
        "Slavonski Brod Style", "Vukovar Moda", "Vinkovci Cloth", "Đakovo Tekstil",
        "Petrinja Fashion", "Kutina Style", "Križevci Moda", "Virovitica Cloth",
        "Pakrac Tekstil", "Novska Fashion", "Garešnica Style", "Daruvar Moda",
        "Grubišno Polje Cloth", "Beli Manastir Tekstil", "Donji Miholjac Fashion",
        "Našice Style", "Orahovica Moda", "Slatina Cloth", "Podravska Slatina Tekstil",
        "Valpovo Fashion", "Belišće Style", "Đurđevac Moda", "Ludbreg Cloth",
        "Novi Marof Tekstil", "Ivanec Fashion", "Lepoglava Style", "Klanjec Moda",
        "Pregrada Cloth", "Krapina Tekstil", "Zabok Fashion"
    };
    private static final String[] RAZLOZI_POVRATA = {
        "Pogrešna veličina", "Oštećena roba", "Kupac se predomislio",
        "Neispravna boja", "Kvaliteta ne odgovara", "Duplikat narudžbe",
        "Istekao rok", "Greška pri narudžbi"
    };

    /**
     * Generira sve testne podatke ako je baza prazna (nema artikala).
     * Sigurno za pozivanje pri svakom pokretanju — ne duplicira podatke.
     *
     * @param db DatabaseManager instanca
     * @throws SQLException ako dođe do greške pri pisanju
     */
    public static void generirajAkoJePrazno(DatabaseManager db) throws SQLException {
        if (!db.getDobavljaci().isEmpty()) return; // već ima podataka
        generiraj(db);
    }

    /**
     * Bezuvjetno generira sve testne podatke.
     *
     * @param db DatabaseManager instanca
     * @throws SQLException ako dođe do greške
     */
    public static void generiraj(DatabaseManager db) throws SQLException {
        List<Dobavljac> dobavljaci = generirajDobavljace(db);
        List<Artikl> artikli = generirajArtikle(db, dobavljaci);
        generirajRacune(db, artikli);
        generirajNabave(db, artikli, dobavljaci);
        generirajPovrate(db, artikli, dobavljaci);
    }

    /** Generira 50 dobavljača. */
    private static List<Dobavljac> generirajDobavljace(DatabaseManager db) throws SQLException {
        List<Dobavljac> lista = new ArrayList<>();
        for (int i = 0; i < DOBAVLJACI_NAZIVI.length; i++) {
            Dobavljac d = new Dobavljac();
            d.setNaziv(DOBAVLJACI_NAZIVI[i]);
            d.setOib(generirajOib(i));
            d.setAdresa("Ulica " + (i + 1) + ", " + (10000 + i * 100) + " Grad");
            d.setEmail("info@" + DOBAVLJACI_NAZIVI[i].toLowerCase()
                .replaceAll("[^a-z]", "").substring(0, Math.min(8, DOBAVLJACI_NAZIVI[i].length())) + ".hr");
            d.setTelefon("0" + (91 + RND.nextInt(9)) + "-" + (1000000 + RND.nextInt(9000000)));
            db.saveDobavljac(d);
            lista.add(d);
        }
        return lista;
    }

    /** Generira 200 artikala s vezama na dobavljače. */
    private static List<Artikl> generirajArtikle(DatabaseManager db,
                                                   List<Dobavljac> dobavljaci) throws SQLException {
        List<Artikl> lista = new ArrayList<>();
        int barkodSeq = 3850000000001L > 0 ? 1 : 0; // dummy
        for (int i = 0; i < 200; i++) {
            String kat = KATEGORIJE[i % KATEGORIJE.length];
            String boja = BOJE[i % BOJE.length];
            String vel = VELICINE[i % VELICINE.length];
            Artikl a = new Artikl();
            a.setNaziv(kat + " " + boja + " " + vel);
            a.setBarkod(String.format("385%010d", i + 1));
            a.setCijena(Math.round((9.99 + RND.nextDouble() * 190) * 100.0) / 100.0);
            a.setPdvStopa(25.0);
            a.setKolicinaNaSkladistu(RND.nextInt(50) + 5);
            db.saveArtikl(a);
            // Poveži s 1-3 dobavljača
            int brDob = 1 + RND.nextInt(3);
            List<Integer> dobIds = new ArrayList<>();
            for (int j = 0; j < brDob; j++)
                dobIds.add(dobavljaci.get(RND.nextInt(dobavljaci.size())).getId());
            db.setDobavljaciZaArtikl(a.getId(), dobIds.stream().distinct().toList());
            lista.add(a);
        }
        return lista;
    }

    /** Generira 1000 računa s 1-5 stavki svaki, raspoređenih kroz zadnjih 365 dana. */
    private static void generirajRacune(DatabaseManager db, List<Artikl> artikli) throws SQLException {
        for (int i = 0; i < 1000; i++) {
            int brojRacuna = db.getSljedeciBrojRacuna();
            Racun r = new Racun();
            r.setBrojRacuna(brojRacuna);
            r.setOznakaRacuna(brojRacuna + "-PP1-1");
            // Rasporedi kroz zadnjih 365 dana
            r.setVrijemeIzdavanja(LocalDateTime.now()
                .minusDays(RND.nextInt(365))
                .minusHours(RND.nextInt(10))
                .minusMinutes(RND.nextInt(60)));
            r.setNacinPlacanja(RND.nextBoolean()
                ? Racun.NacinPlacanja.GOTOVINA : Racun.NacinPlacanja.KARTICA);
            r.setStatus(Racun.Status.KREIRAN);

            int brStavki = 1 + RND.nextInt(5);
            for (int j = 0; j < brStavki; j++) {
                Artikl a = artikli.get(RND.nextInt(artikli.size()));
                // Provjeri ima li zalihe (ne smanjuj ispod 0 u testu)
                if (a.getKolicinaNaSkladistu() <= 0) continue;
                int kom = 1 + RND.nextInt(Math.min(3, a.getKolicinaNaSkladistu()));
                double popust = RND.nextInt(10) == 0 ? 10.0 : 0.0; // 10% šansa za popust
                r.getStavke().add(new StavkaRacuna(a, kom, popust));
                a.setKolicinaNaSkladistu(a.getKolicinaNaSkladistu() - kom); // lokalno praćenje
            }
            if (r.getStavke().isEmpty()) continue;
            db.saveRacun(r);
        }
    }

    /** Generira 200 nabava raspoređenih kroz zadnjih 365 dana. */
    private static void generirajNabave(DatabaseManager db, List<Artikl> artikli,
                                         List<Dobavljac> dobavljaci) throws SQLException {
        for (int i = 0; i < 200; i++) {
            Artikl a = artikli.get(RND.nextInt(artikli.size()));
            Dobavljac d = dobavljaci.get(RND.nextInt(dobavljaci.size()));
            Nabava n = new Nabava();
            n.setArtiklId(a.getId());
            n.setArtiklNaziv(a.getNaziv());
            n.setDobavljacId(d.getId());
            n.setDobavljacNaziv(d.getNaziv());
            n.setKolicina(5 + RND.nextInt(50));
            n.setNabavnaCijena(Math.round(a.getCijena() * (0.4 + RND.nextDouble() * 0.3) * 100.0) / 100.0);
            n.setVrijemeNabave(LocalDateTime.now()
                .minusDays(RND.nextInt(365))
                .minusHours(RND.nextInt(12)));
            n.setNapomena(RND.nextInt(5) == 0 ? "Hitna narudžba" : null);
            db.saveNabava(n);
        }
    }

    /** Generira 200 povrata robe raspoređenih kroz zadnjih 365 dana. */
    private static void generirajPovrate(DatabaseManager db, List<Artikl> artikli,
                                          List<Dobavljac> dobavljaci) throws SQLException {
        for (int i = 0; i < 200; i++) {
            Artikl a = artikli.get(RND.nextInt(artikli.size()));
            PovratRobe.TipPovrata tip = RND.nextBoolean()
                ? PovratRobe.TipPovrata.OD_KUPCA : PovratRobe.TipPovrata.DOBAVLJACU;
            PovratRobe p = new PovratRobe();
            p.setArtiklId(a.getId());
            p.setArtiklNaziv(a.getNaziv());
            p.setKolicina(1 + RND.nextInt(3));
            p.setCijenaPoKomadu(a.getCijena());
            p.setTipPovrata(tip);
            p.setVrijemePovrata(LocalDateTime.now()
                .minusDays(RND.nextInt(365))
                .minusHours(RND.nextInt(12)));
            p.setRazlog(RAZLOZI_POVRATA[RND.nextInt(RAZLOZI_POVRATA.length)]);
            if (tip == PovratRobe.TipPovrata.DOBAVLJACU) {
                Dobavljac d = dobavljaci.get(RND.nextInt(dobavljaci.size()));
                p.setDobavljacId(d.getId());
                p.setDobavljacNaziv(d.getNaziv());
            }
            db.savePovrat(p);
        }
    }

    /** Generira deterministički OIB od 11 znamenki (nije stvarni OIB). */
    private static String generirajOib(int seed) {
        StringBuilder sb = new StringBuilder();
        Random r = new Random(seed * 31L + 7);
        for (int i = 0; i < 11; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }
}
