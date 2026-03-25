package stil.app.model;

import java.time.LocalDateTime;

/**
 * Model koji predstavlja povrat robe dobavljaču ili od kupca.
 *
 * Povrat robe pohranjen je u SQLite tablici {@code povrat_robe}.
 * Svaki povrat vezan je uz određeni artikl i opcionalno uz dobavljača.
 *
 * Tip povrata određuje smjer kretanja robe:
 *   OD_KUPCA  — kupac vraća robu u trgovinu (povećava zalihu)
 *   DOBAVLJACU — trgovina vraća robu dobavljaču (smanjuje zalihu)
 *
 * Povrat automatski ažurira zalihu artikla pri pohrani u bazu
 * (pozitivna promjena za OD_KUPCA, negativna za DOBAVLJACU).
 */
public class PovratRobe {

    /**
     * Tip povrata robe — određuje smjer kretanja zalihe.
     * OD_KUPCA povećava zalihu, DOBAVLJACU smanjuje zalihu.
     */
    public enum TipPovrata { OD_KUPCA, DOBAVLJACU }

    private int id;
    private int artiklId;
    private String artiklNaziv;     // denormalizirano — kopija naziva u trenutku povrata
    private int dobavljacId;        // 0 ako povrat nije vezan uz dobavljača (povrat od kupca)
    private String dobavljacNaziv;  // denormalizirano — kopija naziva dobavljača
    private int kolicina;           // broj komada koji se vraća (uvijek pozitivan)
    private double cijenaPoKomadu;  // nabavna/prodajna cijena po komadu u trenutku povrata
    private TipPovrata tipPovrata;
    private LocalDateTime vrijemePovrata;
    private String razlog;          // razlog povrata (npr. "oštećena roba", "pogrešna veličina")

    public PovratRobe() {}

    /**
     * Konstruktor za kreiranje novog povrata robe.
     *
     * @param artiklId       ID artikla koji se vraća
     * @param artiklNaziv    naziv artikla (denormaliziran)
     * @param kolicina       broj komada (mora biti > 0)
     * @param cijenaPoKomadu cijena po komadu u trenutku povrata
     * @param tipPovrata     tip povrata (OD_KUPCA ili DOBAVLJACU)
     * @param razlog         razlog povrata, može biti null
     */
    public PovratRobe(int artiklId, String artiklNaziv, int kolicina,
                      double cijenaPoKomadu, TipPovrata tipPovrata, String razlog) {
        this.artiklId = artiklId;
        this.artiklNaziv = artiklNaziv;
        this.kolicina = kolicina;
        this.cijenaPoKomadu = cijenaPoKomadu;
        this.tipPovrata = tipPovrata;
        this.razlog = razlog;
        this.vrijemePovrata = LocalDateTime.now();
    }

    /**
     * Izračunava ukupni iznos povrata (kolicina × cijena po komadu).
     *
     * @return ukupni iznos povrata u EUR
     */
    public double getUkupniIznos() {
        return kolicina * cijenaPoKomadu;
    }

    /**
     * Vraća promjenu zalihe koju ovaj povrat uzrokuje.
     * OD_KUPCA → pozitivna promjena (roba ulazi u skladište)
     * DOBAVLJACU → negativna promjena (roba izlazi iz skladišta)
     *
     * @return promjena zalihe (pozitivna ili negativna)
     */
    public int getPromjenaZalihe() {
        return tipPovrata == TipPovrata.OD_KUPCA ? kolicina : -kolicina;
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

    public double getCijenaPoKomadu() { return cijenaPoKomadu; }
    public void setCijenaPoKomadu(double cijenaPoKomadu) { this.cijenaPoKomadu = cijenaPoKomadu; }

    public TipPovrata getTipPovrata() { return tipPovrata; }
    public void setTipPovrata(TipPovrata tipPovrata) { this.tipPovrata = tipPovrata; }

    public LocalDateTime getVrijemePovrata() { return vrijemePovrata; }
    public void setVrijemePovrata(LocalDateTime vrijemePovrata) { this.vrijemePovrata = vrijemePovrata; }

    public String getRazlog() { return razlog; }
    public void setRazlog(String razlog) { this.razlog = razlog; }
}
