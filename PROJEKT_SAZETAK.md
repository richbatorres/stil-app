# stil-app — Sažetak projekta

## Što gradimo
Desktop POS (Point of Sale) blagajna za **STIL A j.d.o.o.** — manja lokalna trgovina odjećom u Hrvatskoj.
Jedan prodavač, offline rad, minimalni troškovi, jednostavno Swing sučelje.

## Tehnički stack
- **Jezik:** Java 21
- **Build:** Gradle 9 (Groovy DSL), single-module projekt (`app`)
- **UI:** Java Swing (sistemski Look and Feel)
- **Baza:** SQLite (`stil.db` u radnom direktoriju, lokalna, bez servera)
- **Fiskalizacija:** HTTPS/SOAP prema Fina CIS servisu, XMLDSig potpis, PKCS12 certifikat
- **Ispis:** Java Print API, termalni printer 80mm, Monospaced font
- **Package root:** `stil.app`

## Struktura paketa
```
stil.app
├── App.java                  — entry point, pokreće Swing EDT i MainWindow
├── model/
│   ├── Artikl.java           — artikl u katalogu (naziv, barkod, cijena, PDV, zaliha)
│   ├── StavkaRacuna.java     — jedna linija računa (denormalizirani naziv, cijena, kolicina, popust)
│   ├── Racun.java            — zaglavlje računa (oznaka, ZKI, JIR, status, stavke)
│   ├── Config.java           — sve postavke aplikacije (OIB, certifikat, PIN, fisk mode, printer)
│   ├── Dobavljac.java        — dobavljač robe (naziv, OIB, kontakt podaci)
│   ├── PovratRobe.java       — povrat robe (od kupca ili dobavljaču, tip, količina, razlog)
│   ├── Nabava.java           — nabava robe od dobavljača (artikl, količina, nabavna cijena)
│   └── IzvjestajPodaci.java  — DTO s agregiranim podacima za izvještaj (prodaja, nabava, povrati, marža)
├── db/
│   └── DatabaseManager.java  — singleton, SQLite CRUD za sve entitete, transakcije, izvještaji
├── util/
│   └── CryptoUtil.java       — AES-256-GCM enkripcija PIN-a i lozinke certifikata
├── ui/
│   ├── MainWindow.java       — JFrame s tabovima (Prodaja, Artikli, Dobavljači, Nabava, Povrat robe, Izvještaji, Postavke)
│   ├── ProdajaPanel.java     — glavni prodajni ekran (lista artikala + košarica + naplata)
│   ├── ArtikliPanel.java     — pregled i upravljanje katalogom artikala
│   ├── ArtiklForm.java       — JDialog za dodavanje/uređivanje artikla + odabir dobavljača
│   ├── DobavljaciPanel.java  — pregled i upravljanje katalogom dobavljača
│   ├── DobavljacForm.java    — JDialog za dodavanje/uređivanje dobavljača
│   ├── NabavaPanel.java      — evidencija nabave robe (unos + tablica pregleda)
│   ├── IzvjestajiPanel.java  — pregled poslovnih izvještaja + storniranje računa
│   ├── PovratRobePanel.java  — evidencija i pregled povrata robe
│   ├── PostavkePanel.java    — postavke tvrtke, fiskalizacije, odabir printera, PIN
│   └── LockScreen.java       — JDialog za zaključavanje blagajne s PIN unosom
├── fisk/
│   ├── ZkiKalkulator.java    — izračun ZKI (SHA1withRSA potpis + MD5 hash)
│   ├── FiskalizacijaZahtjev.java — DTO s podacima za SOAP zahtjev
│   ├── FiskXmlGraditelj.java — gradi i potpisuje SOAP XML (XMLDSig enveloped)
│   └── FiskalizacijaServis.java  — orkestrira fiskalizaciju, HTTPS poziv, parsira JIR
└── print/
    └── IspisRacuna.java      — Printable implementacija za termalni printer 80mm
```

## SQLite schema
| Tablica         | Opis                                                          |
|-----------------|---------------------------------------------------------------|
| `config`        | key-value postavke (OIB, certifikat, PIN, itd.)               |
| `artikl`        | katalog artikala s cijenama i zalihama                        |
| `racun`         | zaglavlja računa (ZKI, JIR, status)                           |
| `stavka_racuna` | stavke računa (denormalizirani naziv artikla)                 |
| `dobavljac`     | katalog dobavljača (naziv, OIB, kontakt podaci, napomena)     |
| `povrat_robe`   | evidencija povrata robe (tip, artikl, dobavljač, razlog)      |
| `nabava`        | evidencija nabave robe (artikl, dobavljač, količina, nab. cijena) |
| `artikl_dobavljac` | many-to-many veza artikla i dobavljača (CASCADE delete) |

## Tok naplate (ProdajaPanel.naplati)
1. Spremi račun + stavke u bazu (transakcija, smanjuje zalihe)
2. Fiskalizacija (ako su OIB i certifikat postavljeni):
   - Izračunaj ZKI → pošalji SOAP → dobij JIR → ažuriraj bazu
   - Ako ne uspije: upozorenje, račun ostaje lokalno
3. Ispis na termalni printer bez dijaloga
   - Ako ne uspije: upozorenje, račun je već u bazi

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
- CRUD kroz tab "Nabava" u MainWindow

