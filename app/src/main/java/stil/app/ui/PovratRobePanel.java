package stil.app.ui;

import stil.app.db.DatabaseManager;
import stil.app.model.Artikl;
import stil.app.model.Dobavljac;
import stil.app.model.PovratRobe;
import stil.app.util.Validator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel za evidenciju i pregled povrata robe.
 *
 * Podržava dva tipa povrata:
 * - OD_KUPCA: kupac vraća robu u trgovinu (povećava zalihu artikla)
 * - DOBAVLJACU: trgovina vraća robu dobavljaču (smanjuje zalihu artikla)
 *
 * Gornji dio panela prikazuje formu za unos novog povrata.
 * Donji dio prikazuje tablicu svih dosadašnjih povrata.
 *
 * Ovaj panel je prikazan kao tab "Povrat robe" u {@link MainWindow}.
 */
public class PovratRobePanel extends JPanel {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final JComboBox<Artikl>    artiklBox     = new JComboBox<>();
    private final JComboBox<String>    tipBox        = new JComboBox<>(new String[]{"OD_KUPCA", "DOBAVLJACU"});
    private final JComboBox<Dobavljac> dobavljacBox  = new JComboBox<>();
    private final JTextField           kolicinField  = new JTextField("1", 8);
    private final JTextField           cijenaField   = new JTextField("0.00", 8);
    private final JTextField           razlogField   = new JTextField(20);
    private final JLabel               dobavljacLabel = new JLabel("Dobavljač:");

