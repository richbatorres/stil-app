package stil.app.ui;

import stil.app.db.DatabaseManager;
import stil.app.model.Dobavljac;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;

/**
 * Panel za pregled i upravljanje katalogom dobavljača.
 *
 * Prikazuje tablicu svih dobavljača s osnovnim podacima.
 * Omogućuje dodavanje, uređivanje i brisanje dobavljača.
 * Dvostruki klik na redak otvara formu za uređivanje.
 *
 * Ovaj panel je prikazan kao tab "Dobavljači" u {@link MainWindow}.
 */
public class DobavljaciPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable table;
    private List<Dobavljac> dobavljaci;

    public DobavljaciPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        tableModel = new DefaultTableModel(
            new String[]{"ID", "Naziv", "OIB", "Email", "Telefon", "Adresa"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(36);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(0).setMinWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(160);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton noviBtn   = new JButton("Novi dobavljač");
        JButton urediBtn  = new JButton("Uredi");
        JButton obrisiBtn = new JButton("Obriši");

        toolbar.add(noviBtn);
        toolbar.add(urediBtn);
        toolbar.add(obrisiBtn);
        add(toolbar, BorderLayout.NORTH);

        noviBtn.addActionListener(e -> openForm(null));
        urediBtn.addActionListener(e -> {
            Dobavljac d = getSelected();
            if (d != null) openForm(d);
        });
        obrisiBtn.addActionListener(e -> obrisi());

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && getSelected() != null) openForm(getSelected());
            }
        });

        ucitaj();
    }

    /** Učitava sve dobavljače iz baze i osvježava tablicu. */
    public void ucitaj() {
        try {
            dobavljaci = DatabaseManager.getInstance().getDobavljaci();
            tableModel.setRowCount(0);
            for (Dobavljac d : dobavljaci) {
                tableModel.addRow(new Object[]{
                    d.getId(), d.getNaziv(), d.getOib(),
                    d.getEmail(), d.getTelefon(), d.getAdresa()
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška: " + e.getMessage());
        }
    }

    /** Vraća trenutno odabranog dobavljača ili null ako ništa nije odabrano. */
    private Dobavljac getSelected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : dobavljaci.get(row);
    }

    /** Otvara formu za dodavanje/uređivanje dobavljača. */
    private void openForm(Dobavljac dobavljac) {
        DobavljacForm form = new DobavljacForm(
            (Frame) SwingUtilities.getWindowAncestor(this), dobavljac);
        form.setVisible(true);
        if (form.isSaved()) ucitaj();
    }

    /** Briše odabranog dobavljača uz potvrdu korisnika. */
    private void obrisi() {
        Dobavljac d = getSelected();
        if (d == null) return;
        int confirm = JOptionPane.showConfirmDialog(this,
            "Obrisati dobavljača \"" + d.getNaziv() + "\"?\n" +
            "Povijesni povrati robe neće biti obrisani.",
            "Potvrda brisanja", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            DatabaseManager.getInstance().deleteDobavljac(d.getId());
            ucitaj();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška: " + e.getMessage());
        }
    }
}
