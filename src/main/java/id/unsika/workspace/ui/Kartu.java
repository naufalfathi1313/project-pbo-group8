package id.unsika.workspace.ui;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/** Panel bersudut bulat. Dasar dari semua "kartu" di aplikasi (stat, fasilitas, grafik). */
public class Kartu extends JPanel {

    private static final int RADIUS = 16;
    private Color garisTepi = Tema.GARIS;

    public Kartu(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBackground(Tema.KARTU);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
    }

    public void setGarisTepi(Color warna) {
        this.garisTepi = warna;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);
        if (garisTepi != null) {
            g2.setColor(garisTepi);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
