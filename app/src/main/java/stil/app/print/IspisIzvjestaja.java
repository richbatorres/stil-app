package stil.app.print;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import stil.app.model.IzvjestajPodaci;
import stil.app.model.Zaklucnica;

import javax.print.*;
import java.awt.print.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generira PDF datoteke i ispisuje izvještaje i zaključnice.
 *
 * PDF se generira pomoću Apache PDFBox 3.x bez vanjskih fontova.
 * Ispis koristi Java Print API s automatskom detekcijom printera.
 * Ista logika robusnog spremanja kao IspisRacuna: retry + fallback + verifikacija.
 */
public class IspisIzvjestaja {

    private static final DateTimeFormatter DTF_NAZIV = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DTF_PRIKAZ = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE_PRIKAZ = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int MAX_RETRY = 3;
    private static final long MIN_VELICINA_BAJTA = 200;

    /**
     * Sprema zaključnicu kao PDF s retry/fallback mehanizmom.
     *
     * @param z          zaključnica za spremanje
     * @param direktorij primarni direktorij (npr. "zaklucnice")
     * @return apsolutna putanja do PDF datoteke
     * @throws IOException ako ni primarni ni fallback ne rade
     */
    public static Path spremiZaklucnicuPdf(Zaklucnica z, String direktorij) throws IOException {
        String naziv = "zaklucnica_" + z.getVrijemeGeneriranja().format(DTF_NAZIV) + ".pdf";
        return spremiPdf(naziv, direktorij, "zaklucnice_backup", doc -> popuniZaklucnicu(doc, z));
    }

    /**
     * Sprema izvještaj kao PDF s retry/fallback mehanizmom.
     *
     * @param p          podaci izvještaja
     * @param od         početak razdoblja
     * @param do_        kraj razdoblja
     * @param direktorij primarni direktorij (npr. "izvjestaji")
     * @return apsolutna putanja do PDF datoteke
     * @throws IOException ako ni primarni ni fallback ne rade
     */
    public static Path spremiIzvjestajPdf(IzvjestajPodaci p, LocalDate od, LocalDate do_,
                                           String direktorij) throws IOException {
        String naziv = "izvjestaj_" + od.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "_" + do_.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
        return spremiPdf(naziv, direktorij, "izvjestaji_backup",
                doc -> popuniIzvjestaj(doc, p, od, do_));
    }

    /** Ispisuje PDF datoteku na printer bez dijaloga. Vraća opis greške ili null ako je uspjelo. */
    public static String ispisiPdf(Path pdfPutanja) {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            PrintService ps = pronadjiPrinter();
            if (ps == null) {
                PrintService[] svi = PrintServiceLookup.lookupPrintServices(null, null);
                if (svi.length == 0) return "Nije pronađen niti jedan printer na sustavu.";
                ps = svi[0];
            }
            job.setPrintService(ps);
            // Ispis PDF-a kao slike putem PDFBox
            try (PDDocument doc = Loader.loadPDF(pdfPutanja.toFile())) {
                job.setPrintable((g, pf, page) -> {
                    if (page >= doc.getNumberOfPages()) return Printable.NO_SUCH_PAGE;
                    try {
                        var renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
                        var img = renderer.renderImageWithDPI(page, 150);
                        g.drawImage(img, (int) pf.getImageableX(), (int) pf.getImageableY(),
                                (int) pf.getImageableWidth(), (int) pf.getImageableHeight(), null);
                    } catch (IOException ex) {
                        return Printable.NO_SUCH_PAGE;
                    }
                    return Printable.PAGE_EXISTS;
                });
                job.print();
            }
            return null; // uspjeh
        } catch (PrinterException e) {
            return opisGreskePrintera(e);
        } catch (Exception e) {
            return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }

    // -------------------------------------------------------------------------
    // Privatne metode
    // -------------------------------------------------------------------------

    @FunctionalInterface
    private interface PdfPopunjivac {
        void popuni(PDDocument doc) throws IOException;
    }

