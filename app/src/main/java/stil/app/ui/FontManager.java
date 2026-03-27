package stil.app.ui;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.Enumeration;
/**
 * Centralni upravitelj veličinom fonta za cijelu aplikaciju.
 *
 * Postavlja font na sve UIManager ključeve i rekurzivno ažurira
 * sve žive komponente u prozoru, uključujući JTable rowHeight.
 * Koristi se za inicijalno postavljanje i za Ctrl+scroll zoom.
 */
public class FontManager {

    public static final float MIN = 10f;
    public static final float MAX = 48f;
    private static volatile float trenutna = 20f;

    /**
     * Vraća trenutnu veličinu fonta.
     *
     * @return veličina u točkama
     */
    public static float getTrenutna() { return trenutna; }

    /**
     * Postavlja novu veličinu fonta i primjenjuje je globalno.
     * Ažurira UIManager defaults i sve komponente u zadanom prozoru.
     *
     * @param velicina nova veličina fonta
     * @param prozor   prozor čije komponente treba ažurirati
     */
    public static void postavi(float velicina, Window prozor) {
        trenutna = Math.max(MIN, Math.min(MAX, velicina));
        azurirajUIManager(trenutna);
        if (prozor != null) azurirajKomponente(prozor, trenutna);
    }

    /** Ažurira sve .font ključeve u UIManager defaults. */
    private static void azurirajUIManager(float velicina) {
        Enumeration<Object> keys = UIManager.getLookAndFeelDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key.toString().endsWith(".font")) {
                Font f = UIManager.getFont(key);
                if (f != null) UIManager.put(key, f.deriveFont(velicina));
            }
        }
    }

    /**
     * Rekurzivno prolazi sve komponente i postavlja font + rowHeight za JTable.
     *
     * @param comp     komponenta (i sve njene djeca)
     * @param velicina nova veličina fonta
     */
    public static void azurirajKomponente(Component comp, float velicina) {
        if (comp instanceof JTable table) {
            Font f = table.getFont();
            if (f != null) table.setFont(f.deriveFont(velicina));
            // rowHeight = font visina + padding
            table.setRowHeight((int)(velicina * 1.8f));
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                Font hf = header.getFont();
                if (hf != null) header.setFont(hf.deriveFont(velicina));
            }
        } else if (comp instanceof JComponent jc) {
            Font f = jc.getFont();
            if (f != null) jc.setFont(f.deriveFont(velicina));
        }

        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                azurirajKomponente(child, velicina);
            }
        }

        // Dijalozi i popup meniji koji nisu u stablu
        if (comp instanceof Window w) {
            for (Window owned : w.getOwnedWindows()) {
                azurirajKomponente(owned, velicina);
            }
        }
    }
}
