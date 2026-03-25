package stil.app.model;

import java.time.LocalDateTime;

/**
 * Model koji predstavlja jednu nabavu robe od dobavljača.
 *
 * Pohranjen u SQLite tablici {@code nabava}.
 * Svaka nabava bilježi ulaz određene količine artikla u skladište,
 * s nabavnom cijenom i vezom na dobavljača.
 *
 * Pri pohrani u bazu, zaliha artikla se automatski povećava za nabavljenu količinu
 * u istoj transakciji — gubitak konekcije ili pad aplikacije ne može unijeti
 * nekonzistentno stanje (ili su i nabava i zaliha ažurirane, ili nijedno).
 */
public class Nabava {

    private int id;
    private int artiklId;
    private String artiklNaziv;       // denormalizirano — kopija naziva u trenutku nabave
    private int dobavljacId;          // 0 ako dobavljač nije evidentiran
    private String dobavljacNaziv;    // denormalizirano — kopija naziva dobavljača
    private int kolicina;             // broj nabavljenih komada (uvijek > 0)
    private double nabavnaCijena;     // nabavna cijena po komadu (bez PDV-a)
    private LocalDateTime vrijemeNabave;
    private String napomena;

    public Nabava() {}

    /**
     * Konstruktor za kreiranje nove nabave.
     *
     * @param artiklId      ID artikla koji se nabavlja
     * @param artiklNaziv   naziv artikla (denormaliziran)
     * @param kolicina      broj komada (mora biti > 0)
     * @param nabavnaCijena nabavna cijena po komadu
     * @param napomena      opcionalna napomena (može biti null)
     */
    public Nabava(int artiklId, String artiklNaziv, int kolicina,
                  double nabavnaCijena, String napomena) {
        this.artiklId = artiklId;
        this.artiklNaziv = artiklNaziv;
        this.kolicina = kolicina;
        this.nabavnaCijena = nabavnaCijena;
        this.napomena = napomena;
        this.vrijemeNabave = LocalDateTime.now();
    }

    /**
     * Izračunava ukupni iznos nabave (kolicina × nabavna cijena po komadu).
     *
     * @return ukupni nabavni trošak u EUR
     */
    public double getUkupniTrosak() {
        return kolicina * nabavnaCijena;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getArtiklId() { return artiklId; }
    public void setArtiklId(int artiklId) { this.artiklId = artiklId; }

    public String getArtiklNaziv() { return artiklNaziv; }
    public void setArtiklNaziv(String artiklNaziv) { this.artiklNaziv = artiklNaziv; }

    public int getDobavljacId() { return dobavljacId; }
    public void setDobavljacId(int dobavljacId) { this.dobavljacId = dobavljacId; }

    public String getDobavljacNaziv() { return dobavljacNaziv; }
    public void setDobavljacNaziv(String dobavljacNaziv) { this.dobavljacNaziv = dobavljacNaziv; }

    public int getKolicina() { return kolicina; }
    public void setKolicina(int kolicina) { this.kolicina = kolicina; }

    public double getNabavnaCijena() { return nabavnaCijena; }
    public void setNabavnaCijena(double nabavnaCijena) { this.nabavnaCijena = nabavnaCijena; }

    public LocalDateTime getVrijemeNabave() { return vrijemeNabave; }
    public void setVrijemeNabave(LocalDateTime vrijemeNabave) { this.vrijemeNabave = vrijemeNabave; }

    public String getNapomena() { return napomena; }
    public void setNapomena(String napomena) { this.napomena = napomena; }
}
