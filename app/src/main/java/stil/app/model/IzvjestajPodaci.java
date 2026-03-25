package stil.app.model;

/**
 * DTO (Data Transfer Object) koji sadrži agregirane podatke za izvještaj
 * o poslovanju u zadanom vremenskom razdoblju.
 *
 * Koristi se za prikaz u IzvjestajiPanel i za generiranje PDF/tekstualnih izvještaja.
 * Svi iznosi su u EUR, s PDV-om (bruto).
 */
public class IzvjestajPodaci {

    private String naslov;
    private String razdobljeOd;
    private String razdobljeDo;

    // Prodaja
    private int brojRacuna;
    private double ukupnaProdaja;       // bruto prihod od prodaje
    private double ukupniPdvProdaja;    // PDV sadržan u prodaji
    private double prodajaGotovina;
    private double prodajaKartica;

    // Nabava
    private int brojNabava;
    private double ukupnaNabava;        // ukupni nabavni troškovi

    // Povrati
    private int brojPovratOdKupca;
    private double iznosPovratOdKupca;
    private int brojPovratDobavljacu;
    private double iznosPovratDobavljacu;

    // Marža = prodaja - nabava (aproksimacija, bez PDV-a na prodaji)
    /**
     * Izračunava bruto maržu: prihod od prodaje bez PDV-a minus nabavni troškovi.
     *
     * @return bruto marža u EUR
     */
    public double getBrutoMarza() {
        return (ukupnaProdaja - ukupniPdvProdaja) - ukupnaNabava;
    }

    /**
     * Izračunava neto promet: prodaja minus povrati od kupca.
     *
     * @return neto promet u EUR
     */
    public double getNetoPromet() {
        return ukupnaProdaja - iznosPovratOdKupca;
    }

    public String getNaslov() { return naslov; }
    public void setNaslov(String naslov) { this.naslov = naslov; }

    public String getRazdobljeDo() { return razdobljeDo; }
    public void setRazdobljeDo(String razdobljeDo) { this.razdobljeDo = razdobljeDo; }

    public String getRazdobljeOd() { return razdobljeOd; }
    public void setRazdobljeOd(String razdobljeOd) { this.razdobljeOd = razdobljeOd; }

    public int getBrojRacuna() { return brojRacuna; }
    public void setBrojRacuna(int brojRacuna) { this.brojRacuna = brojRacuna; }

    public double getUkupnaProdaja() { return ukupnaProdaja; }
    public void setUkupnaProdaja(double ukupnaProdaja) { this.ukupnaProdaja = ukupnaProdaja; }

    public double getUkupniPdvProdaja() { return ukupniPdvProdaja; }
    public void setUkupniPdvProdaja(double ukupniPdvProdaja) { this.ukupniPdvProdaja = ukupniPdvProdaja; }

    public double getProdajaGotovina() { return prodajaGotovina; }
    public void setProdajaGotovina(double prodajaGotovina) { this.prodajaGotovina = prodajaGotovina; }

    public double getProdajaKartica() { return prodajaKartica; }
    public void setProdajaKartica(double prodajaKartica) { this.prodajaKartica = prodajaKartica; }

    public int getBrojNabava() { return brojNabava; }
    public void setBrojNabava(int brojNabava) { this.brojNabava = brojNabava; }

    public double getUkupnaNabava() { return ukupnaNabava; }
    public void setUkupnaNabava(double ukupnaNabava) { this.ukupnaNabava = ukupnaNabava; }

    public int getBrojPovratOdKupca() { return brojPovratOdKupca; }
    public void setBrojPovratOdKupca(int brojPovratOdKupca) { this.brojPovratOdKupca = brojPovratOdKupca; }

    public double getIznosPovratOdKupca() { return iznosPovratOdKupca; }
    public void setIznosPovratOdKupca(double iznosPovratOdKupca) { this.iznosPovratOdKupca = iznosPovratOdKupca; }

    public int getBrojPovratDobavljacu() { return brojPovratDobavljacu; }
    public void setBrojPovratDobavljacu(int brojPovratDobavljacu) { this.brojPovratDobavljacu = brojPovratDobavljacu; }

    public double getIznosPovratDobavljacu() { return iznosPovratDobavljacu; }
    public void setIznosPovratDobavljacu(double iznosPovratDobavljacu) { this.iznosPovratDobavljacu = iznosPovratDobavljacu; }
}
