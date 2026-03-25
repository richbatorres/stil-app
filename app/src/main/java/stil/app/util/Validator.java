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
            JOptionPane.showMessageDialog(parent,
                naziv + " je obavezno polje.", "Neispravan unos", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        double v;
        try {
            v = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(parent,
                naziv + ": \"" + tekst.trim() + "\" nije ispravan broj.\nPrimjer ispravnog unosa: 12.50",
                "Neispravan unos", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        if (!dozvoliNeg && v < 0) {
            JOptionPane.showMessageDialog(parent,
                naziv + " ne može biti negativan.", "Neispravan unos", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        if (!dozvoli0 && v == 0) {
            JOptionPane.showMessageDialog(parent,
                naziv + " mora biti veći od nule.", "Neispravan unos", JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(parent,
                naziv + " je obavezno polje.", "Neispravan unos", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int v;
        try {
            v = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(parent,
                naziv + ": \"" + s + "\" nije ispravan cijeli broj.\nPrimjer ispravnog unosa: 5",
                "Neispravan unos", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        if (v < min) {
            JOptionPane.showMessageDialog(parent,
                naziv + " mora biti najmanje " + min + ".", "Neispravan unos", JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(parent,
                "OIB mora sadržavati točno 11 znamenki.\nUneseno: \"" + oib.trim() + "\"",
                "Neispravan unos", JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(parent,
                "Email adresa nije ispravnog formata.\nUneseno: \"" + email.trim() + "\"\nPrimjer: ime@tvrtka.hr",
                "Neispravan unos", JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(parent,
                naziv + " je obavezno polje.", "Neispravan unos", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }
}
