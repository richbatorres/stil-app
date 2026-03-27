package stil.app.ui;

import org.junit.jupiter.api.*;
import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testovi za FontManager — globalni zoom i propagacija fonta.
 * Pokreću se u headless modu (bez ekrana), testiraju logiku bez renderinga.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FontManagerTest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    @Order(1)
    void defaultVelicina_je20() {
        assertEquals(20f, FontManager.getTrenutna(), 0.01f);
    }

    @Test
    @Order(2)
    void postavi_mijenjaTrenutnu() {
        FontManager.postavi(24f, null);
        assertEquals(24f, FontManager.getTrenutna(), 0.01f);
        FontManager.postavi(20f, null);
    }

    @Test
    @Order(3)
    void postavi_clampMin() {
        FontManager.postavi(1f, null);
        assertEquals(FontManager.MIN, FontManager.getTrenutna(), 0.01f);
        FontManager.postavi(20f, null);
    }

    @Test
    @Order(4)
    void postavi_clampMax() {
        FontManager.postavi(999f, null);
        assertEquals(FontManager.MAX, FontManager.getTrenutna(), 0.01f);
        FontManager.postavi(20f, null);
    }

    @Test
    @Order(5)
    void azurirajKomponente_postaviFontNaLabel() {
        JLabel label = new JLabel("test");
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        FontManager.azurirajKomponente(label, 22f);
        assertEquals(22f, label.getFont().getSize2D(), 0.01f);
    }

    @Test
    @Order(6)
    void azurirajKomponente_postaviFontNaTableIRowHeight() {
        JTable table = new JTable(new String[][]{{"a"}}, new String[]{"Kol"});
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        FontManager.azurirajKomponente(table, 20f);
        assertEquals(20f, table.getFont().getSize2D(), 0.01f);
        assertEquals(36, table.getRowHeight()); // 20 * 1.8 = 36
    }

    @Test
    @Order(7)
    void azurirajKomponente_rekurzivnoAzuriraPanel() {
        JPanel panel = new JPanel();
        JButton btn = new JButton("test");
        btn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        panel.add(btn);
        FontManager.azurirajKomponente(panel, 18f);
        assertEquals(18f, btn.getFont().getSize2D(), 0.01f);
    }

    @Test
    @Order(8)
    void azurirajKomponente_nullFontSePreskace() {
        JLabel label = new JLabel("test");
        label.setFont(null);
        assertDoesNotThrow(() -> FontManager.azurirajKomponente(label, 20f));
    }
}
