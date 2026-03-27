# stil-app — Sažetak projekta

## Što gradimo
Desktop POS (Point of Sale) blagajna za manju lokalnu trgovinu odjećom u Hrvatskoj.
Jedan prodavač, offline rad, minimalni troškovi, jednostavno Swing sučelje.

## Tehnički stack
- **Jezik:** Java 21
- **Build:** Gradle 9 (Groovy DSL), single-module projekt (`app`)
- **UI:** Java Swing (sistemski Look and Feel)
- **Baza:** SQLite (`stil.db` u radnom direktoriju, lokalna, bez servera)
- **Fiskalizacija:** HTTPS/SOAP prema Fina CIS servisu, XMLDSig potpis, PKCS12 certifikat
- **Ispis:** Apache PDFBox 3.0.3, termalni printer 80mm, PDF format
- **Package root:** `stil.app`

## Struktura paketa
```
stil.app
├── App.java                  — entry point, inicijalni font 20pt, Ctrl+scroll zoom, pokreće MainWindow
├── model/
│   ├── Artikl.java           — artikl u katalogu (naziv, barkod, cijena, PDV, zaliha)
│   ├── StavkaRacuna.java     — jedna linija računa (denormalizirani naziv, cijena, kolicina, popust)
│   ├── Racun.java            — zaglavlje računa (oznaka, ZKI, JIR, status, stavke)
│   ├── Config.java           — sve postavke aplikacije (OIB, certifikat, PIN, fisk mode, printer)
│   ├── Dobavljac.java        — dobavljač robe (naziv, OIB, kontakt podaci)
│   ├── PovratRobe.java       — povrat robe (od kupca ili dobavljaču, tip, količina, razlog)
│   ├── Nabava.java           — nabava robe od dobavljača (artikl, količina, nabavna cijena)
│   ├── IzvjestajPodaci.java  — DTO s agregiranim podacima za izvještaj (prodaja, nabava, povrati, marža)
│   ├── KomisijaStavka.java   — komisijska stavka po dobavljaču i obračunskom periodu
│   └── Zaklucnica.java       — model dnevne zaključnice (iznosi, putanja PDF-a)
├── db/
│   ├── DatabaseManager.java  — singleton, SQLite CRUD za sve entitete, transakcije, izvještaji, keširani PS
│   └── TestDataGenerator.java — generira testne podatke (artikli, dobavljači, računi, komisija...)
├── util/
│   ├── CryptoUtil.java       — AES-256-GCM enkripcija PIN-a i lozinke certifikata
│   └── Validator.java        — validacija unosa, headless-safe upozorenja
├── ui/
│   ├── MainWindow.java       — JFrame s tabovima + gumb Zaključnica
│   ├── FontManager.java      — upravljanje veličinom fonta (10–48pt), rekurzivno ažuriranje komponenti
│   ├── ProdajaPanel.java     — glavni prodajni ekran (lista artikala + košarica + naplata)
│   ├── ArtikliPanel.java     — pregled i upravljanje katalogom artikala
│   ├── ArtiklForm.java       — JDialog za dodavanje/uređivanje artikla + odabir dobavljača
│   ├── DobavljaciPanel.java  — pregled i upravljanje katalogom dobavljača
│   ├── DobavljacForm.java    — JDialog za dodavanje/uređivanje dobavljača
│   ├── NabavaPanel.java      — evidencija nabave robe (unos + tablica pregleda)
│   ├── IzvjestajiPanel.java  — pregled poslovnih izvještaja + storniranje + ispis izvještaja
│   ├── PovratRobePanel.java  — evidencija i pregled povrata robe
│   ├── KomisijaPanel.java    — pregled komisijskih stavki po dobavljaču i periodu
│   ├── PostavkePanel.java    — postavke tvrtke, fiskalizacije, odabir printera, PIN
│   └── LockScreen.java       — JDialog za zaključavanje blagajne s PIN unosom
├── fisk/
│   ├── ZkiKalkulator.java    — izračun ZKI (SHA1withRSA potpis + MD5 hash)
│   ├── FiskalizacijaZahtjev.java — DTO s podacima za SOAP zahtjev
│   ├── FiskXmlGraditelj.java — gradi i potpisuje SOAP XML (XMLDSig enveloped)
│   └── FiskalizacijaServis.java  — orkestrira fiskalizaciju, HTTPS poziv, parsira JIR
└── print/
    ├── IspisRacuna.java      — PDF generiranje računa (80mm, PDFBox), spremi + ispis bez dijaloga
    └── IspisIzvjestaja.java  — PDF generiranje izvještaja i zaključnice, ispis
```

