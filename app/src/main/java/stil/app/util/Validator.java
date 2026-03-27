package stil.app.util;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Pattern;

/**
 * Pomoćna klasa s metodama za validaciju korisničkih unosa u formama.
 *
 * Sve metode prikazuju JOptionPane upozorenje ako validacija ne prođe
 * i vraćaju null/false kako bi pozivatelj mogao prekinuti pohranу.
 * Centralizirana validacija sprječava dupliciranje logike po formama.
 */
public class Validator {

    // U headless modu (testovi) ne prikazujemo dijaloge
    private static void upozori(Component parent, String poruka) {
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(parent, poruka, "Neispravan unos", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$");
    private static final Pattern OIB_PATTERN =
        Pattern.compile("^\\d{11}$");

    /**
     * Parsira decimalni broj iz tekst polja. Prihvaća zarez i točku kao decimalni separator.
     * Prikazuje upozorenje i vraća null ako unos nije validan broj ili je negativan (kad se zabrani).
     *
     * @param parent      roditeljska komponenta za JOptionPane
     * @param tekst       tekst za parsiranje
     * @param naziv       naziv polja za poruku greške (npr. "Cijena")
     * @param dozvoli0    true ako je 0 dozvoljena vrijednost
     * @param dozvoliNeg  true ako su negativne vrijednosti dozvoljene
     * @return parsirani Double ili null ako validacija nije prošla
     */
    public static Double parseDecimal(Component parent, String tekst, String naziv,
                                      boolean dozvoli0, boolean dozvoliNeg) {
        String s = tekst == null ? "" : tekst.trim().replace(",", ".");
        if (s.isEmpty()) {
            upozori(parent, naziv + " je obavezno polje.");
            return null;
        }
        double v;
        try {
            v = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            upozori(parent, naziv + ": \"" + tekst.trim() + "\" nije ispravan broj.\nPrimjer ispravnog unosa: 12.50");
            return null;
        }
        if (!dozvoliNeg && v < 0) {
            upozori(parent, naziv + " ne može biti negativan.");
            return null;
        }
        if (!dozvoli0 && v == 0) {
            upozori(parent, naziv + " mora biti veći od nule.");
            return null;
        }
        return v;
    }

    /**
     * Parsira cijeli broj iz tekst polja.
     * Prikazuje upozorenje i vraća null ako unos nije validan cijeli broj ili krši ograničenja.
     *
     * @param parent   roditeljska komponenta
     * @param tekst    tekst za parsiranje
     * @param naziv    naziv polja za poruku greške
     * @param min      minimalna dozvoljena vrijednost (uključivo)
     * @return parsirani Integer ili null ako validacija nije prošla
     */
    public static Integer parseInt(Component parent, String tekst, String naziv, int min) {
        String s = tekst == null ? "" : tekst.trim();
        if (s.isEmpty()) {
            upozori(parent, naziv + " je obavezno polje.");
            return null;
        }
        int v;
        try {
            v = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            upozori(parent, naziv + ": \"" + s + "\" nije ispravan cijeli broj.\nPrimjer ispravnog unosa: 5");
            return null;
        }
        if (v < min) {
            upozori(parent, naziv + " mora biti najmanje " + min + ".");
            return null;
        }
        return v;
    }

    /**
     * Provjerava je li OIB ispravan (11 znamenki).
     * Prikazuje upozorenje i vraća false ako nije ispravan.
     * Prazni OIB se smatra ispravnim (OIB je opcionalan za strane dobavljače).
     *
     * @param parent roditeljska komponenta
     * @param oib    OIB za provjeru (može biti null ili prazan)
     * @return true ako je OIB ispravan ili prazan
     */
    public static boolean validateOib(Component parent, String oib) {
        if (oib == null || oib.trim().isEmpty()) return true; // opcionalno
        if (!OIB_PATTERN.matcher(oib.trim()).matches()) {
            upozori(parent, "OIB mora sadržavati točno 11 znamenki.\nUneseno: \"" + oib.trim() + "\"");
            return false;
        }
        return true;
    }

    /**
     * Provjerava je li email adresa ispravnog formata.
     * Prazni email se smatra ispravnim (email je opcionalan).
     *
     * @param parent roditeljska komponenta
     * @param email  email za provjeru
     * @return true ako je email ispravan ili prazan
     */
    public static boolean validateEmail(Component parent, String email) {
        if (email == null || email.trim().isEmpty()) return true;
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            upozori(parent, "Email adresa nije ispravnog formata.\nUneseno: \"" + email.trim() + "\"\nPrimjer: ime@tvrtka.hr");
            return false;
        }
        return true;
    }

    /**
     * Provjerava je li obavezno tekstualno polje popunjeno.
     *
     * @param parent roditeljska komponenta
     * @param tekst  tekst za provjeru
     * @param naziv  naziv polja za poruku greške
     * @return true ako polje nije prazno
     */
    public static boolean requireNonEmpty(Component parent, String tekst, String naziv) {
        if (tekst == null || tekst.trim().isEmpty()) {
            upozori(parent, naziv + " je obavezno polje.");
            return false;
        }
        return true;
    }
}
