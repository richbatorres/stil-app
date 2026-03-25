package stil.app.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model koji predstavlja jedan fiskalni račun.
 *
 * Pohranjen u SQLite tablici {@code racun}, s povezanim stavkama u {@code stavka_racuna}.
 *
 * Oznaka računa formata je "brojRacuna-oznakaPosProstora-oznakaUredaja", npr. "1-1-1".
 * ZKI (Zaštitni Kod Izdavatelja) generira se lokalno prije slanja Fini.
 * JIR (Jedinstveni Identifikator Računa) vraća Fina nakon uspješne fiskalizacije.
 *
 * Status životnog ciklusa:
 *   KREIRAN → račun je pohranjen lokalno, fiskalizacija još nije obavljena
 *   FISKALIZIRAN → JIR je dobiven od Fine, račun je legalno fiskaliziran
 *   STORNIRAN → račun je storniran (nije implementirano u UI, ali podržano u modelu)
 */
public class Racun {

    /** Način plaćanja — G (gotovina) ili K (kartica) u Fina XML-u. */
    public enum NacinPlacanja { GOTOVINA, KARTICA }

    /** Status fiskalizacije računa. */
    public enum Status { KREIRAN, FISKALIZIRAN, STORNIRAN }

    private int id;
    private int brojRacuna;
    private String oznakaRacuna;          // npr. "1-1-1" (broj-posProstor-uredaj)
    private LocalDateTime vrijemeIzdavanja;
    private NacinPlacanja nacinPlacanja;
    private Status status;
    private String zki;                   // Zaštitni Kod Izdavatelja (MD5 od RSA potpisa)
    private String jir;                   // Jedinstveni Identifikator Računa (od Fine)
    private List<StavkaRacuna> stavke = new ArrayList<>();

    /**
     * Izračunava ukupni iznos računa (suma svih stavki s popustima).
     *
     * @return ukupni iznos u EUR
     */
    public double getUkupno() {
        return stavke.stream().mapToDouble(StavkaRacuna::getUkupno).sum();
    }

    /**
     * Izračunava ukupni iznos PDV-a koristeći formulu za preračun iz bruto iznosa.
     * Formula: PDV = bruto × stopa / (100 + stopa)
     *
     * @return ukupni PDV iznos u EUR
     */
    public double getUkupnoPdv() {
        return stavke.stream()
            .mapToDouble(s -> s.getUkupno() * s.getPdvStopa() / (100 + s.getPdvStopa()))
            .sum();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBrojRacuna() { return brojRacuna; }
    public void setBrojRacuna(int brojRacuna) { this.brojRacuna = brojRacuna; }

    public String getOznakaRacuna() { return oznakaRacuna; }
    public void setOznakaRacuna(String oznakaRacuna) { this.oznakaRacuna = oznakaRacuna; }

    public LocalDateTime getVrijemeIzdavanja() { return vrijemeIzdavanja; }
    public void setVrijemeIzdavanja(LocalDateTime vrijemeIzdavanja) { this.vrijemeIzdavanja = vrijemeIzdavanja; }

    public NacinPlacanja getNacinPlacanja() { return nacinPlacanja; }
    public void setNacinPlacanja(NacinPlacanja nacinPlacanja) { this.nacinPlacanja = nacinPlacanja; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getZki() { return zki; }
    public void setZki(String zki) { this.zki = zki; }

    public String getJir() { return jir; }
    public void setJir(String jir) { this.jir = jir; }

    public List<StavkaRacuna> getStavke() { return stavke; }
    public void setStavke(List<StavkaRacuna> stavke) { this.stavke = stavke; }
}
