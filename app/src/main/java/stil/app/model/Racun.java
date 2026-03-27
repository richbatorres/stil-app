package stil.app.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model koji predstavlja jedan fiskalni račun.
 *
 * Pohranjen u SQLite tablici {@code racun}, s povezanim stavkama u {@code stavka_racuna}.
 * Oznaka računa formata je "brojRacuna-oznakaPosProstora-oznakaUredaja", npr. "1-1-1".
 * ZKI generira se lokalno prije slanja Fini.
 * JIR vraća Fina nakon uspješne fiskalizacije.
 */
public class Racun {

    public enum NacinPlacanja { GOTOVINA, KARTICA }
    public enum Status { KREIRAN, FISKALIZIRAN, STORNIRAN }

    private int id;
    private int brojRacuna;
    private String oznakaRacuna;
    private LocalDateTime vrijemeIzdavanja;
    private NacinPlacanja nacinPlacanja;
    private Status status;
    private String zki;
    private String jir;
    private List<StavkaRacuna> stavke = new ArrayList<>();

    public double getUkupno() {
        return stavke.stream().mapToDouble(StavkaRacuna::getUkupno).sum();
    }

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
    public void setVrijemeIzdavanja(LocalDateTime v) { this.vrijemeIzdavanja = v; }
    public NacinPlacanja getNacinPlacanja() { return nacinPlacanja; }
    public void setNacinPlacanja(NacinPlacanja n) { this.nacinPlacanja = n; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getZki() { return zki; }
    public void setZki(String zki) { this.zki = zki; }
    public String getJir() { return jir; }
    public void setJir(String jir) { this.jir = jir; }
    public List<StavkaRacuna> getStavke() { return stavke; }
    public void setStavke(List<StavkaRacuna> stavke) { this.stavke = stavke; }
}