## Izvještaji
- Pregled poslovnih rezultata za proizvoljno vremensko razdoblje
- Agregirani podaci: ukupna prodaja (bruto + PDV), po načinu plaćanja, nabava, povrati, neto promet, bruto marža
- Detaljna tablica računa za odabrano razdoblje
- Stornirani računi se ne ubrajaju u prodaju
- Brzi gumbi: Danas, Ovaj mjesec, Ova godina
- Svi iznosi računaju se direktno u SQL-u — uvijek točni i ažurni

## Dobavljači
- Evidencija dobavljača robe s kontakt podacima (naziv, OIB, adresa, email, telefon, napomena)
- OIB je opcionalan (strani dobavljači)
- CRUD operacije kroz tab "Dobavljači" u MainWindow
- Dobavljači se koriste pri povratu robe tipa DOBAVLJACU

## Povrat robe
- Dva tipa povrata:
  - **OD_KUPCA**: kupac vraća robu → zaliha artikla se **povećava**
  - **DOBAVLJACU**: trgovina vraća robu dobavljaču → zaliha artikla se **smanjuje**
- Svaki povrat bilježi: artikl, tip, dobavljač (opcionalno), količinu, cijenu, razlog, datum/vrijeme
- Zaliha se ažurira atomarno u transakciji zajedno s pohranom povrata
- Pregled svih povrata u tablici (sortirano od najnovijeg)

## Fiskalizacija
- **Test okruženje:** `https://cistest.apis-it.hr:8449/FiskalizacijaService` (default)
- **Produkcija:** `https://cis.porezna-uprava.hr:8449/FiskalizacijaService`
- Toggle u Postavke kartici — checkbox "Koristi testno okruženje"
- Certifikat: PKCS12 (.p12) od Fine, putanja i lozinka u Postavkama
- ZKI algoritam: konkatenacija (OIB+datum+brRac+posProstor+uredaj+iznos) → SHA1withRSA → MD5 → hex
- XML namespace: `http://www.apis-it.hr/fin/2012/types/f73`
- OIB operatera = OIB tvrtke (j.d.o.o., jedan vlasnik)
- PDV: samo 25% stopa, svi artikli

## Zaključavanje blagajne
- Aplikacija se zaključa pri pokretanju i traži PIN
- Ako PIN nije postavljen, prolazi bez unosa
- Gumb "🔒 Zaključaj" u gornjem desnom kutu
- PIN se postavlja u Postavke kartici (dvostruki unos za potvrdu)

## Ispis računa
- Termalni printer 80mm, automatska detekcija po imenu (Epson, Star, Bixolon, POS...)
- Monospaced font, 42 znaka po retku
- Sadržaj: zaglavlje tvrtke, stavke, PDV razrada, ZKI/JIR, zahvala

## Testovi
- Framework: JUnit Jupiter 5.12.1
- Pokretanje: `cmd /c run-tests.bat` (bez Gradle, direktno javac + java)
- Testne klase u `app/src/test/java/stil/app/`
- DatabaseManagerTest koristi privremenu SQLite bazu (refleksija za zamjenu connection-a)

| Testna klasa              | Testova | Pokriva                                      |
|---------------------------|---------|----------------------------------------------|
| `ArtiklTest`              | 6       | model Artikl                                 |
| `StavkaRacunaTest`        | 7       | model StavkaRacuna, izračun ukupnog           |
| `RacunTest`               | 9       | model Racun, PDV izračun                     |
| `ConfigTest`              | 5       | model Config                                 |
| `DobavljacTest`           | 6       | model Dobavljac                              |
| `PovratRobeTest`          | 10      | model PovratRobe, promjena zalihe            |
| `NabavaTest`              | 7       | model Nabava, izračun ukupnog troška         |
| `IzvjestajPodaciTest`     | 6       | DTO IzvjestajPodaci, marža, neto promet      |
| `CryptoUtilTest`          | 11      | AES-256-GCM enkripcija, round-trip, edge cases |
| `FiskalizacijaZahtjevTest`| 3       | DTO FiskalizacijaZahtjev                     |
| `ZkiKalkulatorTest`       | 6       | ZKI algoritam (RSA + MD5)                    |
| `DatabaseManagerTest`     | 69      | CRUD + nabava + izvještaji + storniranje + artikl-dobavljač veza |
| `IspisRacunaTest`         | 8       | priprema linija za ispis                     |
| `AppTest`                 | 1       | postojanje paketa                            |
| **Ukupno**                | **142** | **142/142 prolazi ✅**                        |

## Pokretanje aplikacije
```bat
start "STIL App" cmd /k "cd /d c:\Users\ZBARTIN\ONEDRI~1\DOCUME~1\stil-app && start-app.bat"
```

## Pokretanje testova
```bat
cmd /c "c:\Users\ZBARTIN\ONEDRI~1\DOCUME~1\stil-app\run-tests.bat"
```

## Što nedostaje / sljedeći koraci
- Enkripcija ostalih config polja (adresa, OIB) — trenutno samo PIN i lozinka certifikata
- Odabir printera u Postavkama (implementirano ✅)
- Storniranje računa (implementirano ✅)
- Vezanje artikla uz dobavljača (implementirano ✅)
