package stil.app.ui;

import stil.app.db.DatabaseManager;
import stil.app.model.Artikl;
import stil.app.model.Dobavljac;
import stil.app.util.Validator;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JDialog za dodavanje i uređivanje artikla u katalogu.
 *
 * Otvara se iz {@link ArtikliPanel} klikom na "Novi artikl" ili "Uredi".
 * Nakon uspješnog spremanja, {@link #isSaved()} vraća true i pozivatelj
 * treba osvježiti prikaz liste artikala.
 *
 * Jedino obavezno polje je naziv artikla.
 * Barkod je opcionalan i mora biti jedinstven u cijelom katalogu.
 */
public class ArtiklForm extends JDialog {

    private final JTextField nazivField = new JTextField(20);
    private final JTextField barkodField = new JTextField(20);
    private final JTextField cijenaField = new JTextField(20);
    private final JTextField pdvField = new JTextField("25", 20);
    private final JTextField zalihaField = new JTextField("0", 20);
    private final JList<Dobavljac> dobavljacList = new JList<>();
    private boolean saved = false;
    private final Artikl artikl;

    public ArtiklForm(Frame parent, Artikl artikl) {
        super(parent, artikl == null ? "Novi artikl" : "Uredi artikl", true);
        this.artikl = artikl == null ? new Artikl() : artikl;

        if (artikl != null) {
            nazivField.setText(artikl.getNaziv());
            barkodField.setText(artikl.getBarkod());
            cijenaField.setText(String.valueOf(artikl.getCijena()));
            pdvField.setText(String.valueOf(artikl.getPdvStopa()));
            zalihaField.setText(String.valueOf(artikl.getKolicinaNaSkladistu()));
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        addRow(form, gbc, 0, "Naziv:", nazivField);
        addRow(form, gbc, 1, "Barkod:", barkodField);
        addRow(form, gbc, 2, "Cijena (EUR):", cijenaField);
        addRow(form, gbc, 3, "PDV stopa (%):", pdvField);
        addRow(form, gbc, 4, "Zaliha:", zalihaField);

        // Dobavljači — višestruki odabir
        try {
            List<Dobavljac> svi = DatabaseManager.getInstance().getDobavljaci();
            dobavljacList.setListData(svi.toArray(new Dobavljac[0]));
            dobavljacList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            if (artikl != null && artikl.getId() > 0) {
                List<Dobavljac> vezani = DatabaseManager.getInstance().getDobavljaciZaArtikl(artikl.getId());
                List<Integer> indices = new ArrayList<>();
                for (int i = 0; i < svi.size(); i++) {
                    final int idx = i;
                    if (vezani.stream().anyMatch(d -> d.getId() == svi.get(idx).getId()))
                        indices.add(i);
                }
                dobavljacList.setSelectedIndices(indices.stream().mapToInt(Integer::intValue).toArray());
            }
        } catch (SQLException ignored) {}
        JScrollPane dobScroll = new JScrollPane(dobavljacList);
        dobScroll.setPreferredSize(new Dimension(200, 80));
        addRow(form, gbc, 5, "Dobavljači:", dobScroll);

        JButton spremiBtn = new JButton("Spremi");
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

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private void spremi() {
        if (!Validator.requireNonEmpty(this, nazivField.getText(), "Naziv")) return;

        Double cijena = Validator.parseDecimal(this, cijenaField.getText(), "Cijena", true, false);
        if (cijena == null) return;
        Double pdv = Validator.parseDecimal(this, pdvField.getText(), "PDV stopa", true, false);
        if (pdv == null) return;
        Integer zaliha = Validator.parseInt(this, zalihaField.getText(), "Zaliha", 0);
        if (zaliha == null) return;

        artikl.setNaziv(nazivField.getText().trim());
        artikl.setBarkod(barkodField.getText().trim().isEmpty() ? null : barkodField.getText().trim());
        artikl.setCijena(cijena);
        artikl.setPdvStopa(pdv);
        artikl.setKolicinaNaSkladistu(zaliha);

        try {
            DatabaseManager db = DatabaseManager.getInstance();
            db.saveArtikl(artikl);
            // Spremi veze s dobavljačima
            List<Integer> odabraniIds = new ArrayList<>();
            for (Dobavljac d : dobavljacList.getSelectedValuesList())
                odabraniIds.add(d.getId());
            db.setDobavljaciZaArtikl(artikl.getId(), odabraniIds);
            saved = true;
            dispose();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Greška pri spremanju: " + e.getMessage());
        }
    }

    public boolean isSaved() { return saved; }
}
