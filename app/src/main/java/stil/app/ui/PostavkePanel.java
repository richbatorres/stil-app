package stil.app.ui;

import stil.app.db.DatabaseManager;
import stil.app.model.Config;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
/**
 * Panel za konfiguraciju aplikacije.
 *
 * Sadrži tri sekcije:
 * - Podaci tvrtke: naziv, OIB, adresa (ispisuju se na računu)
 * - Fiskalizacija: oznaka poslovnog prostora i uređaja, putanja i lozinka PKCS12 certifikata,
 *   toggle između testnog i produkcijskog okruženja Fine
 * - Zaključavanje blagajne: postavljanje PIN-a (dvostruki unos za potvrdu)
 *
 * Sve postavke se pohranjuju u SQLite tablicu {@code config} kao key-value parovi.
 * Ovaj panel je prikazan kao tab "Postavke" u {@link MainWindow}.
 */
public class PostavkePanel extends JPanel {

    private final JTextField nazivField = new JTextField(25);
    private final JTextField oibField = new JTextField(25);
    private final JTextField adresaField = new JTextField(25);
    private final JTextField posProstorField = new JTextField(25);
    private final JTextField uredajField = new JTextField(25);
    private final JTextField certifikatField = new JTextField(25);
    private final JPasswordField certLozinkaField = new JPasswordField(25);
    private final JPasswordField pinField = new JPasswordField(25);
    private final JPasswordField pinPotvrda = new JPasswordField(25);
    private final JCheckBox fiskTestModeCheck = new JCheckBox("Koristi testno okruženje (cistest.apis-it.hr)");
    private final JComboBox<String> printerCombo = new JComboBox<>();

    public PostavkePanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        addSection(form, gbc, 0, "— Podaci tvrtke —");
        addRow(form, gbc, 1, "Naziv tvrtke:", nazivField);
        addRow(form, gbc, 2, "OIB:", oibField);
        addRow(form, gbc, 3, "Adresa:", adresaField);

        addSection(form, gbc, 4, "— Fiskalizacija —");
        addRow(form, gbc, 5, "Oznaka posl. prostora:", posProstorField);
        addRow(form, gbc, 6, "Oznaka uređaja:", uredajField);
        addRow(form, gbc, 7, "Putanja certifikata (.p12):", certifikatField);
        addRow(form, gbc, 8, "Lozinka certifikata:", certLozinkaField);
        addRow(form, gbc, 9, "Fiskalizacija:", fiskTestModeCheck);

        addSection(form, gbc, 10, "— Printer —");
        ucitajPrintere();
        addRow(form, gbc, 11, "Printer za račune:", printerCombo);

        addSection(form, gbc, 12, "— Zaključavanje blagajne —");
        addRow(form, gbc, 13, "Novi PIN:", pinField);
        addRow(form, gbc, 14, "Potvrdi PIN:", pinPotvrda);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        JButton spremiBtn = new JButton("Spremi postavke");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(spremiBtn);
        add(bottom, BorderLayout.SOUTH);

        spremiBtn.addActionListener(e -> spremi());
        ucitaj();
    }

    private void addSection(JPanel panel, GridBagConstraints gbc, int row, String text) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel label = new JLabel(text);
        label.setForeground(Color.GRAY);
        panel.add(label, gbc);
        gbc.gridwidth = 1;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private void ucitajPrintere() {
        printerCombo.removeAllItems();
        printerCombo.addItem("(auto-detekcija)");
        for (PrintService ps : PrintServiceLookup.lookupPrintServices(null, null))
            printerCombo.addItem(ps.getName());
    }

    private void ucitaj() {
        try {
            Config c = DatabaseManager.getInstance().loadConfig();
            nazivField.setText(c.getNazivTvrtke());
            oibField.setText(c.getOib());
            adresaField.setText(c.getAdresa());
            posProstorField.setText(c.getOznakaPosProstora());
            uredajField.setText(c.getOznakaUredaja());
            certifikatField.setText(c.getPutanjaCertifikata());
            fiskTestModeCheck.setSelected(c.isFiskTestMode());
            // Odabir printera
            String sp = c.getNazivPrintera();
            if (sp != null && !sp.isEmpty()) printerCombo.setSelectedItem(sp);
            else printerCombo.setSelectedIndex(0);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška pri učitavanju postavki: " + e.getMessage());
        }
    }

    private void spremi() {
        String pin = new String(pinField.getPassword());
        String potvrda = new String(pinPotvrda.getPassword());

        if (!pin.isEmpty() && !pin.equals(potvrda)) {
            JOptionPane.showMessageDialog(this, "PIN i potvrda PINa se ne podudaraju.");
            return;
        }

        try {
            Config c = DatabaseManager.getInstance().loadConfig();
            c.setNazivTvrtke(nazivField.getText().trim());
            c.setOib(oibField.getText().trim());
            c.setAdresa(adresaField.getText().trim());
            c.setOznakaPosProstora(posProstorField.getText().trim());
            c.setOznakaUredaja(uredajField.getText().trim());
            c.setPutanjaCertifikata(certifikatField.getText().trim());
            if (!new String(certLozinkaField.getPassword()).isEmpty())
                c.setLozinkaCertifikata(new String(certLozinkaField.getPassword()));
            c.setFiskTestMode(fiskTestModeCheck.isSelected());
            // Printer
            String odabrani = (String) printerCombo.getSelectedItem();
            c.setNazivPrintera("(auto-detekcija)".equals(odabrani) ? null : odabrani);
            if (!pin.isEmpty())
                c.setPin(pin);

            DatabaseManager.getInstance().saveConfig(c);
            JOptionPane.showMessageDialog(this, "Postavke su spremljene.");
            pinField.setText("");
            pinPotvrda.setText("");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška pri spremanju: " + e.getMessage());
        }
    }
}
