package stil.app.fisk;

import stil.app.model.Config;
import stil.app.model.Racun;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.format.DateTimeFormatter;

/**
 * Glavni servis koji orkestrira cijeli proces fiskalizacije računa.
 *
 * Redoslijed operacija pri pozivu {@link #fiskaliziraj(Racun)}:
 * 1. Učitava PKCS12 certifikat s diska
 * 2. Izračunava ZKI (ZkiKalkulator) i postavlja ga na račun
 * 3. Gradi i potpisuje SOAP XML (FiskXmlGraditelj)
 * 4. Šalje HTTPS POST zahtjev prema Fina servisu
 * 5. Parsira odgovor i vraća JIR
 *
 * Podržava testno i produkcijsko okruženje:
 * - Test:       https://cistest.apis-it.hr:8449/FiskalizacijaService
 * - Produkcija: https://cis.porezna-uprava.hr:8449/FiskalizacijaService
 *
 * SSL konfiguracija koristi klijentski certifikat iz .p12 datoteke i
 * sistemske CA certifikate za provjeru Fina servera.
 *
 * Ako fiskalizacija ne uspije, pozivatelj (ProdajaPanel) hvata iznimku,
 * prikazuje upozorenje, ali račun ostaje pohranjen lokalno u bazi.
 */
public class FiskalizacijaServis {

    private static final String URL_PRODUKCIJA = "https://cis.porezna-uprava.hr:8449/FiskalizacijaService";
    private static final String URL_TEST = "https://cistest.apis-it.hr:8449/FiskalizacijaService";

    /** Format datuma/vremena koji Fina očekuje u XML zahtjevu. */
    private static final DateTimeFormatter DTF_FISK = DateTimeFormatter.ofPattern("dd.MM.yyyy'T'HH:mm:ss");

    private final Config config;
    private final boolean testMode;

    /**
     * Kreira servis koji koristi testno okruženje Fine (cistest.apis-it.hr).
     * Koristiti za razvoj i testiranje.
     */
    public FiskalizacijaServis(Config config) {
        this(config, true);
    }

    /**
     * Kreira servis s eksplicitnim odabirom okruženja.
     *
     * @param config konfiguracijske postavke s putanjom certifikata i OIB-om
     * @param testMode true = testno okruženje, false = produkcija
     */
    public FiskalizacijaServis(Config config, boolean testMode) {
        this.config = config;
        this.testMode = testMode;
    }

    /**
     * Fiskalizira račun — izračunava ZKI, šalje zahtjev Fini i vraća JIR.
     * Postavlja ZKI na objekt računa kao side-effect.
     *
     * @param racun račun koji se fiskalizira (mora imati stavke i vrijemeIzdavanja)
     * @return JIR (Jedinstveni Identifikator Računa) od Fine
     * @throws Exception ako certifikat nije dostupan, mreža nije dostupna ili Fina vrati grešku
     */
    public String fiskaliziraj(Racun racun) throws Exception {
        KeyStore keyStore = ucitajCertifikat();
        String alias = keyStore.aliases().nextElement();

        BigDecimal ukupno = BigDecimal.valueOf(racun.getUkupno()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pdv = BigDecimal.valueOf(racun.getUkupnoPdv()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal osnovica = ukupno.subtract(pdv);

        // Izračun ZKI i postavljanje na račun
        String zki = ZkiKalkulator.izracunaj(
            config.getOib(),
            racun.getVrijemeIzdavanja(),
            String.valueOf(racun.getBrojRacuna()),
            config.getOznakaPosProstora(),
            config.getOznakaUredaja(),
            ukupno,
            (java.security.PrivateKey) keyStore.getKey(alias, config.getLozinkaCertifikata().toCharArray())
        );
        racun.setZki(zki);

        // Priprema i slanje SOAP zahtjeva
        FiskalizacijaZahtjev z = new FiskalizacijaZahtjev();
        z.setOib(config.getOib());
        z.setDatumVrijemePoruke(racun.getVrijemeIzdavanja().format(DTF_FISK));
        z.setDatumVrijemeRacuna(racun.getVrijemeIzdavanja().format(DTF_FISK));
        z.setBrojRacuna(String.valueOf(racun.getBrojRacuna()));
        z.setOznakaPosProstora(config.getOznakaPosProstora());
        z.setOznakaUredaja(config.getOznakaUredaja());
        z.setIznosUkupno(ukupno.toPlainString());
        z.setOsnovica(osnovica.toPlainString());
        z.setIznosPdv(pdv.toPlainString());
        z.setNacinPlacanja(racun.getNacinPlacanja() == Racun.NacinPlacanja.GOTOVINA ? "G" : "K");
        z.setZki(zki);

        String xml = FiskXmlGraditelj.izgradiRacunZahtjev(z, keyStore, alias, config.getLozinkaCertifikata());
        String odgovor = posaljiSoap(xml, keyStore);
        return izvuciJir(odgovor);
    }

    /** Učitava PKCS12 certifikat s putanje definirane u konfiguraciji. */
    private KeyStore ucitajCertifikat() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(config.getPutanjaCertifikata())) {
            ks.load(fis, config.getLozinkaCertifikata().toCharArray());
        }
        return ks;
    }

    /**
     * Šalje SOAP XML kao HTTPS POST zahtjev prema Fina servisu.
     * Koristi klijentski certifikat za mTLS autentikaciju.
     *
     * @param xml potpisan SOAP XML string
     * @param keyStore keystore s klijentskim certifikatom
     * @return tijelo HTTP odgovora kao String
     * @throws IOException ako server vrati status != 200
     */
    private String posaljiSoap(String xml, KeyStore keyStore) throws Exception {
        SSLContext sslContext = postaviSsl(keyStore);
        URL url = new URL(testMode ? URL_TEST : URL_PRODUKCIJA);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        if (conn instanceof HttpsURLConnection httpsConn) {
            httpsConn.setSSLSocketFactory(sslContext.getSocketFactory());
        }

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        conn.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
        conn.setRequestProperty("SOAPAction", "");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(xml.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String odgovor = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        if (status != 200) {
            throw new IOException("Fina server vratio status " + status + ": " + odgovor);
        }
        return odgovor;
    }

    /**
     * Konfigurira SSL kontekst s klijentskim certifikatom za mTLS
     * i sistemskim CA certifikatima za provjeru Fina servera.
     */
    private SSLContext postaviSsl(KeyStore keyStore) throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, config.getLozinkaCertifikata().toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null); // null = koristi sistemske CA certifikate (Windows Certificate Store)

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return ctx;
    }

    /**
     * Parsira XML odgovor Fine i izvlači JIR vrijednost.
     * Ako odgovor sadrži grešku (PorukaGreske), baca iznimku s porukom greške.
     *
     * @param xml SOAP XML odgovor od Fine
     * @return JIR string
     * @throws IOException ako JIR nije pronađen ili odgovor sadrži grešku
     */
    private String izvuciJir(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(
            new org.xml.sax.InputSource(new StringReader(xml)));

        NodeList jirNodes = doc.getElementsByTagNameNS("*", "Jir");
        if (jirNodes.getLength() > 0) return jirNodes.item(0).getTextContent();

        NodeList greske = doc.getElementsByTagNameNS("*", "PorukaGreske");
        if (greske.getLength() > 0) throw new IOException("Fina greška: " + greske.item(0).getTextContent());

        throw new IOException("JIR nije pronađen u odgovoru.");
    }
}
