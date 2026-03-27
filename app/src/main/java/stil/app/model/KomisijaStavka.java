package stil.app.model;

/**
 * Jedna stavka komisijskog obračuna za određenog dobavljača i mjesec.
 *
 * Sadrži podatke o tome koliko je komada nekog artikla nabavljeno u komisiju,
 * koliko je prodano i koliko je ostalo za povrat dobavljaču.
 * Izračunava se iz nabava i prodaja u zadanom mjesecu.
 */
public class KomisijaStavka {

    private int artiklId;
    private String artiklNaziv;
    private int nabavljeno;      // ukupno primljeno u komisiju u tom mjesecu
    private int prodano;         // prodano u tom mjesecu
    private int ostalo;          // nabavljeno - prodano (za povrat)
    private double nabavnaCijena; // cijena po komadu (iz nabave)

    public KomisijaStavka() {}

    /**
     * @param artiklId      ID artikla
     * @param artiklNaziv   naziv artikla
     * @param nabavljeno    količina primljena u komisiju
     * @param prodano       količina prodana u periodu
     * @param nabavnaCijena nabavna cijena po komadu
     */
    public KomisijaStavka(int artiklId, String artiklNaziv,
                          int nabavljeno, int prodano, double nabavnaCijena) {
        this.artiklId = artiklId;
        this.artiklNaziv = artiklNaziv;
        this.nabavljeno = nabavljeno;
        this.prodano = prodano;
        this.ostalo = nabavljeno - prodano;
        this.nabavnaCijena = nabavnaCijena;
    }

    public int getArtiklId() { return artiklId; }
    public void setArtiklId(int artiklId) { this.artiklId = artiklId; }

    public String getArtiklNaziv() { return artiklNaziv; }
    public void setArtiklNaziv(String artiklNaziv) { this.artiklNaziv = artiklNaziv; }

    public int getNabavljeno() { return nabavljeno; }
    public void setNabavljeno(int nabavljeno) {
        this.nabavljeno = nabavljeno;
        this.ostalo = nabavljeno - prodano;
    }

    public int getProdano() { return prodano; }
    public void setProdano(int prodano) {
        this.prodano = prodano;
        this.ostalo = nabavljeno - prodano;
    }

    public int getOstalo() { return ostalo; }

    public double getNabavnaCijena() { return nabavnaCijena; }
    public void setNabavnaCijena(double nabavnaCijena) { this.nabavnaCijena = nabavnaCijena; }

    /** Ukupna vrijednost prodanih komada po nabavnoj cijeni. */
    public double getVrijednostProdano() { return prodano * nabavnaCijena; }

    /** Ukupna vrijednost neprodanih komada za povrat dobavljaču. */
    public double getVrijednostPovrat() { return ostalo * nabavnaCijena; }
}
