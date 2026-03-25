package stil.app.ui;

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

    public MainWindow() {
        setTitle("STIL A j.d.o.o. — Blagajna");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(950, 620));
        setLocationRelativeTo(null);

        prodajaPanel    = new ProdajaPanel();
        povratRobePanel = new PovratRobePanel();
        nabavaPanel     = new NabavaPanel();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Prodaja",     prodajaPanel);
        tabs.addTab("Artikli",     new ArtikliPanel());
        tabs.addTab("Dobavljači",  new DobavljaciPanel());
        tabs.addTab("Nabava",      nabavaPanel);
        tabs.addTab("Povrat robe", povratRobePanel);
        tabs.addTab("Izvještaji",  new IzvjestajiPanel());
        tabs.addTab("Postavke",    new PostavkePanel());

        // Osvježi podatke kad se aktiviraju tabovi koji ovise o svježim podacima
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == povratRobePanel) {
                povratRobePanel.ucitajPodatke();
            } else if (tabs.getSelectedComponent() == nabavaPanel) {
                nabavaPanel.ucitajPodatke();
            }
        });

        JButton zakljucajBtn = new JButton("🔒 Zaključaj");
        zakljucajBtn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        zakljucajBtn.setFocusable(false);
        zakljucajBtn.addActionListener(e -> zakljucaj());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        btnPanel.setOpaque(false);
        btnPanel.add(zakljucajBtn);

        setLayout(new BorderLayout());
        add(btnPanel, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        zakljucaj();
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