## SQLite schema
| Tablica            | Opis                                                              |
|--------------------|-------------------------------------------------------------------|
| `config`           | key-value postavke (OIB, certifikat, PIN, itd.)                   |
| `artikl`           | katalog artikala s cijenama i zalihama                            |
| `racun`            | zaglavlja računa (ZKI, JIR, status)                               |
| `stavka_racuna`    | stavke računa (denormalizirani naziv artikla)                     |
| `dobavljac`        | katalog dobavljača (naziv, OIB, kontakt podaci, napomena)         |
| `povrat_robe`      | evidencija povrata robe (tip, artikl, dobavljač, razlog)          |
| `nabava`           | evidencija nabave robe (artikl, dobavljač, količina, nab. cijena) |
| `artikl_dobavljac` | many-to-many veza artikla i dobavljača (CASCADE delete)           |
| `komisija_stavka`  | komisijske stavke po dobavljaču i obračunskom periodu             |
| `zaklucnica`       | dnevne zaključnice (iznosi, putanja PDF-a, timestamp)             |

## Tok naplate (ProdajaPanel.naplati)
1. Spremi račun + stavke u bazu (transakcija, smanjuje zalihe)
2. Fiskalizacija (ako su OIB i certifikat postavljeni):
   - Izračunaj ZKI → pošalji SOAP → dobij JIR → ažuriraj bazu
   - Ako ne uspije: upozorenje, račun ostaje lokalno
3. Generiraj PDF račun → spremi u `racuni/` → ispis bez dijaloga
   - Ako ne uspije: upozorenje, račun je već u bazi

## Ispis računa (IspisRacuna)
- PDF format, 80mm širina (227pt), Apache PDFBox 3.0.3
- `spremiRacun(dir)` — retry + fallback naziv + verifikacija, vraća `Path`
- `ispisiBezDijaloga()` — vraća `String` opis greške (null = uspjeh)
- `ascii()` helper — zamjenjuje hrvatska slova za Standard14 fontove

## Ispis izvještaja i zaključnice (IspisIzvjestaja)
- `spremiIzvjestajPdf()` — generira PDF izvještaja za odabrano razdoblje
- `spremiZaklucnicuPdf()` — generira PDF dnevne zaključnice, sprema u `zaklucnice/`
- `ispisiPdf()` — ispisuje PDF na odabrani printer
- `opisGreskePrintera()` — čitljiv opis greške printera (ne null)

## Zaključnica
- Gumb "Zaključnica" u MainWindow generira dnevni izvještaj
- Sprema PDF u `zaklucnice/zaklucnica_YYYYMMDD_HHmmss.pdf`
- Sprema zapis u tablicu `zaklucnica` u bazi
- Ispisuje na printer bez dijaloga

## Zoom sučelja (FontManager)
- `volatile float trenutna = 20f` — inicijalni font 20pt
- `postavi(float, Window)` — ažurira UIManager i rekurzivno sve komponente
- `azurirajKomponente()` — postavlja font i `rowHeight = velicina * 1.8f` na JTable
- Ctrl+scroll u App.java mijenja font 10–48pt

## Enkripcija osjetljivih podataka
- PIN i lozinka certifikata enkriptiraju se AES-256-GCM prije pohrane u bazu
- Ključ se izvodi iz korisničkog imena OS-a i hostnamea (machine-specific)
- Enkriptirane vrijednosti imaju prefiks `ENC:` — stare nešifrirane vrijednosti rade bez migracije
- `CryptoUtil.encrypt()` je idempotentna — ne enkriptira već enkriptirane vrijednosti

## Storniranje računa
- Gumb "❌ Storniraj račun" u tabu Izvještaji, uz odabrani račun u tablici
- Stornirani računi imaju status STORNIRAN i ne ubrajaju se u izvještaje prodaje
- Storniranje je evidencijsko — ne vraća zalihe automatski (za to koristiti Povrat robe)

## Odabir printera
- Dropdown u Postavkama prikazuje sve instalirane printere
- Opcija "(auto-detekcija)" traži termalni printer po imenu (Epson, Star, Bixolon, POS...)
- Odabrani printer pohranjuje se u config tablici kao `naziv_printera`

## Artikl-Dobavljač veza
- Many-to-many relacija u tablici `artikl_dobavljac`
- Višestruki odabir dobavljača u ArtiklForm (JList s MULTIPLE_INTERVAL_SELECTION)
- CASCADE delete — brisanje artikla ili dobavljača automatski briše veze

## Nabava robe
- Evidencija ulaza robe u skladište s nabavnom cijenom po komadu
- Svaka nabava atomarno povećava zalihu artikla u istoj transakciji (rollback pri grešci)
- Opcionalna veza na dobavljača (denormaliziran naziv za povijesnu točnost)

