package stil.app.ui;

import stil.app.db.DatabaseManager;
import stil.app.model.Artikl;
import stil.app.util.Validator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;

/**
 * Panel za pregled i upravljanje katalogom artikala.
 *
 * Prikazuje tablicu svih artikala s cijenama, PDV stopom i zalihama.
 * Omogućuje dodavanje, uređivanje i brisanje artikala,
 * te ručnu korekciju zaliha (+ Zaliha / - Zaliha).
 * Dvostruki klik na redak otvara formu za uređivanje.
 *
 * Ovaj panel je prikazan kao tab "Artikli" u {@link MainWindow}.
 */
public class ArtikliPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable table;
    private List<Artikl> artikli;

    public ArtikliPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        tableModel = new DefaultTableModel(new String[]{"ID", "Naziv", "Barkod", "Cijena (EUR)", "PDV %", "Zaliha"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(32);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(0).setMinWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton noviBtn = new JButton("Novi artikl");
        JButton urediBtn = new JButton("Uredi");
        JButton obrisiBtn = new JButton("Obriši");
        JButton zalihaPlusBtn = new JButton("+ Zaliha");
        JButton zalihaMinusBtn = new JButton("- Zaliha");

        toolbar.add(noviBtn);
        toolbar.add(urediBtn);
        toolbar.add(obrisiBtn);
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(zalihaPlusBtn);
        toolbar.add(zalihaMinusBtn);
        add(toolbar, BorderLayout.NORTH);

        noviBtn.addActionListener(e -> openForm(null));
        urediBtn.addActionListener(e -> {
            Artikl a = getSelected();
            if (a != null) openForm(a);
        });
        obrisiBtn.addActionListener(e -> obrisi());
        zalihaPlusBtn.addActionListener(e -> promijeniZalihu(1));
        zalihaMinusBtn.addActionListener(e -> promijeniZalihu(-1));

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && getSelected() != null) openForm(getSelected());
            }
        });

        ucitaj();
    }

    private void ucitaj() {
        try {
            artikli = DatabaseManager.getInstance().getArtikli();
            tableModel.setRowCount(0);
            for (Artikl a : artikli) {
                tableModel.addRow(new Object[]{
                    a.getId(), a.getNaziv(), a.getBarkod(),
                    String.format("%.2f", a.getCijena()),
                    String.format("%.0f", a.getPdvStopa()),
                    a.getKolicinaNaSkladistu()
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška: " + e.getMessage());
        }
    }

    private Artikl getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return artikli.get(row);
    }

    private void openForm(Artikl artikl) {
        ArtiklForm form = new ArtiklForm((Frame) SwingUtilities.getWindowAncestor(this), artikl);
        form.setVisible(true);
        if (form.isSaved()) ucitaj();
    }

    private void obrisi() {
        Artikl a = getSelected();
        if (a == null) return;
        int confirm = JOptionPane.showConfirmDialog(this,
            "Obrisati artikl \"" + a.getNaziv() + "\"?", "Potvrda", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            DatabaseManager.getInstance().deleteArtikl(a.getId());
            ucitaj();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška: " + e.getMessage());
        }
    }

    private void promijeniZalihu(int smjer) {
        Artikl a = getSelected();
        if (a == null) return;
        String input = JOptionPane.showInputDialog(this,
            (smjer > 0 ? "Dodaj" : "Ukloni") + " količinu za \"" + a.getNaziv() + "\":", "1");
        if (input == null) return;
        Integer kolicina = Validator.parseInt(this, input, "Količina", 1);
        if (kolicina == null) return;
        try {
            DatabaseManager.getInstance().updateKolicina(a.getId(), smjer * kolicina);
            ucitaj();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška: " + e.getMessage());
        }
    }
}
