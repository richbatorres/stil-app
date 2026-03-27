package stil.app.ui;

import stil.app.db.DatabaseManager;
import stil.app.fisk.FiskalizacijaServis;
import stil.app.model.Artikl;
import stil.app.model.Config;
import stil.app.model.Racun;
import stil.app.model.StavkaRacuna;
import stil.app.print.IspisRacuna;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Glavni prodajni ekran blagajne.
 *
 * Lijeva strana prikazuje katalog artikala s pretraživanjem po nazivu i barkodu.
 * Desna strana prikazuje košaricu s mogućnošću uređivanja količine i popusta.
 *
 * Tok naplate:
 * 1. Odabir artikala (klik, dvostruki klik ili skeniranje barkoda)
 * 2. Klik "NAPLATI" → sprema račun u bazu, fiskalizira (ako je konfigurirano), ispisuje
 *
 * Zalihe se automatski smanjuju pri svakoj naplati.
 */
public class ProdajaPanel extends JPanel {

    private final DefaultTableModel artiklModel;
    private final DefaultTableModel kosariceModel;
    private final JTextField pretragaField;
    private final JLabel ukupnoLabel;
    private final JComboBox<String> nacinPlacanjaBox;
    private final List<StavkaRacuna> kosarica = new ArrayList<>();
    private List<Artikl> sviArtikli = new ArrayList<>();

    public ProdajaPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // --- LIJEVO: lista artikala ---
        JPanel lijevo = new JPanel(new BorderLayout(4, 4));
        lijevo.setBorder(BorderFactory.createTitledBorder("Artikli"));

        pretragaField = new JTextField();
        pretragaField.setToolTipText("Pretraži po nazivu ili skeniraj barkod");
        lijevo.add(pretragaField, BorderLayout.NORTH);

        artiklModel = new DefaultTableModel(new String[]{"Naziv", "Cijena", "Zaliha"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable artiklTable = new JTable(artiklModel);
        artiklTable.setRowHeight(32);
        artiklTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        artiklTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        artiklTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        lijevo.add(new JScrollPane(artiklTable), BorderLayout.CENTER);

        JButton dodajBtn = new JButton("Dodaj u košaricu");
        lijevo.add(dodajBtn, BorderLayout.SOUTH);

        // --- DESNO: košarica ---
        JPanel desno = new JPanel(new BorderLayout(4, 4));
        desno.setBorder(BorderFactory.createTitledBorder("Košarica"));

        kosariceModel = new DefaultTableModel(new String[]{"Naziv", "Kom", "Cijena", "Popust%", "Ukupno"}, 0) {
            public boolean isCellEditable(int r, int c) { return c == 1 || c == 3; }
        };
        JTable kosaricaTable = new JTable(kosariceModel);
        kosaricaTable.setRowHeight(32);
        desno.add(new JScrollPane(kosaricaTable), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        ukupnoLabel = new JLabel("Ukupno: 0,00 EUR");

        nacinPlacanjaBox = new JComboBox<>(new String[]{"GOTOVINA", "KARTICA"});

        JButton ukloniBtn = new JButton("Ukloni stavku");
        JButton ocistiBtn = new JButton("Očisti");
        JButton naплatiBtn = new JButton("NAPLATI");

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        bottomPanel.add(ukupnoLabel, gbc);
        gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.5;
        bottomPanel.add(new JLabel("Način plaćanja:"), gbc);
        gbc.gridx = 1;
        bottomPanel.add(nacinPlacanjaBox, gbc);
        gbc.gridx = 0; gbc.gridy = 2;
        bottomPanel.add(ukloniBtn, gbc);
        gbc.gridx = 1;
        bottomPanel.add(ocistiBtn, gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        bottomPanel.add(naплatiBtn, gbc);

        desno.add(bottomPanel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lijevo, desno);
        split.setDividerLocation(450);
        add(split, BorderLayout.CENTER);

        // --- EVENTI ---
        pretragaField.addActionListener(e -> onBarkodScan());
        pretragaField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterArtikli(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterArtikli(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });

        dodajBtn.addActionListener(e -> {
            int row = artiklTable.getSelectedRow();
            if (row >= 0) dodajUKosaricu(getFilteredArtikl(row));
        });

        artiklTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = artiklTable.getSelectedRow();
                    if (row >= 0) dodajUKosaricu(getFilteredArtikl(row));
                }
            }
        });