## Komisija
- Evidencija komisijskih artikala po dobavljaču i obračunskom periodu (mjesec/godina)
- KomisijaPanel — odabir dobavljača i perioda, prikaz stavki u tablici
- TestDataGenerator generira komisijske nabave za zadnjih 6 završenih mjeseci

## Izvještaji
- Agregirani podaci: ukupna prodaja (bruto + PDV), po načinu plaćanja, nabava, povrati, neto promet, bruto marža
- Detaljna tablica računa za odabrano razdoblje
- Brzi gumbi: Danas, Ovaj mjesec, Ova godina
- Gumb "Ispis izvještaja" — generira PDF i ispisuje

## Fiskalizacija
- **Test okruženje:** `https://cistest.apis-it.hr:8449/FiskalizacijaService` (default)
- **Produkcija:** `https://cis.porezna-uprava.hr:8449/FiskalizacijaService`
- Toggle u Postavke kartici — checkbox "Koristi testno okruženje"
- Certifikat: PKCS12 (.p12) od Fine, putanja i lozinka u Postavkama
- ZKI algoritam: konkatenacija (OIB+datum+brRac+posProstor+uredaj+iznos) → SHA1withRSA → MD5 → hex
- XML namespace: `http://www.apis-it.hr/fin/2012/types/f73`
- PDV: samo 25% stopa, svi artikli

## Zaključavanje blagajne
- Aplikacija se zaključa pri pokretanju i traži PIN
- Ako PIN nije postavljen, prolazi bez unosa
- Gumb "🔒 Zaključaj" u gornjem desnom kutu
- PIN se postavlja u Postavke kartici (dvostruki unos za potvrdu)

## Testovi
- Framework: JUnit Jupiter 5.12.1
- Pokretanje: `run-tests.bat` (bez Gradle, direktno javac + java)
- Testne klase u `app/src/test/java/stil/app/`
- DatabaseManagerTest koristi privremenu SQLite bazu (refleksija za zamjenu connection-a)
- Validator koristi `GraphicsEnvironment.isHeadless()` — testovi rade u headless modu

| Testna klasa               | Testova | Pokriva                                               |
|----------------------------|---------|-------------------------------------------------------|
| `ArtiklTest`               | 6       | model Artikl                                          |
| `StavkaRacunaTest`         | 7       | model StavkaRacuna, izračun ukupnog                   |
| `RacunTest`                | 9       | model Racun, PDV izračun                              |
| `ConfigTest`               | 5       | model Config                                          |
| `DobavljacTest`            | 6       | model Dobavljac                                       |
| `PovratRobeTest`           | 10      | model PovratRobe, promjena zalihe                     |
| `NabavaTest`               | 7       | model Nabava, izračun ukupnog troška                  |
| `IzvjestajPodaciTest`      | 6       | DTO IzvjestajPodaci, marža, neto promet               |
| `KomisijaStavkaTest`       | 5       | model KomisijaStavka                                  |
| `CryptoUtilTest`           | 11      | AES-256-GCM enkripcija, round-trip, edge cases        |
| `ValidatorTest`            | 4       | validacija unosa, headless mode                       |
| `FiskalizacijaZahtjevTest` | 3       | DTO FiskalizacijaZahtjev                              |
| `ZkiKalkulatorTest`        | 6       | ZKI algoritam (RSA + MD5)                             |
| `DatabaseManagerTest`      | 69      | CRUD + nabava + izvještaji + storniranje + komisija   |
| `IspisRacunaTest`          | 9       | PDF generiranje računa, veličina, ZKI/JIR, kartica    |
| `FontManagerTest`          | 8       | clamping, propagacija na Label/Table/Panel, null font |
| `AppTest`                  | 1       | postojanje paketa                                     |
| **Ukupno**                 | **202** | **202/202 prolazi ✅**                                 |

## Pokretanje aplikacije
```bat
start-app.bat
```

## Pokretanje testova
```bat
run-tests.bat
```

## Vanjske ovisnosti (app/libs/)
| JAR                          | Verzija | Svrha                    |
|------------------------------|---------|--------------------------|
| `sqlite-jdbc-3.47.1.0.jar`   | 3.47.1  | SQLite driver            |
| `pdfbox-3.0.3.jar`           | 3.0.3   | PDF generiranje          |
| `pdfbox-io-3.0.3.jar`        | 3.0.3   | PDFBox I/O               |
| `fontbox-3.0.3.jar`          | 3.0.3   | PDFBox font podrška      |
| `commons-logging-1.3.3.jar`  | 1.3.3   | PDFBox logging           |
