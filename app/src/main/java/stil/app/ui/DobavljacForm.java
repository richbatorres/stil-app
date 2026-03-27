package stil.app.ui;

import stil.app.db.DatabaseManager;
import stil.app.model.Dobavljac;
import stil.app.util.Validator;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/**
 * JDialog za dodavanje i uređivanje dobavljača.
 *
 * Otvara se iz {@link DobavljaciPanel} klikom na "Novi dobavljač" ili "Uredi".
 * Nakon uspješnog spremanja, {@link #isSaved()} vraća true i pozivatelj
 * treba osvježiti prikaz liste dobavljača.
 *
 * Jedino obavezno polje je naziv dobavljača — sva ostala polja su opcionalna.
 */
public class DobavljacForm extends JDialog {

    private final JTextField nazivField    = new JTextField(25);
    private final JTextField oibField      = new JTextField(25);
    private final JTextField adresaField   = new JTextField(25);
    private final JTextField emailField    = new JTextField(25);
    private final JTextField telefonField  = new JTextField(25);
    private final JTextArea  napomenaArea  = new JTextArea(3, 25);
    private final JCheckBox  komisijaCb    = new JCheckBox("Nudi komisijsku prodaju");

    private boolean saved = false;
    private final Dobavljac dobavljac;

    /**
     * Kreira dialog za dodavanje novog ili uređivanje postojećeg dobavljača.
     *
     * @param parent    roditeljski prozor
     * @param dobavljac postojeći dobavljač za uređivanje, ili null za novog
     */
    public DobavljacForm(Frame parent, Dobavljac dobavljac) {
        super(parent, dobavljac == null ? "Novi dobavljač" : "Uredi dobavljača", true);
        this.dobavljac = dobavljac == null ? new Dobavljac() : dobavljac;

        if (dobavljac != null) {
            nazivField.setText(dobavljac.getNaziv());
            oibField.setText(dobavljac.getOib());
            adresaField.setText(dobavljac.getAdresa());
            emailField.setText(dobavljac.getEmail());
            telefonField.setText(dobavljac.getTelefon());
            napomenaArea.setText(dobavljac.getNapomena());
            komisijaCb.setSelected(dobavljac.isKomisijskiModel());
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        addRow(form, gbc, 0, "Naziv: *", nazivField);
        addRow(form, gbc, 1, "OIB:", oibField);
        addRow(form, gbc, 2, "Adresa:", adresaField);
        addRow(form, gbc, 3, "Email:", emailField);
        addRow(form, gbc, 4, "Telefon:", telefonField);
        addRow(form, gbc, 5, "Napomena:", new JScrollPane(napomenaArea));
        addRow(form, gbc, 6, "", komisijaCb);

        JButton spremiBtn   = new JButton("Spremi");
        JButton odustaniBtn = new JButton("Odustani");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(odustaniBtn);
        buttons.add(spremiBtn);

        spremiBtn.addActionListener(e -> spremi());
        odustaniBtn.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(spremiBtn);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    /** Dodaje redak s labelom i poljem u GridBag formu. */
    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    /** Validira unos, sprema dobavljača u bazu i zatvara dialog. */
    private void spremi() {
        if (!Validator.requireNonEmpty(this, nazivField.getText(), "Naziv dobavljača")) return;
        if (!Validator.validateOib(this, oibField.getText())) return;
        if (!Validator.validateEmail(this, emailField.getText())) return;

        dobavljac.setNaziv(nazivField.getText().trim());
        dobavljac.setOib(nullIfEmpty(oibField.getText()));
        dobavljac.setAdresa(nullIfEmpty(adresaField.getText()));
        dobavljac.setEmail(nullIfEmpty(emailField.getText()));
        dobavljac.setTelefon(nullIfEmpty(telefonField.getText()));
        dobavljac.setNapomena(nullIfEmpty(napomenaArea.getText()));
        dobavljac.setKomisijskiModel(komisijaCb.isSelected());

        try {
            DatabaseManager.getInstance().saveDobavljac(dobavljac);
            saved = true;
            dispose();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška pri spremanju: " + e.getMessage());
        }
    }

    /** Vraća null za prazan string, inače vraća trimani string. */
    private String nullIfEmpty(String s) {
        String t = s == null ? "" : s.trim();
        return t.isEmpty() ? null : t;
    }

    /** @return true ako je dobavljač uspješno pohranjen */
    public boolean isSaved() { return saved; }
}
