package stil.app.model;

import java.time.LocalDateTime;

/**
 * Model dnevne zaključnice blagajne.
 *
 * Sadrži agregirane podatke o prodaji za jedan radni dan:
 * broj računa, ukupni iznos, iznos po načinu plaćanja.
 * Sprema se u bazu i kao PDF na disk pri generiranju.
 */
public class Zaklucnica {

    private int id;
    private LocalDateTime vrijemeGeneriranja;
    private LocalDateTime danOd;   // početak radnog dana (00:00:00)
    private LocalDateTime danDo;   // kraj radnog dana (23:59:59)
    private int brojRacuna;
    private double ukupnoEur;
    private double gotovinaEur;
    private double karticaEur;
    private String putanjaPdf;     // apsolutna putanja do PDF datoteke

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getVrijemeGeneriranja() { return vrijemeGeneriranja; }
    public void setVrijemeGeneriranja(LocalDateTime v) { this.vrijemeGeneriranja = v; }

    public LocalDateTime getDanOd() { return danOd; }
    public void setDanOd(LocalDateTime d) { this.danOd = d; }

    public LocalDateTime getDanDo() { return danDo; }
    public void setDanDo(LocalDateTime d) { this.danDo = d; }

    public int getBrojRacuna() { return brojRacuna; }
    public void setBrojRacuna(int b) { this.brojRacuna = b; }

    public double getUkupnoEur() { return ukupnoEur; }
    public void setUkupnoEur(double u) { this.ukupnoEur = u; }

    public double getGotovinaEur() { return gotovinaEur; }
    public void setGotovinaEur(double g) { this.gotovinaEur = g; }

    public double getKarticaEur() { return karticaEur; }
    public void setKarticaEur(double k) { this.karticaEur = k; }

    public String getPutanjaPdf() { return putanjaPdf; }
    public void setPutanjaPdf(String p) { this.putanjaPdf = p; }
}
