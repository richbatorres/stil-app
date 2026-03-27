package stil.app.ui;

import stil.app.db.DatabaseManager;
import stil.app.model.IzvjestajPodaci;
import stil.app.model.Racun;

import stil.app.print.IspisIzvjestaja;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Panel za pregled poslovnih izvještaja u odabranom vremenskom razdoblju.
 *
 * Prikazuje agregirane podatke o prodaji, nabavi, povratima i marži.
 * Sadrži i tablicu pojedinih računa za detaljan pregled prometa.
 * Izvještaji se generiraju direktno iz baze — uvijek su točni i ažurni.
 */
public class IzvjestajiPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final JTextField odField = new JTextField(10);
    private final JTextField doField = new JTextField(10);
    private final JTextArea sumarnoArea = new JTextArea(12, 40);
    private final DefaultTableModel racuniModel;

    private IzvjestajPodaci trenutniIzvjestaj;
    private LocalDate trenutniOd, trenutniDo;

    public IzvjestajiPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        racuniModel = new DefaultTableModel(
            new String[]{"Datum", "Oznaka", "Način plaćanja", "Status", "Iznos"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable racuniTable = new JTable(racuniModel);

        JButton stornirajBtn = new JButton("❌ Storniraj račun");
        stornirajBtn.addActionListener(e -> stornirajOdabrani(racuniTable));
        JPanel racuniPanel = new JPanel(new BorderLayout(4, 4));
        racuniPanel.add(new JScrollPane(racuniTable), BorderLayout.CENTER);
        JPanel racuniToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        racuniToolbar.add(stornirajBtn);
        racuniPanel.add(racuniToolbar, BorderLayout.SOUTH);

        sumarnoArea.setEditable(false);
        sumarnoArea.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(sumarnoArea),
            racuniPanel);
        split.setResizeWeight(0.45);

        add(kreirajFilterPanel(), BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        // Postavi default: tekući mjesec
        LocalDate danas = LocalDate.now();
        odField.setText(LocalDate.of(danas.getYear(), danas.getMonth(), 1).format(DATE_FMT));
        doField.setText(danas.format(DATE_FMT));
    }

    /** Kreira panel s filterima za odabir vremenskog raspona. */
    private JPanel kreirajFilterPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setBorder(BorderFactory.createTitledBorder("Vremensko razdoblje"));

        p.add(new JLabel("Od:"));
        p.add(odField);
        p.add(new JLabel("Do:"));
        p.add(doField);
        p.add(new JLabel("(format: dd.MM.yyyy)"));

        JButton prikaziBtn = new JButton("📊 Prikaži izvještaj");
        prikaziBtn.addActionListener(e -> prikaziIzvjestaj());
        p.add(prikaziBtn);

        JButton ispisBtn = new JButton("🖨 Ispis izvještaja");
        ispisBtn.addActionListener(e -> ispisIzvjestaja());
        p.add(ispisBtn);

        // Brzi gumbi za česta razdoblja
        JButton danasnjiBtn = new JButton("Danas");
        danasnjiBtn.addActionListener(e -> {
            String danas = LocalDate.now().format(DATE_FMT);
            odField.setText(danas);
            doField.setText(danas);
            prikaziIzvjestaj();
        });

        JButton ovajMjesecBtn = new JButton("Ovaj mjesec");
        ovajMjesecBtn.addActionListener(e -> {
            LocalDate danas = LocalDate.now();
            odField.setText(LocalDate.of(danas.getYear(), danas.getMonth(), 1).format(DATE_FMT));
            doField.setText(danas.format(DATE_FMT));
            prikaziIzvjestaj();
        });

        JButton ovaGodinaBtn = new JButton("Ova godina");
        ovaGodinaBtn.addActionListener(e -> {
            int god = LocalDate.now().getYear();
            odField.setText(LocalDate.of(god, 1, 1).format(DATE_FMT));
            doField.setText(LocalDate.now().format(DATE_FMT));
            prikaziIzvjestaj();
        });

        p.add(danasnjiBtn);
        p.add(ovajMjesecBtn);
        p.add(ovaGodinaBtn);
        return p;
    }

    /** Stornira odabrani račun nakon potvrde korisnika i osvježava prikaz. */
    private void stornirajOdabrani(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Odaberite račun za storniranje.",
                "Upozorenje", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String oznaka = (String) racuniModel.getValueAt(row, 1);
        String status = (String) racuniModel.getValueAt(row, 3);
        if ("STORNIRAN".equals(status)) {
            JOptionPane.showMessageDialog(this, "Račun je već storniran.",
                "Upozorenje", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int potvrda = JOptionPane.showConfirmDialog(this,
            "Stornirati račun " + oznaka + "?\nOva akcija se ne može poništiti.",
            "Potvrda storniranja", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (potvrda != JOptionPane.YES_OPTION) return;
        try {
            // Dohvati ID računa iz baze prema oznaci
            DatabaseManager db = DatabaseManager.getInstance();
            String odStr = LocalDateTime.parse(
                racuniModel.getValueAt(row, 0).toString(),
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")).minusSeconds(1).toString();
            String doStr = LocalDateTime.parse(
                racuniModel.getValueAt(row, 0).toString(),
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")).plusSeconds(59).toString();
            List<Racun> racuni = db.getRacuniURasponu(odStr, doStr);
            racuni.stream()
                  .filter(r -> r.getOznakaRacuna().equals(oznaka))
                  .findFirst()
                  .ifPresent(r -> {
                      try { db.stornirajRacun(r.getId()); } catch (SQLException ex) {
                          JOptionPane.showMessageDialog(this, "Greška: " + ex.getMessage(),
                              "Greška", JOptionPane.ERROR_MESSAGE);
                      }
                  });
            prikaziIzvjestaj(); // osvježi
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Greška pri storniranju: " + e.getMessage(),
                "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Parsira datume, dohvaća izvještaj iz baze i prikazuje rezultate.
     * Greška pri parsiranju datuma prikazuje se korisniku bez rušenja aplikacije.
     */
    private void prikaziIzvjestaj() {
        LocalDate od, do_;
        try {
            od = LocalDate.parse(odField.getText().trim(), DATE_FMT);
            do_ = LocalDate.parse(doField.getText().trim(), DATE_FMT);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                "Neispravan format datuma. Koristite dd.MM.yyyy (npr. 01.01.2024).",
                "Greška", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (od.isAfter(do_)) {
            JOptionPane.showMessageDialog(this, "Datum 'Od' mora biti prije datuma 'Do'.",
                "Greška", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String odStr = od.atStartOfDay().toString();
        String doStr = do_.atTime(23, 59, 59).toString();

        try {
            DatabaseManager db = DatabaseManager.getInstance();
            trenutniIzvjestaj = db.generirajIzvjestaj(odStr, doStr);
            trenutniOd = od;
            trenutniDo = do_;
            List<Racun> racuni = db.getRacuniURasponu(odStr, doStr);
            prikaziSumarno(trenutniIzvjestaj, od, do_);
            prikaziRacune(racuni);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška pri generiranju izvještaja: " + e.getMessage(),
                "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Sprema i ispisuje trenutni izvještaj kao PDF. */
    private void ispisIzvjestaja() {
        if (trenutniIzvjestaj == null) {
            JOptionPane.showMessageDialog(this, "Prvo prikažite izvještaj.",
                "Upozorenje", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            java.nio.file.Path pdf = IspisIzvjestaja.spremiIzvjestajPdf(
                trenutniIzvjestaj, trenutniOd, trenutniDo, "izvjestaji");
            String greška = IspisIzvjestaja.ispisiPdf(pdf);
            if (greška != null) {
                JOptionPane.showMessageDialog(this,
                    "Izvještaj je spremljen u:\n" + pdf + "\n\nIspis nije moguć.\nRazlog: " + greška,
                    "Ispis nije uspio", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Greška pri generiranju PDF-a: " + e.getMessage(),
                "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Formatira i prikazuje sumarni izvještaj u tekstualnom polju. */
    private void prikaziSumarno(IzvjestajPodaci p, LocalDate od, LocalDate do_) {
        String sep = "─".repeat(44);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  IZVJEŠTAJ O POSLOVANJU%n"));
        sb.append(String.format("  Razdoblje: %s — %s%n", od.format(DATE_FMT), do_.format(DATE_FMT)));
        sb.append(sep).append("\n");
        sb.append(String.format("  PRODAJA%n"));
        sb.append(String.format("  Broj računa:          %6d%n", p.getBrojRacuna()));
        sb.append(String.format("  Ukupna prodaja:    %9.2f €%n", p.getUkupnaProdaja()));
        sb.append(String.format("    od toga PDV:     %9.2f €%n", p.getUkupniPdvProdaja()));
        sb.append(String.format("    gotovina:        %9.2f €%n", p.getProdajaGotovina()));
        sb.append(String.format("    kartica:         %9.2f €%n", p.getProdajaKartica()));
        sb.append(sep).append("\n");
        sb.append(String.format("  NABAVA%n"));
        sb.append(String.format("  Broj nabava:          %6d%n", p.getBrojNabava()));
        sb.append(String.format("  Ukupna nabava:     %9.2f €%n", p.getUkupnaNabava()));
        sb.append(sep).append("\n");
        sb.append(String.format("  POVRATI%n"));
        sb.append(String.format("  Povrati od kupca:     %6d  (%,.2f €)%n",
            p.getBrojPovratOdKupca(), p.getIznosPovratOdKupca()));
        sb.append(String.format("  Povrati dobavljaču:   %6d  (%,.2f €)%n",
            p.getBrojPovratDobavljacu(), p.getIznosPovratDobavljacu()));
        sb.append(sep).append("\n");
        sb.append(String.format("  REZULTAT%n"));
        sb.append(String.format("  Neto promet:       %9.2f €%n", p.getNetoPromet()));
        sb.append(String.format("  Bruto marža:       %9.2f €%n", p.getBrutoMarza()));
        sumarnoArea.setText(sb.toString());
        sumarnoArea.setCaretPosition(0);
    }

    /** Puni tablicu računa za detaljan pregled prometa. */
    private void prikaziRacune(List<Racun> racuni) {
        racuniModel.setRowCount(0);
        for (Racun r : racuni) {
            racuniModel.addRow(new Object[]{
                r.getVrijemeIzdavanja().format(DT_FMT),
                r.getOznakaRacuna(),
                r.getNacinPlacanja() == Racun.NacinPlacanja.GOTOVINA ? "Gotovina" : "Kartica",
                r.getStatus().name(),
                String.format("%.2f €", r.getUkupno())
            });
        }
    }
}
