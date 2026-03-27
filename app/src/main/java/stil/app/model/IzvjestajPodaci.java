package stil.app.model;

/**
 * DTO s agregiranim podacima izvještaja o poslovanju u zadanom vremenskom razdoblju.
 * Svi iznosi su u EUR, s PDV-om (bruto).
 */
public class IzvjestajPodaci {

    private int brojRacuna;
    private double ukupnaProdaja;
    private double ukupniPdvProdaja;
    private double prodajaGotovina;
    private double prodajaKartica;
    private int brojNabava;
    private double ukupnaNabava;
    private int brojPovratOdKupca;
    private double iznosPovratOdKupca;
    private int brojPovratDobavljacu;
    private double iznosPovratDobavljacu;
    // Pohrana razdoblja za PDF izvoz
    private String razdobljeOd;
    private String razdobljeDo;

    public double getBrutoMarza() { return (ukupnaProdaja - ukupniPdvProdaja) - ukupnaNabava; }
    public double getNetoPromet() { return ukupnaProdaja - iznosPovratOdKupca; }

    public String getRazdobljeOd() { return razdobljeOd; }
    public void setRazdobljeOd(String v) { this.razdobljeOd = v; }
    public String getRazdobljeDo() { return razdobljeDo; }
    public void setRazdobljeDo(String v) { this.razdobljeDo = v; }
    public int getBrojRacuna() { return brojRacuna; }
    public void setBrojRacuna(int v) { this.brojRacuna = v; }
    public double getUkupnaProdaja() { return ukupnaProdaja; }
    public void setUkupnaProdaja(double v) { this.ukupnaProdaja = v; }
    public double getUkupniPdvProdaja() { return ukupniPdvProdaja; }
    public void setUkupniPdvProdaja(double v) { this.ukupniPdvProdaja = v; }
    public double getProdajaGotovina() { return prodajaGotovina; }
    public void setProdajaGotovina(double v) { this.prodajaGotovina = v; }
    public double getProdajaKartica() { return prodajaKartica; }
    public void setProdajaKartica(double v) { this.prodajaKartica = v; }
    public int getBrojNabava() { return brojNabava; }
    public void setBrojNabava(int v) { this.brojNabava = v; }
    public double getUkupnaNabava() { return ukupnaNabava; }
    public void setUkupnaNabava(double v) { this.ukupnaNabava = v; }
    public int getBrojPovratOdKupca() { return brojPovratOdKupca; }
    public void setBrojPovratOdKupca(int v) { this.brojPovratOdKupca = v; }
    public double getIznosPovratOdKupca() { return iznosPovratOdKupca; }
    public void setIznosPovratOdKupca(double v) { this.iznosPovratOdKupca = v; }
    public int getBrojPovratDobavljacu() { return brojPovratDobavljacu; }
    public void setBrojPovratDobavljacu(int v) { this.brojPovratDobavljacu = v; }
    public double getIznosPovratDobavljacu() { return iznosPovratDobavljacu; }
    public void setIznosPovratDobavljacu(double v) { this.iznosPovratDobavljacu = v; }
}
