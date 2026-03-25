package stil.app.ui;

import stil.app.db.DatabaseManager;
import stil.app.model.Artikl;
import stil.app.model.Dobavljac;
import stil.app.model.Nabava;
import stil.app.util.Validator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel za evidenciju nabave robe od dobavljača.
 *
 * Prikazuje tablicu svih nabava (od najnovije) i formu za unos nove nabave.
 * Svaka nabava atomarno ažurira zalihu artikla u istoj transakciji —
 * pad aplikacije ili gubitak konekcije ne može unijeti nekonzistentno stanje.
 */
public class NabavaPanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final DefaultTableModel tableModel;
    private final JComboBox<Artikl> artiklCombo = new JComboBox<>();
    private final JComboBox<Dobavljac> dobavljacCombo = new JComboBox<>();
    private final JSpinner kolicinaSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99999, 1));
    private final JFormattedTextField cijenaField = new JFormattedTextField();
    private final JTextField napomenaField = new JTextField();

    public NabavaPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        tableModel = new DefaultTableModel(
            new String[]{"Datum", "Artikl", "Dobavljač", "Kol.", "Nab. cijena", "Ukupno"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(kreirajFormu(), BorderLayout.SOUTH);

        ucitajPodatke();
    }

    /** Kreira formu za unos nove nabave. */
    private JPanel kreirajFormu() {
        JPanel forma = new JPanel(new GridBagLayout());
        forma.setBorder(BorderFactory.createTitledBorder("Nova nabava"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        cijenaField.setValue(0.00);
        cijenaField.setColumns(8);

        dodajPolje(forma, gbc, 0, "Artikl:", artiklCombo);
        dodajPolje(forma, gbc, 1, "Dobavljač:", dobavljacCombo);
        dodajPolje(forma, gbc, 2, "Količina:", kolicinaSpinner);
        dodajPolje(forma, gbc, 3, "Nab. cijena (€):", cijenaField);
        dodajPolje(forma, gbc, 4, "Napomena:", napomenaField);

        JButton spremiBtn = new JButton("💾 Spremi nabavu");
        spremiBtn.addActionListener(e -> spremiNabavu());
        gbc.gridx = 1; gbc.gridy = 5;
        forma.add(spremiBtn, gbc);

        return forma;
    }

    /** Pomoćna metoda za dodavanje labele i komponente u GridBagLayout. */
    private void dodajPolje(JPanel p, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        p.add(comp, gbc);
    }

    /**
     * Učitava artikle, dobavljače i listu nabava iz baze.
     * Poziva se pri inicijalizaciji i nakon svake pohrane.
     */
    public void ucitajPodatke() {
        try {
            DatabaseManager db = DatabaseManager.getInstance();

            artiklCombo.removeAllItems();
            for (Artikl a : db.getArtikli()) artiklCombo.addItem(a);

            dobavljacCombo.removeAllItems();
            dobavljacCombo.addItem(null); // opcija "bez dobavljača"
            for (Dobavljac d : db.getDobavljaci()) dobavljacCombo.addItem(d);

            tableModel.setRowCount(0);
            for (Nabava n : db.getNabave()) {
                tableModel.addRow(new Object[]{
                    n.getVrijemeNabave().format(FMT),
                    n.getArtiklNaziv(),
                    n.getDobavljacNaziv() != null ? n.getDobavljacNaziv() : "-",
                    n.getKolicina(),
                    String.format("%.2f €", n.getNabavnaCijena()),
                    String.format("%.2f €", n.getUkupniTrosak())
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška pri učitavanju: " + e.getMessage(),
                "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Validira unos i sprema novu nabavu u bazu. */
    private void spremiNabavu() {
        Artikl artikl = (Artikl) artiklCombo.getSelectedItem();
        if (artikl == null) {
            JOptionPane.showMessageDialog(this, "Odaberite artikl.", "Upozorenje", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Double cijena = Validator.parseDecimal(this, cijenaField.getValue().toString(),
            "Nabavna cijena", true, false);
        if (cijena == null) return;
        Integer kolicina = Validator.parseInt(this, kolicinaSpinner.getValue().toString(),
            "Količina", 1);
        if (kolicina == null) return;
        Dobavljac dobavljac = (Dobavljac) dobavljacCombo.getSelectedItem();

        Nabava n = new Nabava(artikl.getId(), artikl.getNaziv(), kolicina, cijena,
            napomenaField.getText().trim().isEmpty() ? null : napomenaField.getText().trim());
        n.setVrijemeNabave(LocalDateTime.now());
        if (dobavljac != null) {
            n.setDobavljacId(dobavljac.getId());
            n.setDobavljacNaziv(dobavljac.getNaziv());
        }

        try {
            DatabaseManager.getInstance().saveNabava(n);
            napomenaField.setText("");
            cijenaField.setValue(0.00);
            kolicinaSpinner.setValue(1);
            ucitajPodatke();
            JOptionPane.showMessageDialog(this,
                String.format("Nabava pohranjena. Zaliha artikla \"%s\" povećana za %d kom.", artikl.getNaziv(), kolicina),
                "Uspjeh", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška pri pohrani nabave: " + e.getMessage(),
                "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }
}
