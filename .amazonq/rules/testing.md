# Pravilo: Testiranje nakon svake izmjene

## Koji testovi se pokreću

Nakon svake izmjene procijeni koje klase su izmijenjene i pokreni samo relevantne testove:

| Izmijenjena klasa | Pokreni testove |
|---|---|
| `DatabaseManager` | `DatabaseManagerTest` |
| `Dobavljac`, `DobavljacForm` | `DobavljacTest`, `DatabaseManagerTest` |
| `Artikl`, `ArtiklForm` | `ArtiklTest`, `DatabaseManagerTest` |
| `Racun`, `StavkaRacuna` | `RacunTest`, `StavkaRacunaTest`, `DatabaseManagerTest` |
| `Nabava` | `NabavaTest`, `DatabaseManagerTest` |
| `PovratRobe` | `PovratRobeTest`, `DatabaseManagerTest` |
| `IzvjestajPodaci` | `IzvjestajPodaciTest`, `DatabaseManagerTest` |
| `KomisijaStavka` | `KomisijaStavkaTest`, `DatabaseManagerTest` |
| `Config` | `ConfigTest`, `DatabaseManagerTest` |
| `CryptoUtil` | `CryptoUtilTest` |
| `Validator` | `ValidatorTest` |
| `IspisRacuna` | `IspisRacunaTest` |
| `ZkiKalkulator` | `ZkiKalkulatorTest` |
| `FiskalizacijaZahtjev` | `FiskalizacijaZahtjevTest` |
| `TestDataGenerator` | `DatabaseManagerTest` (kompajliranje dovoljno) |
| UI klase (`*Panel`, `*Form`) | samo kompajliranje — UI nema unit testova |
| Više klasa odjednom | `DatabaseManagerTest` + relevantni model testovi |

## Kada pokrenuti SVE testove

- Prije git push/commit
- Nakon refaktoriranja koje dira više paketa
- Nakon izmjene `DatabaseManager.initSchema()` ili FK strukture
- Kad nisi siguran koji testovi su pogođeni

## Kako pokrenuti podskup testova

`run-tests.bat` uvijek kompajlira sve klase, ali možeš privremeno komentirati
nepotrebne test klase u sekciji `[2/3]` i runner sekciji.

Alternativno, za brzu provjeru jedne klase:
```
run-tests.bat  (sve — ~30s)
```
Za samo kompajliranje (provjera grešaka bez pokretanja):
```
javac ... (samo main klase)
```

## Obavezni uvjeti

- Svaki test mora biti deterministički — ne smije ovisiti o redoslijedu izvršavanja
- Napiši testove za sve nove javne metode i DB operacije
- Robusni testovi: null vrijednosti, prazni skupovi, granični slučajevi, rollback pri grešci
- Ažuriraj `run-tests.bat` kad dodaješ novu test klasu
