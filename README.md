# STIL — Desktop POS Application

A desktop point-of-sale application for a small local clothing store in Croatia.  
Offline operation, single cashier, simple Swing UI.

## Features

- **Sales** — cashier screen with shopping cart, cash and card payment, automatic stock reduction
- **Fiscalization** — SOAP/HTTPS to Fina CIS service, ZKI + JIR, XMLDSig signing, test and production environments
- **Receipt printing** — PDF on 80mm thermal printer (Apache PDFBox)
- **Items & suppliers** — catalog with many-to-many relationship, CRUD interface
- **Stock intake** — incoming goods with purchase price, automatic stock increase
- **Returns** — from customer (increases stock) and to supplier (decreases stock)
- **Consignment** — tracking of consignment items per supplier and billing periods
- **Reports** — sales, intake, returns, margin for any date range; PDF export and printing
- **Daily closing** — end-of-day report with PDF storage and printing
- **Register lock** — PIN protection (AES-256-GCM encryption)
- **UI zoom** — Ctrl+scroll to change font size (10–48pt)

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Build | Gradle 9 (Groovy DSL) |
| UI | Java Swing (system L&F) |
| Database | SQLite (`stil.db`, local, no server) |
| Fiscalization | HTTPS/SOAP, XMLDSig, PKCS12 certificate |
| Printing | Apache PDFBox 3.0.3, Java Print API |
| Tests | JUnit Jupiter 5.12.1 |

## Running

**Prerequisite:** Java 21 (JDK)

```bat
start-app.bat
```

The script compiles all classes and starts the application. The `stil.db` database is created automatically on first run in the working directory.

## Tests

```bat
run-tests.bat
```

202 tests covering models, database, cryptography, fiscalization and printing.

## Project Structure

```
stil-app/
├── app/
│   ├── src/main/java/stil/app/
│   │   ├── model/          — domain models (Artikl, Racun, Nabava, ...)
│   │   ├── db/             — DatabaseManager (SQLite CRUD, reports)
│   │   ├── fisk/           — fiscalization (ZKI, XML, SOAP)
│   │   ├── print/          — PDF generation (receipts, reports, daily closing)
│   │   ├── ui/             — Swing panels and forms
│   │   ├── util/           — CryptoUtil, Validator
│   │   └── App.java        — entry point
│   └── src/test/           — JUnit tests
├── app/libs/               — SQLite + PDFBox JARs
├── start-app.bat           — compile and run
├── run-tests.bat           — run tests
└── stil.db                 — SQLite database (gitignored)
```

## Fiscalization

Croatian fiscal law requires all receipts to be signed and submitted to the tax authority in real time.

- **Test:** `https://cistest.apis-it.hr:8449/FiskalizacijaService`
- **Production:** `https://cis.porezna-uprava.hr:8449/FiskalizacijaService`
- Certificate: PKCS12 (.p12) from Fina, path and password configured in Settings tab
- ZKI: SHA1withRSA signature → MD5 hash (per Fina specification)
- Toggle test/production in Settings

## Security

- PIN and certificate password are encrypted with AES-256-GCM before being stored in the database
- The encryption key is derived from the OS username and hostname (machine-specific)
- Encrypted values are prefixed with `ENC:`

## License

Private project — not for public distribution.