    private final DefaultTableModel tableModel = new DefaultTableModel(
        new String[]{"Datum", "Artikl", "Tip", "Dobavljač", "Kom", "Cijena/kom", "Ukupno", "Razlog"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private List<Artikl>    artikli;
    private List<Dobavljac> dobavljaci;

    public PovratRobePanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(buildFormaPanel(), BorderLayout.NORTH);
        add(buildTablicaPanel(), BorderLayout.CENTER);

        // Dobavljač polje vidljivo samo za tip DOBAVLJACU
        tipBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                boolean jeDobavljacu = "DOBAVLJACU".equals(tipBox.getSelectedItem());
                dobavljacLabel.setVisible(jeDobavljacu);
                dobavljacBox.setVisible(jeDobavljacu);
            }
        });

        // Inicijalno sakrij dobavljač polje (default je OD_KUPCA)
        dobavljacLabel.setVisible(false);
        dobavljacBox.setVisible(false);

        // Kad se odabere artikl, automatski popuni cijenu
        artiklBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && artiklBox.getSelectedItem() instanceof Artikl a) {
                cijenaField.setText(String.format("%.2f", a.getCijena()));
            }
        });

        ucitajPodatke();
    }

    /** Gradi gornji panel s formom za unos novog povrata. */
    private JPanel buildFormaPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Novi povrat robe"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        addRow(panel, gbc, 0, "Artikl:", artiklBox);
        addRow(panel, gbc, 1, "Tip povrata:", tipBox);
        addRow(panel, gbc, 2, dobavljacLabel, dobavljacBox);
        addRow(panel, gbc, 3, "Količina:", kolicinField);
        addRow(panel, gbc, 4, "Cijena/kom (EUR):", cijenaField);
        addRow(panel, gbc, 5, "Razlog:", razlogField);

        JButton spremiBtn = new JButton("Evidentiraj povrat");
        spremiBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(spremiBtn, gbc);

        spremiBtn.addActionListener(e -> evidentirajPovrat());
        return panel;
    }

    /** Gradi donji panel s tablicom svih povrata. */
    private JPanel buildTablicaPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Povijest povrata"));

        JTable table = new JTable(tableModel);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        table.setRowHeight(22);
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    /** Dodaje redak s labelom (String) i komponentom u GridBag formu. */
    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        addRow(panel, gbc, row, new JLabel(label), field);
    }

    /** Dodaje redak s labelom (JLabel) i komponentom u GridBag formu. */
    private void addRow(JPanel panel, GridBagConstraints gbc, int row, JLabel label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(label, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    /** Učitava artikle, dobavljače i povijest povrata iz baze. */
    public void ucitajPodatke() {
        try {
            DatabaseManager db = DatabaseManager.getInstance();

            artikli = db.getArtikli();
            artiklBox.removeAllItems();
            for (Artikl a : artikli) artiklBox.addItem(a);

            dobavljaci = db.getDobavljaci();
            dobavljacBox.removeAllItems();
            dobavljacBox.addItem(null); // opcija "bez dobavljača"
            for (Dobavljac d : dobavljaci) dobavljacBox.addItem(d);

            ucitajPovrate(db);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška pri učitavanju: " + e.getMessage());
        }
    }

    /** Učitava i prikazuje povijest povrata u tablici. */
    private void ucitajPovrate(DatabaseManager db) throws SQLException {
        List<PovratRobe> povrati = db.getPovrati();
        tableModel.setRowCount(0);
        for (PovratRobe p : povrati) {
            tableModel.addRow(new Object[]{
                p.getVrijemePovrata().format(DTF),
                p.getArtiklNaziv(),
                p.getTipPovrata() == PovratRobe.TipPovrata.OD_KUPCA ? "Od kupca" : "Dobavljaču",
                p.getDobavljacNaziv() != null ? p.getDobavljacNaziv() : "—",
                p.getKolicina(),
                String.format("%.2f EUR", p.getCijenaPoKomadu()),
                String.format("%.2f EUR", p.getUkupniIznos()),
                p.getRazlog() != null ? p.getRazlog() : ""
            });
        }
    }

    /** Validira unos, kreira povrat i sprema ga u bazu. */
    private void evidentirajPovrat() {
        Artikl artikl = (Artikl) artiklBox.getSelectedItem();
        if (artikl == null) {
            JOptionPane.showMessageDialog(this, "Odaberite artikl.", "Neispravan unos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Integer kolicina = Validator.parseInt(this, kolicinField.getText(), "Količina", 1);
        if (kolicina == null) return;
        Double cijena = Validator.parseDecimal(this, cijenaField.getText(), "Cijena/kom", true, false);
        if (cijena == null) return;

        PovratRobe.TipPovrata tip = PovratRobe.TipPovrata.valueOf((String) tipBox.getSelectedItem());

        PovratRobe povrat = new PovratRobe(
            artikl.getId(), artikl.getNaziv(),
            kolicina, cijena, tip,
            nullIfEmpty(razlogField.getText())
        );
        povrat.setVrijemePovrata(LocalDateTime.now());

        // Poveži s dobavljačem ako je odabran i tip je DOBAVLJACU
        if (tip == PovratRobe.TipPovrata.DOBAVLJACU && dobavljacBox.getSelectedItem() instanceof Dobavljac d) {
            povrat.setDobavljacId(d.getId());
            povrat.setDobavljacNaziv(d.getNaziv());
        }

        try {
            DatabaseManager.getInstance().savePovrat(povrat);
            JOptionPane.showMessageDialog(this,
                "Povrat evidentiran. Zaliha artikla \"" + artikl.getNaziv() + "\" " +
                (tip == PovratRobe.TipPovrata.OD_KUPCA ? "povećana" : "smanjena") +
                " za " + kolicina + " kom.");
            resetFormu();
            ucitajPodatke();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška pri evidenciji povrata: " + e.getMessage());
        }
    }

    /** Resetira formu na početne vrijednosti. */
    private void resetFormu() {
        kolicinField.setText("1");
        razlogField.setText("");
        if (artiklBox.getSelectedItem() instanceof Artikl a)
            cijenaField.setText(String.format("%.2f", a.getCijena()));
    }

    /** Vraća null za prazan string, inače vraća trimani string. */
    private String nullIfEmpty(String s) {
        String t = s == null ? "" : s.trim();
        return t.isEmpty() ? null : t;
    }
}