    private static Path spremiPdf(String naziv, String direktorij, String fallbackNaziv,
                                   PdfPopunjivac popunjivac) throws IOException {
        IOException zadnjaGreska = null;

        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                Path dir = Paths.get(direktorij);
                Files.createDirectories(dir);
                Path datoteka = dir.resolve(naziv);
                zapisiIVerificiraj(datoteka, popunjivac);
                return datoteka.toAbsolutePath();
            } catch (IOException e) {
                zadnjaGreska = e;
                try { Thread.sleep(200L * (i + 1)); } catch (InterruptedException ignored) {}
            }
        }

        Path fallback = Paths.get(System.getProperty("user.home"), fallbackNaziv);
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                Files.createDirectories(fallback);
                Path datoteka = fallback.resolve(naziv);
                zapisiIVerificiraj(datoteka, popunjivac);
                return datoteka.toAbsolutePath();
            } catch (IOException e) {
                zadnjaGreska = e;
                try { Thread.sleep(200L * (i + 1)); } catch (InterruptedException ignored) {}
            }
        }

        throw new IOException("Spremanje PDF-a nije uspjelo. Zadnja greška: "
                + (zadnjaGreska != null ? zadnjaGreska.getMessage() : "nepoznato"), zadnjaGreska);
    }

    private static void zapisiIVerificiraj(Path datoteka, PdfPopunjivac popunjivac) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            popunjivac.popuni(doc);
            doc.save(datoteka.toFile());
        }
        if (!Files.exists(datoteka) || Files.size(datoteka) < MIN_VELICINA_BAJTA) {
            Files.deleteIfExists(datoteka);
            throw new IOException("Verifikacija PDF-a nije uspjela: " + datoteka);
        }
    }

    private static void popuniZaklucnicu(PDDocument doc, Zaklucnica z) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        var bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        var normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float y = 780;
            y = pisiBold(cs, bold, 16, 50, y, "DNEVNA ZAKLJU\u010cNICA BLAGAJNE");
            y -= 6;
            y = pisiNormal(cs, normal, 10, 50, y,
                    "Generirano: " + z.getVrijemeGeneriranja().format(DTF_PRIKAZ));
            y = pisiNormal(cs, normal, 10, 50, y,
                    "Radni dan: " + z.getDanOd().format(DATE_PRIKAZ));
            y -= 10;
            y = linija(cs, y);
            y -= 6;
            y = pisiBold(cs, bold, 12, 50, y, "PRODAJA");
            y -= 4;
            y = pisiRed(cs, normal, bold, 50, y, "Broj izdanih ra\u010duna:", String.valueOf(z.getBrojRacuna()));
            y = pisiRed(cs, normal, bold, 50, y, "Ukupna prodaja:", String.format("%.2f EUR", z.getUkupnoEur()));
            y -= 6;
            y = pisiRed(cs, normal, normal, 50, y, "  od toga gotovina:", String.format("%.2f EUR", z.getGotovinaEur()));
            y = pisiRed(cs, normal, normal, 50, y, "  od toga kartica:", String.format("%.2f EUR", z.getKarticaEur()));
            y -= 10;
            y = linija(cs, y);
            y -= 6;
            pisiNormal(cs, normal, 9, 50, y, "Dokument generiran automatski — STIL A j.d.o.o. blagajnički sustav");
        }
    }

    private static void popuniIzvjestaj(PDDocument doc, IzvjestajPodaci p,
                                         LocalDate od, LocalDate do_) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        var bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        var normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float y = 780;
            y = pisiBold(cs, bold, 16, 50, y, "IZVJE\u0160TAJ O POSLOVANJU");
            y -= 4;
            y = pisiNormal(cs, normal, 10, 50, y,
                    "Razdoblje: " + od.format(DATE_PRIKAZ) + " \u2014 " + do_.format(DATE_PRIKAZ));
            y -= 10;
            y = linija(cs, y);
            y -= 6;

            y = pisiBold(cs, bold, 12, 50, y, "PRODAJA");
            y -= 4;
            y = pisiRed(cs, normal, normal, 50, y, "Broj ra\u010duna:", String.valueOf(p.getBrojRacuna()));
            y = pisiRed(cs, normal, bold, 50, y, "Ukupna prodaja:", String.format("%.2f EUR", p.getUkupnaProdaja()));
            y = pisiRed(cs, normal, normal, 50, y, "  od toga PDV:", String.format("%.2f EUR", p.getUkupniPdvProdaja()));
            y = pisiRed(cs, normal, normal, 50, y, "  gotovina:", String.format("%.2f EUR", p.getProdajaGotovina()));
            y = pisiRed(cs, normal, normal, 50, y, "  kartica:", String.format("%.2f EUR", p.getProdajaKartica()));
            y -= 8;

            y = pisiBold(cs, bold, 12, 50, y, "NABAVA");
            y -= 4;
            y = pisiRed(cs, normal, normal, 50, y, "Broj nabava:", String.valueOf(p.getBrojNabava()));
            y = pisiRed(cs, normal, normal, 50, y, "Ukupna nabava:", String.format("%.2f EUR", p.getUkupnaNabava()));
            y -= 8;

            y = pisiBold(cs, bold, 12, 50, y, "POVRATI");
            y -= 4;
            y = pisiRed(cs, normal, normal, 50, y, "Od kupca:",
                    p.getBrojPovratOdKupca() + " kom / " + String.format("%.2f EUR", p.getIznosPovratOdKupca()));
            y = pisiRed(cs, normal, normal, 50, y, "Dobavlja\u010du:",
                    p.getBrojPovratDobavljacu() + " kom / " + String.format("%.2f EUR", p.getIznosPovratDobavljacu()));
            y -= 8;

            y = pisiBold(cs, bold, 12, 50, y, "REZULTAT");
            y -= 4;
            y = pisiRed(cs, normal, bold, 50, y, "Neto promet:", String.format("%.2f EUR", p.getNetoPromet()));
            y = pisiRed(cs, normal, bold, 50, y, "Bruto mar\u017ea:", String.format("%.2f EUR", p.getBrutoMarza()));
            y -= 10;
            linija(cs, y);
        }
    }

    // --- PDF helper metode ---

    /** Zamjenjuje hrvatska slova ASCII ekvivalentima za PDF Standard14 fontove. */
    private static String ascii(String s) {
        if (s == null) return "";
        return s.replace("č", "c").replace("ć", "c").replace("š", "s")
                .replace("ž", "z").replace("đ", "d")
                .replace("Č", "C").replace("Ć", "C").replace("Š", "S")
                .replace("Ž", "Z").replace("Đ", "D");
    }

    private static float pisiBold(PDPageContentStream cs, PDType1Font font, float size,
                                   float x, float y, String tekst) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(ascii(tekst));
        cs.endText();
        return y - size - 4;
    }

    private static float pisiNormal(PDPageContentStream cs, PDType1Font font, float size,
                                     float x, float y, String tekst) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(ascii(tekst));
        cs.endText();
        return y - size - 3;
    }

    private static float pisiRed(PDPageContentStream cs, PDType1Font labelFont, PDType1Font vrijednostFont,
                                  float x, float y, String label, String vrijednost) throws IOException {
        cs.beginText();
        cs.setFont(labelFont, 11);
        cs.newLineAtOffset(x, y);
        cs.showText(ascii(label));
        cs.endText();
        cs.beginText();
        cs.setFont(vrijednostFont, 11);
        cs.newLineAtOffset(350, y);
        cs.showText(ascii(vrijednost));
        cs.endText();
        return y - 16;
    }

    private static float linija(PDPageContentStream cs, float y) throws IOException {
        cs.moveTo(50, y);
        cs.lineTo(545, y);
        cs.stroke();
        return y - 4;
    }

    private static PrintService pronadjiPrinter() {
        for (PrintService ps : PrintServiceLookup.lookupPrintServices(null, null)) {
            String ime = ps.getName().toLowerCase();
            if (ime.contains("thermal") || ime.contains("receipt") || ime.contains("pos")
                    || ime.contains("epson") || ime.contains("star") || ime.contains("bixolon"))
                return ps;
        }
        return null;
    }

    /**
     * Vraća čitljiv opis greške printera umjesto null ili generičke poruke.
     *
     * @param e PrinterException
     * @return opis greške na hrvatskom
     */
    public static String opisGreskePrintera(PrinterException e) {
        if (e == null) return "Nepoznata greška printera.";
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            PrintService[] svi = PrintServiceLookup.lookupPrintServices(null, null);
            if (svi.length == 0) return "Printer nije spojen ili nije instaliran na ovom računalu.";
            return "Printer nije dostupan. Provjerite je li uključen i spojen.";
        }
        if (msg.toLowerCase().contains("no default")) return "Nije postavljen zadani printer.";
        if (msg.toLowerCase().contains("cancel")) return "Ispis je otkazan.";
        return msg;
    }
}
