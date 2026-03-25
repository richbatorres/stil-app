package stil.app.model;

/**
 * Model koji predstavlja jednu stavku (liniju) na računu.
 *
 * Pohranjena u SQLite tablici {@code stavka_racuna}.
 * Naziv artikla se denormalizira (kopira) u trenutku prodaje kako bi
 * povijesni računi ostali ispravni čak i ako se artikl naknadno preimenuje ili obriše.
 *
 * Ukupni iznos stavke izračunava se kao: cijena × količina × (1 - popust/100)
 */
public class StavkaRacuna {
    private int id;
    private int racunId;
    private int artiklId;
    private String artiklNaziv;   // denormalizirano — kopija naziva u trenutku prodaje
    private double cijena;        // cijena po komadu s PDV-om u trenutku prodaje
    private int kolicina;
    private double popust;        // popust u postotku, npr. 10.0 = 10%
    private double pdvStopa;      // PDV stopa u postotku, npr. 25.0

    public StavkaRacuna() {}

    /**
     * Konstruktor koji kopira relevantne podatke iz artikla u trenutku dodavanja u košaricu.
     *
     * @param artikl artikl koji se dodaje
     * @param kolicina količina komada
     * @param popust popust u postotku (0-100)
     */
    public StavkaRacuna(Artikl artikl, int kolicina, double popust) {
        this.artiklId = artikl.getId();
        this.artiklNaziv = artikl.getNaziv();
        this.cijena = artikl.getCijena();
        this.kolicina = kolicina;
        this.popust = popust;
        this.pdvStopa = artikl.getPdvStopa();
    }

    /**
     * Izračunava ukupni iznos stavke s uračunatim popustom.
     *
     * @return cijena × količina × (1 - popust/100)
     */
    public double getUkupno() {
        return cijena * kolicina * (1 - popust / 100);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRacunId() { return racunId; }
    public void setRacunId(int racunId) { this.racunId = racunId; }

    public int getArtiklId() { return artiklId; }
    public void setArtiklId(int artiklId) { this.artiklId = artiklId; }

    public String getArtiklNaziv() { return artiklNaziv; }
    public void setArtiklNaziv(String artiklNaziv) { this.artiklNaziv = artiklNaziv; }

    public double getCijena() { return cijena; }
    public void setCijena(double cijena) { this.cijena = cijena; }

    public int getKolicina() { return kolicina; }
    public void setKolicina(int kolicina) { this.kolicina = kolicina; }

    public double getPopust() { return popust; }
    public void setPopust(double popust) { this.popust = popust; }

    public double getPdvStopa() { return pdvStopa; }
    public void setPdvStopa(double pdvStopa) { this.pdvStopa = pdvStopa; }
}
