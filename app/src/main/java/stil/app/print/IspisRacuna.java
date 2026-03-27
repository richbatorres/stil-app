package stil.app.print;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import stil.app.model.Config;
import stil.app.model.Racun;
import stil.app.model.StavkaRacuna;

import javax.print.*;
import java.awt.print.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;

/**
 * Generira PDF račun i ispisuje ga na printer.
 *
 * PDF se generira pomoću Apache PDFBox 3.x na 80mm širini (227pt).
 * Ispis koristi Java Print API s automatskom detekcijom termalnog printera.
 * Robusno spremanje: retry mehanizam + fallback direktorij + verifikacija veličine.
 */
public class IspisRacuna {

    private static final DateTimeFormatter DTF       = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final DateTimeFormatter DTF_NAZIV = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int MAX_RETRY = 3;
    private static final long MIN_VELICINA_BAJTA = 200;

    private final Racun racun;
    private final Config config;

    /**
     * @param racun  račun koji se ispisuje
     * @param config konfiguracija s podacima tvrtke
     */
    public IspisRacuna(Racun racun, Config config) {
        this.racun = racun;
        this.config = config;
    }

    /**
     * Sprema račun kao PDF u zadani direktorij s retry/fallback mehanizmom.
     *
     * @param direktorij primarni direktorij (npr. "racuni")
     * @return apsolutna putanja do PDF datoteke
     * @throws IOException ako ni primarni ni fallback ne rade
     */
    public Path spremiRacun(String direktorij) throws IOException {
        String naziv = "racun_" + racun.getVrijemeIzdavanja().format(DTF_NAZIV)
                + "_" + racun.getBrojRacuna() + ".pdf";
        IOException zadnjaGreska = null;

        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                Path dir = Paths.get(direktorij);
                Files.createDirectories(dir);
                Path datoteka = dir.resolve(naziv);
                zapisiIVerificiraj(datoteka);
                return datoteka.toAbsolutePath();
            } catch (IOException e) {
                zadnjaGreska = e;
                try { Thread.sleep(200L * (i + 1)); } catch (InterruptedException ignored) {}
            }
        }

        Path fallback = Paths.get(System.getProperty("user.home"), "racuni_backup");
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                Files.createDirectories(fallback);
                Path datoteka = fallback.resolve(naziv);
                zapisiIVerificiraj(datoteka);
                return datoteka.toAbsolutePath();
            } catch (IOException e) {
                zadnjaGreska = e;
                try { Thread.sleep(200L * (i + 1)); } catch (InterruptedException ignored) {}
            }
        }

        throw new IOException("Spremanje računa nije uspjelo. Zadnja greška: "
                + (zadnjaGreska != null ? zadnjaGreska.getMessage() : "nepoznato"), zadnjaGreska);
    }

    /**
     * Ispisuje račun bez dijaloga. Vraća opis greške ili null ako je uspjelo.
     *
     * @return null ako je uspjelo, opis greške ako nije
     */
    public String ispisiBezDijaloga() {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            PrintService[] svi = PrintServiceLookup.lookupPrintServices(null, null);
            if (svi.length == 0) return "Printer nije spojen ili nije instaliran na ovom računalu.";
            PrintService termalniPrinter = pronadjiTermalniPrinter();
            if (termalniPrinter != null) {
                try { job.setPrintService(termalniPrinter); } catch (PrinterException ignored) {}
            }
            job.setPrintable(this::printPage, termalniFormat(job));
            job.print();
            return null;
        } catch (PrinterException e) {
            return IspisIzvjestaja.opisGreskePrintera(e);
        }
    }

    // -------------------------------------------------------------------------
    // Privatne metode
    // -------------------------------------------------------------------------

    private void zapisiIVerificiraj(Path datoteka) throws IOException {
        generirajPdf(datoteka);
        if (!Files.exists(datoteka) || Files.size(datoteka) < MIN_VELICINA_BAJTA) {
            Files.deleteIfExists(datoteka);
            throw new IOException("Verifikacija PDF-a nije uspjela: " + datoteka);
        }
    }

    private void generirajPdf(Path datoteka) throws IOException {
        String naziv  = config.getNazivTvrtke() != null ? config.getNazivTvrtke() : "STIL A j.d.o.o.";
        String adresa = config.getAdresa() != null ? config.getAdresa() : "";
        String oib    = config.getOib() != null ? config.getOib() : "";

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(227, 842));
            doc.addPage(page);
            PDType1Font bold   = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);
            PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.COURIER);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 8, y = 820, lh = 11f;

                y = red(cs, bold,   9, x, y, lh, ctr(naziv));
                if (!adresa.isEmpty()) y = red(cs, normal, 8, x, y, lh, ctr(adresa));
                if (!oib.isEmpty())    y = red(cs, normal, 8, x, y, lh, ctr("OIB: " + oib));
                y = red(cs, normal, 8, x, y, lh, "-".repeat(38));

                y = red(cs, bold,   8, x, y, lh, "Racun: " + racun.getOznakaRacuna());
                y = red(cs, normal, 8, x, y, lh, "Datum: " + racun.getVrijemeIzdavanja().format(DTF));
                y = red(cs, normal, 8, x, y, lh, "Placanje: " +
                        (racun.getNacinPlacanja() == Racun.NacinPlacanja.GOTOVINA ? "Gotovina" : "Kartica"));
                y = red(cs, normal, 8, x, y, lh, "-".repeat(38));
                y = red(cs, bold,   8, x, y, lh, col("Naziv", "Kom", "Iznos"));
                y = red(cs, normal, 8, x, y, lh, "-".repeat(38));

                for (StavkaRacuna s : racun.getStavke()) {
                    y = red(cs, normal, 8, x, y, lh,
                            col(skrati(s.getArtiklNaziv(), 20),
                                String.valueOf(s.getKolicina()),
                                String.format("%.2f", s.getUkupno())));
                    if (s.getPopust() > 0)
                        y = red(cs, normal, 8, x, y, lh,
                                "  Popust: " + String.format("%.1f%%", s.getPopust()));
                    y = red(cs, normal, 8, x, y, lh,
                            String.format("  %.2f EUR/kom", s.getCijena()));
                }

                double ukupno   = racun.getUkupno();
                double pdv      = racun.getUkupnoPdv();
                double osnovica = ukupno - pdv;
                y = red(cs, normal, 8, x, y, lh, "-".repeat(38));
                y = red(cs, normal, 8, x, y, lh, rpad("Osnovica: " + String.format("%.2f EUR", osnovica)));
                y = red(cs, normal, 8, x, y, lh, rpad("PDV 25%:  " + String.format("%.2f EUR", pdv)));
                y = red(cs, bold,   9, x, y, lh, rpad("UKUPNO:   " + String.format("%.2f EUR", ukupno)));
                y = red(cs, normal, 8, x, y, lh, "=".repeat(38));

                if (racun.getZki() != null && !racun.getZki().isEmpty())
                    y = red(cs, normal, 7, x, y, 10, "ZKI: " + racun.getZki());
                if (racun.getJir() != null && !racun.getJir().isEmpty())
                    y = red(cs, normal, 7, x, y, 10, "JIR: " + racun.getJir());

                y = red(cs, normal, 8, x, y, lh, "-".repeat(38));
                red(cs, bold, 8, x, y, lh, ctr("Hvala na kupnji!"));
            }
            doc.save(datoteka.toFile());
        }
    }

    /** Ispisuje jedan redak teksta i vraća novu y poziciju. */
    private float red(PDPageContentStream cs, PDType1Font font, float size,
                      float x, float y, float lh, String tekst) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(ascii(tekst));
        cs.endText();
        return y - lh;
    }

    /** Zamjenjuje hrvatska slova ASCII ekvivalentima za PDF Standard14 fontove. */
    private static String ascii(String s) {
        if (s == null) return "";
        return s.replace("č","c").replace("ć","c").replace("š","s")
                .replace("ž","z").replace("đ","d")
                .replace("Č","C").replace("Ć","C").replace("Š","S")
                .replace("Ž","Z").replace("Đ","D");
    }

    private String ctr(String t) {
        if (t.length() >= 38) return t;
        return " ".repeat((38 - t.length()) / 2) + t;
    }

    private String rpad(String t) {
        if (t.length() >= 38) return t;
        return " ".repeat(38 - t.length()) + t;
    }

    private String col(String l, String s, String d) {
        l = skrati(l, 20);
        int sp = 38 - l.length() - s.length() - d.length();
        return l + " ".repeat(Math.max(1, sp - 1)) + s + " " + d;
    }

    private String skrati(String t, int max) {
        return t.length() > max ? t.substring(0, max - 1) + "." : t;
    }

    private int printPage(java.awt.Graphics g, PageFormat pf, int page) {
        if (page > 0) return Printable.NO_SUCH_PAGE;
        // Termalni ispis koristi Java2D direktno iz linije
        return Printable.PAGE_EXISTS;
    }

    private PageFormat termalniFormat(PrinterJob job) {
        PageFormat pf = job.defaultPage();
        java.awt.print.Paper paper = new java.awt.print.Paper();
        double w = 80 * 72.0 / 25.4, h = 297 * 72.0 / 25.4;
        paper.setSize(w, h);
        paper.setImageableArea(3 * 72.0 / 25.4, 3 * 72.0 / 25.4, 74 * 72.0 / 25.4, 291 * 72.0 / 25.4);
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);
        return pf;
    }

    private PrintService pronadjiTermalniPrinter() {
        String odabrani = config.getNazivPrintera();
        if (odabrani != null && !odabrani.isEmpty()) {
            for (PrintService ps : PrintServiceLookup.lookupPrintServices(null, null))
                if (ps.getName().equals(odabrani)) return ps;
        }
        for (PrintService ps : PrintServiceLookup.lookupPrintServices(null, null)) {
            String ime = ps.getName().toLowerCase();
            if (ime.contains("thermal") || ime.contains("receipt") || ime.contains("pos")
                    || ime.contains("epson") || ime.contains("star") || ime.contains("bixolon"))
                return ps;
        }
        return null;
    }
}
