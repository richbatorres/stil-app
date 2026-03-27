package stil.app;

import stil.app.db.DatabaseManager;
import stil.app.db.TestDataGenerator;
import stil.app.ui.FontManager;
import stil.app.ui.MainWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Ulazna točka aplikacije STIL A j.d.o.o. blagajne.
 * Inicijalizira Swing EDT i pokreće glavni prozor.
 * Pri prvom pokretanju (prazna baza) generira testne podatke.
 * Podržava globalni zoom putem Ctrl+scroll miša.
 */
public class App {

    public static void main(String[] args) {
        try {
            TestDataGenerator.generirajAkoJePrazno(DatabaseManager.getInstance());
        } catch (Exception e) {
            System.err.println("Upozorenje: generiranje testnih podataka nije uspjelo: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            // Inicijalni font — primijeni na UIManager prije kreiranja prozora
            FontManager.postavi(FontManager.getTrenutna(), null);

            MainWindow prozor = new MainWindow();

            // Primijeni font na sve komponente koje su već kreirane
            FontManager.postavi(FontManager.getTrenutna(), prozor);

            // Ctrl+scroll zoom
            Toolkit.getDefaultToolkit().addAWTEventListener((AWTEventListener) event -> {
                MouseWheelEvent mwe = (MouseWheelEvent) event;
                if ((mwe.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                    float nova = FontManager.getTrenutna() - mwe.getWheelRotation() * 1.5f;
                    FontManager.postavi(nova, prozor);
                }
            }, AWTEvent.MOUSE_WHEEL_EVENT_MASK);

            prozor.setVisible(true);
        });
    }
}
