package stil.app.print;

import stil.app.model.Config;
import stil.app.model.Racun;
import stil.app.model.StavkaRacuna;

import javax.print.*;
import java.awt.*;
import java.awt.print.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementira ispis fiskalnog računa na termalni printer (80mm papir).
 *
 * Koristi Java standardni {@link Printable} API bez vanjskih biblioteka.
 * Sadržaj računa se priprema kao lista tekstualnih redaka s fontom (pripremiLinije),
 * a zatim se iscrtava u metodi {@link #print(Graphics, PageFormat, int)}.
 *
 * Format papira: 80mm širina, 297mm visina, margine 3mm sa svake strane.
 * Font: Monospaced 9pt za normalni tekst, 11pt bold za naslove i ukupni iznos.
 * Širina retka: 42 znaka (optimalno za 80mm termalni papir s fontom 9pt).
 *
 * Printer se automatski detektira po imenu (Epson, Star, Bixolon, POS, thermal, receipt).
 * Ako termalni printer nije pronađen, koristi se default sistemski printer.
 *
 * Na računu se ispisuje:
 * - Zaglavlje: naziv tvrtke, adresa, OIB
 * - Broj računa, datum/vrijeme, način plaćanja
 * - Stavke: naziv, količina, iznos, popust (ako postoji), cijena/kom
 * - PDV razrada: osnovica, PDV 25%, ukupno
 * - ZKI i JIR (ako je fiskaliziran)
 * - Zahvala kupcu
 */
public class IspisRacuna implements Printable {

    /** Broj znakova po retku za 80mm papir s Monospaced 9pt fontom. */
    private static final int SIRINA = 42;
    private static final Font FONT_NORMAL = new Font(Font.MONOSPACED, Font.PLAIN, 9);
    private static final Font FONT_BOLD   = new Font(Font.MONOSPACED, Font.BOLD, 9);
    private static final Font FONT_NASLOV = new Font(Font.MONOSPACED, Font.BOLD, 11);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final Racun racun;
    private final Config config;

    /** Lista redaka za ispis: [0] = tekst, [1] = font ključ ("normal", "bold", "naslov"). */
    private final List<String[]> linije = new ArrayList<>();

    /**
     * Kreira instancu i odmah priprema sve retke za ispis.
     *
     * @param racun račun koji se ispisuje
     * @param config konfiguracija s podacima tvrtke
     */
    public IspisRacuna(Racun racun, Config config) {
        this.racun = racun;
        this.config = config;
        pripremiLinije();
    }

    /** Gradi listu svih redaka računa s odgovarajućim fontovima. */
    private void pripremiLinije() {
        String naziv = config.getNazivTvrtke() != null ? config.getNazivTvrtke() : "STIL A j.d.o.o.";
        String adresa = config.getAdresa() != null ? config.getAdresa() : "";
        String oib = config.getOib() != null ? config.getOib() : "";

        // Zaglavlje
        dodaj(centriraj(naziv), "naslov");
        if (!adresa.isEmpty()) dodaj(centriraj(adresa), "normal");
        if (!oib.isEmpty()) dodaj(centriraj("OIB: " + oib), "normal");
        dodaj(linija('-'), "normal");

        // Podaci računa
        dodaj("Racun br: " + racun.getOznakaRacuna(), "bold");
        dodaj("Datum: " + racun.getVrijemeIzdavanja().format(DTF), "normal");
        dodaj("Placanje: " + (racun.getNacinPlacanja() == Racun.NacinPlacanja.GOTOVINA ? "Gotovina" : "Kartica"), "normal");
        dodaj(linija('-'), "normal");

        // Zaglavlje tablice stavki
        dodaj(stupci("Naziv", "Kom", "Iznos"), "bold");
        dodaj(linija('-'), "normal");

        // Stavke
        for (StavkaRacuna s : racun.getStavke()) {
            String ime = skrati(s.getArtiklNaziv(), 22);
            String kom = String.valueOf(s.getKolicina());
            String iznos = String.format("%.2f", s.getUkupno());
            dodaj(stupci(ime, kom, iznos), "normal");
            if (s.getPopust() > 0)
                dodaj("  Popust: " + String.format("%.1f%%", s.getPopust()), "normal");
            dodaj(String.format("  %.2f EUR/kom", s.getCijena()), "normal");
        }

        // PDV razrada i ukupno
        dodaj(linija('-'), "normal");
        double ukupno = racun.getUkupno();
        double pdv = racun.getUkupnoPdv();
        double osnovica = ukupno - pdv;
        dodaj(desno("Osnovica: " + String.format("%.2f EUR", osnovica)), "normal");
        dodaj(desno("PDV 25%:  " + String.format("%.2f EUR", pdv)), "normal");
        dodaj(desno("UKUPNO:   " + String.format("%.2f EUR", ukupno)), "naslov");
        dodaj(linija('='), "normal");

        // Fiskalizacijski kodovi (prazni ako račun nije fiskaliziran)
        if (racun.getZki() != null && !racun.getZki().isEmpty())
            dodaj("ZKI: " + racun.getZki(), "normal");
        if (racun.getJir() != null && !racun.getJir().isEmpty())
            dodaj("JIR: " + racun.getJir(), "normal");

        dodaj(linija('-'), "normal");
        dodaj(centriraj("Hvala na kupnji!"), "bold");
        dodaj("", "normal");
        dodaj("", "normal"); // prazan prostor za odvajanje papira
    }

    private void dodaj(String tekst, String font) {
        linije.add(new String[]{tekst, font});
    }

    /**
     * Iscrtava sadržaj računa na stranicu printera.
     * Poziva Java Print API — ne pozivati direktno.
     */
    @Override
    public int print(Graphics g, PageFormat pf, int page) {
        if (page > 0) return NO_SUCH_PAGE;
        Graphics2D g2 = (Graphics2D) g;
        g2.translate(pf.getImageableX(), pf.getImageableY());
        float y = 0;
        for (String[] l : linije) {
            Font font = switch (l[1]) {
                case "bold"   -> FONT_BOLD;
                case "naslov" -> FONT_NASLOV;
                default       -> FONT_NORMAL;
            };
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            y += fm.getAscent();
            g2.drawString(l[0], 0, y);
            y += fm.getDescent() + fm.getLeading();
        }
        return PAGE_EXISTS;
    }

    /**
     * Ispisuje račun s prikazom dijaloga za odabir printera.
     * Koristiti za ručni ponovni ispis.
     */
    public void ispisi() throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        PrintService termalniPrinter = pronadjiTermalniPrinter();
        if (termalniPrinter != null) {
            try { job.setPrintService(termalniPrinter); } catch (PrinterException ignored) {}
        }
        job.setPrintable(this, termalniFormat(job));
        job.print();
    }

    /**
     * Ispisuje račun bez dijaloga — koristi se automatski nakon naplate.
     * Ako termalni printer nije pronađen, koristi default printer.
     */
    public void ispisiBezDijaloga() throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        PrintService termalniPrinter = pronadjiTermalniPrinter();
        if (termalniPrinter != null) {
            try { job.setPrintService(termalniPrinter); } catch (PrinterException ignored) {}
        }
        job.setPrintable(this, termalniFormat(job));
        job.print();
    }

    /** Definira format papira za 80mm termalni printer. */
    private PageFormat termalniFormat(PrinterJob job) {
        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();
        double sirina = mmToPoints(80);
        double visina = mmToPoints(297);
        paper.setSize(sirina, visina);
        paper.setImageableArea(mmToPoints(3), mmToPoints(3), mmToPoints(74), mmToPoints(291));
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);
        return pf;
    }

    /**
     * Traži termalni printer po poznatim imenima proizvođača.
     * Vraća null ako nije pronađen (tada se koristi default printer).
     */
    private PrintService pronadjiTermalniPrinter() {
        // Ako je printer eksplicitno odabran u postavkama, koristi njega
        String odabrani = config.getNazivPrintera();
        if (odabrani != null && !odabrani.isEmpty()) {
            for (PrintService ps : PrintServiceLookup.lookupPrintServices(null, null)) {
                if (ps.getName().equals(odabrani)) return ps;
            }
        }
        // Fallback: auto-detekcija po poznatim imenima termalnih printera
        for (PrintService ps : PrintServiceLookup.lookupPrintServices(null, null)) {
            String ime = ps.getName().toLowerCase();
            if (ime.contains("thermal") || ime.contains("receipt") || ime.contains("pos")
                    || ime.contains("epson") || ime.contains("star") || ime.contains("bixolon"))
                return ps;
        }
        return null;
    }

    /** Pretvara milimetre u printer points (1 inch = 72 points = 25.4mm). */
    private double mmToPoints(double mm) { return mm * 72.0 / 25.4; }

    /** Centrira tekst unutar širine retka dodavanjem razmaka s lijeve strane. */
    private String centriraj(String t) {
        if (t.length() >= SIRINA) return t;
        return " ".repeat((SIRINA - t.length()) / 2) + t;
    }

    /** Poravnava tekst desno unutar širine retka. */
    private String desno(String t) {
        if (t.length() >= SIRINA) return t;
        return " ".repeat(SIRINA - t.length()) + t;
    }

    /** Vraća liniju sastavljenu od ponavljajućeg znaka (separator). */
    private String linija(char z) { return String.valueOf(z).repeat(SIRINA); }

    /** Skraćuje tekst na max znakova dodajući "." na kraju ako je predugačak. */
    private String skrati(String t, int max) {
        return t.length() > max ? t.substring(0, max - 1) + "." : t;
    }

    /**
     * Formatira tri stupca (lijevo, sredina, desno) unutar širine retka.
     * Lijevi stupac se skraćuje na 22 znaka ako je predugačak.
     */
    private String stupci(String l, String s, String d) {
        int razmak = SIRINA - l.length() - s.length() - d.length();
        return skrati(l, 22) + " ".repeat(Math.max(1, razmak - 1)) + s + " " + d;
    }
}