        kosariceModel.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int col = e.getColumn();
                if (row >= 0 && row < kosarica.size()) {
                    try {
                        if (col == 1) {
                            int kom = Integer.parseInt(kosariceModel.getValueAt(row, 1).toString());
                            kosarica.get(row).setKolicina(Math.max(1, kom));
                        } else if (col == 3) {
                            double popust = Double.parseDouble(kosariceModel.getValueAt(row, 3).toString());
                            kosarica.get(row).setPopust(Math.min(100, Math.max(0, popust)));
                        }
                        osvjeziKosaricu();
                    } catch (NumberFormatException ignored) {}
                }
            }
        });

        ukloniBtn.addActionListener(e -> {
            int row = kosaricaTable.getSelectedRow();
            if (row >= 0) {
                kosarica.remove(row);
                osvjeziKosaricu();
            }
        });

        ocistiBtn.addActionListener(e -> {
            kosarica.clear();
            osvjeziKosaricu();
        });

        naплatiBtn.addActionListener(e -> naplati());

        ucitajArtikle();
    }

    private void ucitajArtikle() {
        try {
            sviArtikli = DatabaseManager.getInstance().getArtikli();
            filterArtikli();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška pri učitavanju artikala: " + e.getMessage());
        }
    }

    private void filterArtikli() {
        String query = pretragaField.getText().trim().toLowerCase();
        artiklModel.setRowCount(0);
        for (Artikl a : sviArtikli) {
            if (query.isEmpty() || a.getNaziv().toLowerCase().contains(query)
                    || (a.getBarkod() != null && a.getBarkod().contains(query))) {
                artiklModel.addRow(new Object[]{
                    a.getNaziv(),
                    String.format("%.2f EUR", a.getCijena()),
                    a.getKolicinaNaSkladistu()
                });
            }
        }
    }

    private void onBarkodScan() {
        String query = pretragaField.getText().trim();
        for (Artikl a : sviArtikli) {
            if (query.equals(a.getBarkod())) {
                dodajUKosaricu(a);
                pretragaField.setText("");
                return;
            }
        }
        filterArtikli();
    }

    private Artikl getFilteredArtikl(int tableRow) {
        String query = pretragaField.getText().trim().toLowerCase();
        List<Artikl> filtered = sviArtikli.stream()
            .filter(a -> query.isEmpty() || a.getNaziv().toLowerCase().contains(query)
                || (a.getBarkod() != null && a.getBarkod().contains(query)))
            .toList();
        return filtered.get(tableRow);
    }

    private void dodajUKosaricu(Artikl a) {
        for (StavkaRacuna s : kosarica) {
            if (s.getArtiklId() == a.getId()) {
                s.setKolicina(s.getKolicina() + 1);
                osvjeziKosaricu();
                return;
            }
        }
        kosarica.add(new StavkaRacuna(a, 1, 0));
        osvjeziKosaricu();
    }

    private void osvjeziKosaricu() {
        kosariceModel.setRowCount(0);
        for (StavkaRacuna s : kosarica) {
            kosariceModel.addRow(new Object[]{
                s.getArtiklNaziv(),
                s.getKolicina(),
                String.format("%.2f", s.getCijena()),
                String.format("%.1f", s.getPopust()),
                String.format("%.2f EUR", s.getUkupno())
            });
        }
        double ukupno = kosarica.stream().mapToDouble(StavkaRacuna::getUkupno).sum();
        ukupnoLabel.setText(String.format("Ukupno: %.2f EUR", ukupno));
    }

    private void naplati() {
        if (kosarica.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Košarica je prazna.");
            return;
        }
        // Provjeri zalihe za sve stavke prije nego što počnemo transakciju
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            for (StavkaRacuna s : kosarica) {
                Artikl svjezi = db.getArtiklById(s.getArtiklId());
                if (svjezi != null && svjezi.getKolicinaNaSkladistu() < s.getKolicina()) {
                    JOptionPane.showMessageDialog(this,
                        "Nedovoljna zaliha za \"" + s.getArtiklNaziv() + "\":\n" +
                        "Na skladištu: " + svjezi.getKolicinaNaSkladistu() + " kom\n" +
                        "Traženo: " + s.getKolicina() + " kom",
                        "Nedovoljna zaliha", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška pri provjeri zaliha: " + e.getMessage(),
                "Greška", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            int brojRacuna = db.getSljedeciBrojRacuna();

            Racun racun = new Racun();
            racun.setBrojRacuna(brojRacuna);
            racun.setOznakaRacuna(brojRacuna + "-1-1");
            racun.setVrijemeIzdavanja(LocalDateTime.now());
            racun.setNacinPlacanja(nacinPlacanjaBox.getSelectedIndex() == 0
                ? Racun.NacinPlacanja.GOTOVINA : Racun.NacinPlacanja.KARTICA);
            racun.setStatus(Racun.Status.KREIRAN);
            racun.setStavke(new ArrayList<>(kosarica));

            db.saveRacun(racun);
            Config config = db.loadConfig();

            // Fiskalizacija
            if (config.getOib() != null && !config.getOib().isEmpty()
                    && config.getPutanjaCertifikata() != null && !config.getPutanjaCertifikata().isEmpty()) {
                try {
                    FiskalizacijaServis fisk = new FiskalizacijaServis(config, config.isFiskTestMode());
                    String jir = fisk.fiskaliziraj(racun);
                    racun.setJir(jir);
                    db.updateRacunFiskalizacija(racun.getId(), racun.getZki(), jir);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                        "Fiskalizacija nije uspjela: " + ex.getMessage() + "\nRacun je spremljen lokalno.",
                        "Upozorenje", JOptionPane.WARNING_MESSAGE);
                }
            }

            // Spremi račun u PDF datoteku (obavezno — alarm ako ne uspije)
            IspisRacuna ispis = new IspisRacuna(racun, config);
            try {
                ispis.spremiRacun("racuni");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "KRITIČNA GREŠKA: Račun nije mogao biti spremljen!\n\n" +
                    "Razlog: " + ex.getMessage() + "\n\n" +
                    "Molimo kontaktirajte tehničku podršku.",
                    "Greška spremanja računa", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Ispis (neobavezno — alarm ako ne uspije, ali račun je već spremljen)
            String ispisGreska = ispis.ispisiBezDijaloga();
            if (ispisGreska != null) {
                JOptionPane.showMessageDialog(this,
                    "Račun je uspješno spremljen.\n\nIspis nije moguć.\nRazlog: " + ispisGreska,
                    "Ispis nije uspio", JOptionPane.WARNING_MESSAGE);
            }

            kosarica.clear();
            osvjeziKosaricu();
            ucitajArtikle();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška pri naplati: " + e.getMessage(), "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void osvjeziArtikle() {
        ucitajArtikle();
    }
}
