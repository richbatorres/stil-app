package stil.app.ui;

import stil.app.db.DatabaseManager;
import stil.app.model.Dobavljac;
import stil.app.model.KomisijaStavka;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Panel za upravljanje komisijskom prodajom.
 *
 * Prikazuje obračun za odabranog komisijskog dobavljača i mjesec:
 * koliko je artikala nabavljeno, prodano i ostalo za povrat.
 * Omogućuje generiranje obračuna, spremanje u bazu i izvoz u tekstualnu datoteku.
 */
public class KomisijaPanel extends JPanel {

    private final JComboBox<Dobavljac> dobavljacCombo = new JComboBox<>();
    private final JComboBox<String> mjesecCombo = new JComboBox<>();
    private final JComboBox<Integer> godinaCombo = new JComboBox<>();
    private final DefaultTableModel tableModel;
    private final JLabel sumaLabel = new JLabel(" ");
    private List<KomisijaStavka> trenutneStavke = List.of();
    private Dobavljac trenutniDobavljac;

    public KomisijaPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // --- GORNJI PANEL: odabir ---
        JPanel odabirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        odabirPanel.setBorder(BorderFactory.createTitledBorder("Odabir dobavljača i razdoblja"));

        String[] mjeseci = {"Siječanj","Veljača","Ožujak","Travanj","Svibanj","Lipanj",
                            "Srpanj","Kolovoz","Rujan","Listopad","Studeni","Prosinac"};
        for (String m : mjeseci) mjesecCombo.addItem(m);

        int godinaSad = LocalDate.now().getYear();
        for (int g = godinaSad; g >= godinaSad - 5; g--) godinaCombo.addItem(g);
        godinaCombo.setPreferredSize(new Dimension(100, godinaCombo.getPreferredSize().height));

        // Defaultno odaberi prethodni mjesec (testni podaci su za zadnjih 6 završenih)
        LocalDate prosliMjesec = LocalDate.now().minusMonths(1);
        mjesecCombo.setSelectedIndex(prosliMjesec.getMonthValue() - 1);
        godinaCombo.setSelectedItem(prosliMjesec.getYear());

        JButton generirajBtn = new JButton("Generiraj obračun");
        JButton spremiBtn    = new JButton("Spremi u bazu");
        JButton izvozBtn     = new JButton("Izvezi u datoteku");

        odabirPanel.add(new JLabel("Dobavljač:"));
        odabirPanel.add(dobavljacCombo);
        odabirPanel.add(new JLabel("Mjesec:"));
        odabirPanel.add(mjesecCombo);
        odabirPanel.add(new JLabel("Godina:"));
        odabirPanel.add(godinaCombo);
        odabirPanel.add(generirajBtn);
        odabirPanel.add(spremiBtn);
        odabirPanel.add(izvozBtn);

