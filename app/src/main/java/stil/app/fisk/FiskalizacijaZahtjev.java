package stil.app.fisk;

/**
 * DTO (Data Transfer Object) koji sadrži sve podatke potrebne za izgradnju
 * SOAP XML zahtjeva prema Fina fiskalizacijskom servisu.
 *
 * Iznosi su pohranjeni kao String u formatu "0.00" jer ih XML zahtijeva u tom obliku.
 * Datum/vrijeme je u formatu "dd.MM.yyyy'T'HH:mm:ss" prema Fina specifikaciji.
 * Način plaćanja je "G" za gotovinu ili "K" za karticu.
 */
public class FiskalizacijaZahtjev {
    private String oib;                  // OIB tvrtke, 11 znamenki
    private String datumVrijemePoruke;   // datum/vrijeme slanja poruke, format: dd.MM.yyyy'T'HH:mm:ss
    private String datumVrijemeRacuna;   // datum/vrijeme izdavanja računa, isti format
    private String brojRacuna;           // redni broj računa (samo numerički dio oznake)
    private String oznakaPosProstora;    // oznaka poslovnog prostora (npr. "PP1")
    private String oznakaUredaja;        // oznaka naplatnog uređaja (npr. "1")
    private String iznosUkupno;          // ukupni iznos s PDV-om, format "0.00"
    private String osnovica;             // PDV osnovica (iznos bez PDV-a), format "0.00"
    private String iznosPdv;             // iznos PDV-a, format "0.00"
    private String nacinPlacanja;        // "G" = gotovina, "K" = kartica
    private String zki;                  // prethodno izračunati ZKI

    public String getOib() { return oib; }
    public void setOib(String oib) { this.oib = oib; }

    public String getDatumVrijemePoruke() { return datumVrijemePoruke; }
    public void setDatumVrijemePoruke(String datumVrijemePoruke) { this.datumVrijemePoruke = datumVrijemePoruke; }

    public String getDatumVrijemeRacuna() { return datumVrijemeRacuna; }
    public void setDatumVrijemeRacuna(String datumVrijemeRacuna) { this.datumVrijemeRacuna = datumVrijemeRacuna; }

    public String getBrojRacuna() { return brojRacuna; }
    public void setBrojRacuna(String brojRacuna) { this.brojRacuna = brojRacuna; }

    public String getOznakaPosProstora() { return oznakaPosProstora; }
    public void setOznakaPosProstora(String oznakaPosProstora) { this.oznakaPosProstora = oznakaPosProstora; }

    public String getOznakaUredaja() { return oznakaUredaja; }
    public void setOznakaUredaja(String oznakaUredaja) { this.oznakaUredaja = oznakaUredaja; }

    public String getIznosUkupno() { return iznosUkupno; }
    public void setIznosUkupno(String iznosUkupno) { this.iznosUkupno = iznosUkupno; }

    public String getOsnovica() { return osnovica; }
    public void setOsnovica(String osnovica) { this.osnovica = osnovica; }

    public String getIznosPdv() { return iznosPdv; }
    public void setIznosPdv(String iznosPdv) { this.iznosPdv = iznosPdv; }

    public String getNacinPlacanja() { return nacinPlacanja; }
    public void setNacinPlacanja(String nacinPlacanja) { this.nacinPlacanja = nacinPlacanja; }

    public String getZki() { return zki; }
    public void setZki(String zki) { this.zki = zki; }
}
