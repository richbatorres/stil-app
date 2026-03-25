package stil.app.model;

/**
 * Model koji drži sve konfiguracijske postavke aplikacije.
 *
 * Postavke se pohranjuju u SQLite tablici {@code config} kao key-value parovi.
 * Učitavaju se pri pokretanju i mogu se mijenjati kroz karticu Postavke u UI-u.
 *
 * Osjetljivi podaci (lozinka certifikata, PIN) pohranjuju se u bazu bez enkripcije —
 * prihvatljivo za lokalno single-user okruženje, ali treba imati na umu pri deploymentu.
 */
public class Config {
    private String nazivTvrtke;          // npr. "STIL A j.d.o.o."
    private String oib;                  // OIB tvrtke, 11 znamenki
    private String adresa;               // adresa poslovnog prostora za ispis na računu
    private String oznakaPosProstora;    // oznaka poslovnog prostora za fiskalizaciju (npr. "PP1")
    private String oznakaUredaja;        // oznaka naplatnog uređaja za fiskalizaciju (npr. "1")
    private String putanjaCertifikata;   // apsolutna putanja do .p12 certifikata od Fine
    private String lozinkaCertifikata;   // lozinka za otvaranje .p12 certifikata
    private String pin;                  // PIN za zaključavanje blagajne (null = bez PINa)
    private boolean fiskTestMode = true; // true = testno okruženje Fine, false = produkcija
    private String nazivPrintera;        // naziv odabranog printera (null = auto-detekcija)

    public String getNazivTvrtke() { return nazivTvrtke; }
    public void setNazivTvrtke(String nazivTvrtke) { this.nazivTvrtke = nazivTvrtke; }

    public String getOib() { return oib; }
    public void setOib(String oib) { this.oib = oib; }

    public String getAdresa() { return adresa; }
    public void setAdresa(String adresa) { this.adresa = adresa; }

    public String getOznakaPosProstora() { return oznakaPosProstora; }
    public void setOznakaPosProstora(String oznakaPosProstora) { this.oznakaPosProstora = oznakaPosProstora; }

    public String getOznakaUredaja() { return oznakaUredaja; }
    public void setOznakaUredaja(String oznakaUredaja) { this.oznakaUredaja = oznakaUredaja; }

    public String getPutanjaCertifikata() { return putanjaCertifikata; }
    public void setPutanjaCertifikata(String putanjaCertifikata) { this.putanjaCertifikata = putanjaCertifikata; }

    public String getLozinkaCertifikata() { return lozinkaCertifikata; }
    public void setLozinkaCertifikata(String lozinkaCertifikata) { this.lozinkaCertifikata = lozinkaCertifikata; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    /** @return true ako aplikacija koristi Fina testno okruženje (cistest.apis-it.hr) */
    public boolean isFiskTestMode() { return fiskTestMode; }
    public void setFiskTestMode(boolean fiskTestMode) { this.fiskTestMode = fiskTestMode; }

    public String getNazivPrintera() { return nazivPrintera; }
    public void setNazivPrintera(String nazivPrintera) { this.nazivPrintera = nazivPrintera; }
}
