package stil.app;

import stil.app.db.DatabaseManager;
import stil.app.db.TestDataGenerator;
import stil.app.ui.MainWindow;

import javax.swing.*;

/**
 * Ulazna točka aplikacije STIL A j.d.o.o. blagajne.
 * Inicijalizira Swing EDT i pokreće glavni prozor.
 * Pri prvom pokretanju (prazna baza) generira testne podatke.
 */
public class App {
    public static void main(String[] args) {
        // Generiraj testne podatke ako je baza prazna (van EDT-a)
        try {
            TestDataGenerator.generirajAkoJePrazno(DatabaseManager.getInstance());
        } catch (Exception e) {
            System.err.println("Upozorenje: generiranje testnih podataka nije uspjelo: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new MainWindow().setVisible(true);
        });
    }
}
