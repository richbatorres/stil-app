package stil.app.model;

/**
 * Model koji predstavlja dobavljača robe.
 *
 * Dobavljač je tvrtka ili fizička osoba od koje trgovina nabavlja robu.
 * Pohranjen u SQLite tablici {@code dobavljac}.
 *
 * OIB je opcionalan jer strani dobavljači ne moraju imati hrvatski OIB.
 * Email i telefon su kontaktni podaci za narudžbe.
 */
public class Dobavljac {

    private int id;
    private String naziv;       // naziv tvrtke ili ime dobavljača
    private String oib;         // OIB (opcionalan, za domaće dobavljače)
    private String adresa;      // adresa sjedišta
    private String email;       // kontakt email
    private String telefon;     // kontakt telefon
    private String napomena;    // slobodna napomena (uvjeti plaćanja, rok isporuke, itd.)
    private boolean komisijskiModel; // nudi li dobavljač komisijsku prodaju (povrat neprodanog)

    public Dobavljac() {}

    /**
     * Konstruktor za kreiranje novog dobavljača.
     *
     * @param naziv   naziv tvrtke ili ime dobavljača
     * @param oib     OIB dobavljača, može biti null
     * @param adresa  adresa sjedišta, može biti null
     * @param email   kontakt email, može biti null
     * @param telefon kontakt telefon, može biti null
     */
    public Dobavljac(String naziv, String oib, String adresa, String email, String telefon) {
        this.naziv = naziv;
        this.oib = oib;
        this.adresa = adresa;
        this.email = email;
        this.telefon = telefon;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNaziv() { return naziv; }
    public void setNaziv(String naziv) { this.naziv = naziv; }

    public String getOib() { return oib; }
    public void setOib(String oib) { this.oib = oib; }

    public String getAdresa() { return adresa; }
    public void setAdresa(String adresa) { this.adresa = adresa; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = telefon; }

    public String getNapomena() { return napomena; }
    public void setNapomena(String napomena) { this.napomena = napomena; }

    public boolean isKomisijskiModel() { return komisijskiModel; }
    public void setKomisijskiModel(boolean komisijskiModel) { this.komisijskiModel = komisijskiModel; }

    /** Vraća naziv dobavljača — koristi se za prikaz u Swing komponentama (npr. JComboBox). */
    @Override
    public String toString() { return naziv; }
}
