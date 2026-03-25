package stil.app.ui;

import stil.app.db.DatabaseManager;
import stil.app.model.Config;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;

/**
 * Ekran za zaključavanje blagajne s unosom PIN-a.
 *
 * Prikazuje se modalni, nedekoriran dialog tamne pozadine.
 * Ako PIN nije postavljen u konfiguraciji, odmah prolazi bez unosa.
 * Ako korisnik zatvori dialog bez ispravnog PIN-a, {@link #isUnlocked()} vraća false
 * i pozivatelj ({@link MainWindow}) gasi aplikaciju.
 *
 * Podržava unos PIN-a tipkovnicom (Enter potvrda) i klikom na gumb.
 */
public class LockScreen extends JDialog {

    private final JPasswordField pinField = new JPasswordField(10);
    private final JLabel poruka = new JLabel(" ");
    private boolean unlocked = false;

    public LockScreen(Frame parent) {
        super(parent, "Blagajna zaključana", true);
        setUndecorated(true);
        setSize(320, 220);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
            BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));
        panel.setBackground(new Color(45, 45, 45));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        JLabel naslov = new JLabel("🔒 Blagajna zaključana", SwingConstants.CENTER);
        naslov.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        naslov.setForeground(Color.WHITE);

        JLabel pinLabel = new JLabel("Unesite PIN:", SwingConstants.CENTER);
        pinLabel.setForeground(Color.LIGHT_GRAY);

        pinField.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        pinField.setHorizontalAlignment(SwingConstants.CENTER);

        poruka.setForeground(new Color(255, 80, 80));
        poruka.setHorizontalAlignment(SwingConstants.CENTER);

        JButton otključajBtn = new JButton("Otključaj");
        otključajBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        panel.add(naslov, gbc);
        panel.add(pinLabel, gbc);
        panel.add(pinField, gbc);
        panel.add(poruka, gbc);
        panel.add(otključajBtn, gbc);

        add(panel);

        otključajBtn.addActionListener(e -> provjeriPin());
        pinField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) provjeriPin();
            }
        });

        SwingUtilities.invokeLater(pinField::requestFocusInWindow);
    }

    private void provjeriPin() {
        String uneseni = new String(pinField.getPassword());
        try {
            Config config = DatabaseManager.getInstance().loadConfig();
            String pin = config.getPin();
            if (pin == null || pin.isEmpty() || pin.equals(uneseni)) {
                unlocked = true;
                dispose();
            } else {
                poruka.setText("Pogrešan PIN!");
                pinField.setText("");
                pinField.requestFocusInWindow();
            }
        } catch (SQLException e) {
            poruka.setText("Greška baze podataka.");
        }
    }

    public boolean isUnlocked() { return unlocked; }
}
