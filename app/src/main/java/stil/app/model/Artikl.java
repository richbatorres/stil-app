package stil.app.model;

/**
 * Model koji predstavlja jedan artikl u katalogu trgovine.
 *
 * Artikl se pohranjuje u SQLite tablici {@code artikl}.
 * Barkod je opcionalan i jedinstven — koristi se za brzo dodavanje u košaricu skeniranjem.
 * PDV stopa je pohranjena po artiklu radi fleksibilnosti, iako trenutno svi artikli koriste 25%.
 */
public class Artikl {
    private int id;
    private String naziv;
    private String barkod;
    private double cijena;        // maloprodajna cijena s PDV-om, u EUR
    private double pdvStopa;      // postotak, npr. 25.0
    private int kolicinaNaSkladistu;

    public Artikl() {}

    /**
     * Konstruktor za kreiranje novog artikla prije pohrane u bazu.
     *
     * @param naziv naziv artikla
     * @param barkod EAN/barkod artikla, može biti null
     * @param cijena maloprodajna cijena s PDV-om u EUR
     * @param pdvStopa PDV stopa u postotku (npr. 25.0)
     * @param kolicinaNaSkladistu početna količina na skladištu
     */
    public Artikl(String naziv, String barkod, double cijena, double pdvStopa, int kolicinaNaSkladistu) {
        this.naziv = naziv;
        this.barkod = barkod;
        this.cijena = cijena;
        this.pdvStopa = pdvStopa;
        this.kolicinaNaSkladistu = kolicinaNaSkladistu;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNaziv() { return naziv; }
    public void setNaziv(String naziv) { this.naziv = naziv; }

    public String getBarkod() { return barkod; }
    public void setBarkod(String barkod) { this.barkod = barkod; }

    public double getCijena() { return cijena; }
    public void setCijena(double cijena) { this.cijena = cijena; }

    public double getPdvStopa() { return pdvStopa; }
    public void setPdvStopa(double pdvStopa) { this.pdvStopa = pdvStopa; }

    public int getKolicinaNaSkladistu() { return kolicinaNaSkladistu; }
    public void setKolicinaNaSkladistu(int kolicinaNaSkladistu) { this.kolicinaNaSkladistu = kolicinaNaSkladistu; }

    /** Vraća naziv artikla — koristi se za prikaz u Swing komponentama (npr. JComboBox). */
    @Override
    public String toString() { return naziv; }
}