        // --- TABLICA ---
        tableModel = new DefaultTableModel(
            new String[]{"Artikl", "Nabavljeno", "Prodano", "Ostalo za povrat",
                         "Nab. cijena", "Vrijednost prodano", "Vrijednost povrat"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(32);
        table.getColumnModel().getColumn(0).setPreferredWidth(220);

        sumaLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        add(odabirPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(sumaLabel, BorderLayout.SOUTH);

        // --- EVENTI ---
        generirajBtn.addActionListener(e -> generiraj());
        spremiBtn.addActionListener(e -> spremiUBazu());
        izvozBtn.addActionListener(e -> izvezi());

        ucitajDobavljace();
    }

    /** Učitava komisijske dobavljače u combo. */
    public void ucitajDobavljace() {
        try {
            dobavljacCombo.removeAllItems();
            List<Dobavljac> lista = DatabaseManager.getInstance().getKomisijskiDobavljaci();
            for (Dobavljac d : lista) dobavljacCombo.addItem(d);
            if (lista.isEmpty()) {
                sumaLabel.setText("Nema dobavljača s komisijskim modelom. Uredite dobavljača i označite opciju.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška pri učitavanju dobavljača: " + e.getMessage());
        }
    }

    /** Generira obračun za odabranog dobavljača i period. */
    private void generiraj() {
        trenutniDobavljac = (Dobavljac) dobavljacCombo.getSelectedItem();
        if (trenutniDobavljac == null) {
            JOptionPane.showMessageDialog(this, "Odaberite dobavljača.", "Upozorenje", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int godina = (Integer) godinaCombo.getSelectedItem();
        int mjesec = mjesecCombo.getSelectedIndex() + 1;
        try {
            trenutneStavke = DatabaseManager.getInstance()
                .generirajKomisijaObracun(trenutniDobavljac.getId(), godina, mjesec);
            prikaziStavke(trenutneStavke, trenutniDobavljac.getNaziv(), godina, mjesec);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška pri generiranju obračuna: " + e.getMessage(),
                "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Prikazuje stavke u tablici i ažurira sumu. */
    private void prikaziStavke(List<KomisijaStavka> stavke, String dobavljacNaziv, int godina, int mjesec) {
        tableModel.setRowCount(0);
        double ukupnoProdano = 0, ukupnoPovrat = 0;
        for (KomisijaStavka s : stavke) {
            tableModel.addRow(new Object[]{
                s.getArtiklNaziv(),
                s.getNabavljeno(),
                s.getProdano(),
                s.getOstalo(),
                String.format("%.2f EUR", s.getNabavnaCijena()),
                String.format("%.2f EUR", s.getVrijednostProdano()),
                String.format("%.2f EUR", s.getVrijednostPovrat())
            });
            ukupnoProdano += s.getVrijednostProdano();
            ukupnoPovrat  += s.getVrijednostPovrat();
        }
        String imeMjeseca = mjesecCombo.getItemAt(mjesec - 1);
        if (stavke.isEmpty()) {
            sumaLabel.setText(dobavljacNaziv + " — " + imeMjeseca + " " + godina
                + ": nema nabava u komisiju za ovaj period.");
        } else {
            sumaLabel.setText(String.format(
                "%s — %s %d  |  Prodano: %.2f EUR  |  Za povrat: %.2f EUR  |  Stavki: %d",
                dobavljacNaziv, imeMjeseca, godina, ukupnoProdano, ukupnoPovrat, stavke.size()));
        }
    }

    /** Sprema trenutni obračun u bazu. */
    private void spremiUBazu() {
        if (trenutniDobavljac == null || trenutneStavke.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Prvo generirajte obračun.", "Upozorenje", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int godina = (Integer) godinaCombo.getSelectedItem();
        int mjesec = mjesecCombo.getSelectedIndex() + 1;
        try {
            DatabaseManager.getInstance().spremiKomisijaObracun(trenutniDobavljac, godina, mjesec, trenutneStavke);
            JOptionPane.showMessageDialog(this, "Obračun uspješno spremljen.", "Uspjeh", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška pri spremanju: " + e.getMessage(),
                "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Izvozi obračun u tekstualnu datoteku u mapu 'komisija/'. */
    private void izvezi() {
        if (trenutniDobavljac == null || trenutneStavke.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Prvo generirajte obračun.", "Upozorenje", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int godina = (Integer) godinaCombo.getSelectedItem();
        int mjesec = mjesecCombo.getSelectedIndex() + 1;
        String imeMjeseca = mjesecCombo.getItemAt(mjesec - 1);

        String naziv = String.format("komisija_%s_%04d_%02d.txt",
            trenutniDobavljac.getNaziv().replaceAll("[^a-zA-Z0-9_]", "_"), godina, mjesec);

        try {
            java.nio.file.Path dir = java.nio.file.Paths.get("komisija");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path datoteka = dir.resolve(naziv);

            StringBuilder sb = new StringBuilder();
            sb.append("KOMISIJSKI OBRAČUN\n");
            sb.append("==================\n");
            sb.append(String.format("Dobavljač: %s%n", trenutniDobavljac.getNaziv()));
            sb.append(String.format("Razdoblje: %s %d%n", imeMjeseca, godina));
            sb.append("\n");
            sb.append(String.format("%-30s %10s %10s %10s %12s %14s %14s%n",
                "Artikl", "Nabavljeno", "Prodano", "Ostalo", "Nab.cijena", "Vr.prodano", "Vr.povrat"));
            sb.append("-".repeat(102)).append("\n");

            double ukupnoProdano = 0, ukupnoPovrat = 0;
            for (KomisijaStavka s : trenutneStavke) {
                sb.append(String.format("%-30s %10d %10d %10d %12.2f %14.2f %14.2f%n",
                    skrati(s.getArtiklNaziv(), 30),
                    s.getNabavljeno(), s.getProdano(), s.getOstalo(),
                    s.getNabavnaCijena(), s.getVrijednostProdano(), s.getVrijednostPovrat()));
                ukupnoProdano += s.getVrijednostProdano();
                ukupnoPovrat  += s.getVrijednostPovrat();
            }
            sb.append("-".repeat(102)).append("\n");
            sb.append(String.format("%-30s %10s %10s %10s %12s %14.2f %14.2f%n",
                "UKUPNO", "", "", "", "", ukupnoProdano, ukupnoPovrat));

            java.nio.file.Files.writeString(datoteka, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);

            JOptionPane.showMessageDialog(this,
                "Obračun izvezen u:\n" + datoteka.toAbsolutePath(),
                "Izvoz uspješan", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Greška pri izvozu: " + e.getMessage(),
                "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String skrati(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "." : s;
    }
}
