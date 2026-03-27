package stil.app.ui;

import stil.app.db.DatabaseManager;
import stil.app.model.Zaklucnica;
import stil.app.print.IspisIzvjestaja;
import javax.swing.*;
import java.awt.*;

/**
 * Glavni prozor aplikacije STIL A j.d.o.o. blagajne.
 *
 * Sadrži JTabbedPane s tabovima:
 * - Prodaja     — glavni prodajni ekran s košaricom i naplatom
 * - Artikli     — pregled i upravljanje katalogom artikala
 * - Dobavljači  — pregled i upravljanje katalogom dobavljača
 * - Nabava      — evidencija nabave robe od dobavljača
 * - Povrat robe — evidencija povrata robe od kupca ili dobavljaču
 * - Izvještaji  — pregled poslovnih izvještaja po vremenskom razdoblju
 * - Postavke    — konfiguracija tvrtke, fiskalizacije i PIN-a
 *
 * U gornjem desnom kutu nalazi se gumb za zaključavanje blagajne.
 * Aplikacija se zaključava i pri pokretanju — traži PIN ako je postavljen.
 *
 * Promjena taba na "Povrat robe" automatski osvježava listu artikala i dobavljača
 * kako bi podaci bili ažurni.
 */
public class MainWindow extends JFrame {

    private final ProdajaPanel    prodajaPanel;
    private final PovratRobePanel povratRobePanel;
    private final NabavaPanel     nabavaPanel;
    private final KomisijaPanel   komisijaPanel;

    public MainWindow() {
        setTitle("STIL A j.d.o.o. — Blagajna");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(950, 620));
        setLocationRelativeTo(null);

        prodajaPanel    = new ProdajaPanel();
        povratRobePanel = new PovratRobePanel();
        nabavaPanel     = new NabavaPanel();
        komisijaPanel   = new KomisijaPanel();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Prodaja",     prodajaPanel);
        tabs.addTab("Artikli",     new ArtikliPanel());
        tabs.addTab("Dobavljači",  new DobavljaciPanel());
        tabs.addTab("Nabava",      nabavaPanel);
        tabs.addTab("Povrat robe", povratRobePanel);
        tabs.addTab("Komisija",    komisijaPanel);
        tabs.addTab("Izvještaji",  new IzvjestajiPanel());
        tabs.addTab("Postavke",    new PostavkePanel());

        // Osvježi podatke kad se aktiviraju tabovi koji ovise o svježim podacima
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == povratRobePanel) {
                povratRobePanel.ucitajPodatke();
            } else if (tabs.getSelectedComponent() == nabavaPanel) {
                nabavaPanel.ucitajPodatke();
            } else if (tabs.getSelectedComponent() == komisijaPanel) {
                komisijaPanel.ucitajDobavljace();
            }
        });

        JButton zakljucajBtn = new JButton("🔒 Zaključaj");
        zakljucajBtn.setFocusable(false);
        zakljucajBtn.addActionListener(e -> zakljucaj());

        JButton zaklucnicaBtn = new JButton("📄 Zaključnica");
        zaklucnicaBtn.setFocusable(false);
        zaklucnicaBtn.addActionListener(e -> generirajZaklucnicu());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        btnPanel.setOpaque(false);
        btnPanel.add(zaklucnicaBtn);
        btnPanel.add(zakljucajBtn);

        setLayout(new BorderLayout());
        add(btnPanel, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        zakljucaj();
    }

    /**
     * Generira dnevnu zaključnicu, sprema u bazu i PDF, ispisuje.
     */
    private void generirajZaklucnicu() {
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            Zaklucnica z = db.generirajZaklucnicu(java.time.LocalDate.now());

            // Spremi PDF
            java.nio.file.Path pdf = IspisIzvjestaja.spremiZaklucnicuPdf(z, "zaklucnice");
            z.setPutanjaPdf(pdf.toString());

            // Spremi u bazu
            db.saveZaklucnica(z);

            // Prikaži podatke korisniku
            String poruka = String.format(
                "DNEVNA ZAKLJUČNICA%n" +
                "Dan: %s%n%n" +
                "Broj računa:   %d%n" +
                "Ukupno:       %.2f EUR%n" +
                "Gotovina:     %.2f EUR%n" +
                "Kartica:      %.2f EUR%n%n" +
                "PDF spremljen u:%n%s",
                z.getDanOd().toLocalDate(),
                z.getBrojRacuna(), z.getUkupnoEur(),
                z.getGotovinaEur(), z.getKarticaEur(),
                pdf);
            JOptionPane.showMessageDialog(this, poruka, "Zaključnica", JOptionPane.INFORMATION_MESSAGE);

            // Ispis
            String greška = IspisIzvjestaja.ispisiPdf(pdf);
            if (greška != null) {
                JOptionPane.showMessageDialog(this,
                    "Zaključnica je spremljena.\nIspis nije moguć.\nRazlog: " + greška,
                    "Ispis nije uspio", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Greška pri generiranju zaključnice: " + e.getMessage(),
                "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Zaključava blagajnu prikazivanjem LockScreen dijaloga.
     * Ako korisnik ne unese ispravan PIN (ili zatvori dialog), aplikacija se gasi.
     */
    private void zakljucaj() {
        LockScreen lock = new LockScreen(this);
        lock.setVisible(true);
        if (!lock.isUnlocked()) {
            System.exit(0);
        }
    }
}
