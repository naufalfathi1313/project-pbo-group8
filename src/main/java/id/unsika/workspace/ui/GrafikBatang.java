package id.unsika.workspace.ui;

import id.unsika.workspace.util.Format;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Grafik batang mendatar dengan label di kiri dan nilai di ujung batang.
 * Dipakai di Rekap untuk "Pendapatan per jenis fasilitas".
 */
public class GrafikBatang extends JComponent {

    private static final int TEBAL = 18;
    private static final int JARAK_BARIS = 44;
    private static final int RADIUS = 4;

    private String[] label = new String[0];
    private double[] nilai = new double[0];
    private String[] keterangan = new String[0];
    private int hover = -1;

    public GrafikBatang() {
        setPreferredSize(new Dimension(320, 3 * JARAK_BARIS + 8));
        setToolTipText("");
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int baru = barisDi(e.getY());
                if (baru != hover) {
                    hover = baru;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    /** keterangan: teks kecil di bawah label, misalnya "12 transaksi". */
    public void setData(String[] label, double[] nilai, String[] keterangan) {
        this.label = label;
        this.nilai = nilai;
        this.keterangan = keterangan;
        setPreferredSize(new Dimension(320, label.length * JARAK_BARIS + 8));
        revalidate();
        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int i = barisDi(e.getY());
        return i < 0 ? null : label[i] + "  ·  " + Format.rupiah(nilai[i]) + "  ·  " + keterangan[i];
    }

    private int barisDi(int y) {
        int i = (y - 4) / JARAK_BARIS;
        return (y < 4 || i >= nilai.length) ? -1 : i;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(getFont().deriveFont(12f));
        FontMetrics fm = g2.getFontMetrics();

        double maks = 0;
        int lebarLabel = 0;
        int lebarNilai = 0;
        for (int i = 0; i < nilai.length; i++) {
            maks = Math.max(maks, nilai[i]);
            lebarLabel = Math.max(lebarLabel, fm.stringWidth(label[i]));
            lebarNilai = Math.max(lebarNilai, fm.stringWidth(Format.rupiah(nilai[i])));
        }
        int kiri = lebarLabel + 14;
        int panjangMaks = getWidth() - kiri - lebarNilai - 12;

        for (int i = 0; i < nilai.length; i++) {
            int yAtas = 4 + i * JARAK_BARIS;
            int yBatang = yAtas + (JARAK_BARIS - TEBAL) / 2 - 4;
            if (i == hover) {
                g2.setColor(new java.awt.Color(255, 255, 255, 14));
                g2.fillRoundRect(0, yAtas, getWidth(), JARAK_BARIS - 4, 8, 8);
            }
            g2.setColor(Tema.TEKS);
            g2.drawString(label[i], 4, yBatang + TEBAL / 2 + fm.getAscent() / 2 - 2);
            g2.setColor(Tema.TEKS_SAMAR);
            g2.setFont(getFont().deriveFont(10.5f));
            g2.drawString(keterangan[i], 4, yBatang + TEBAL + 12);
            g2.setFont(getFont().deriveFont(12f));

            // Lintasan tipis sebagai latar, lalu batang nilai di atasnya
            g2.setColor(Tema.GARIS);
            g2.fillRoundRect(kiri, yBatang + TEBAL / 2 - 1, Math.max(panjangMaks, 0), 2, 2, 2);
            int panjang = maks == 0 ? 0 : (int) Math.round(nilai[i] / maks * panjangMaks);
            if (panjang > 0) {
                Graphics2D gb = (Graphics2D) g2.create();
                gb.clip(new Rectangle(kiri, yBatang, panjang, TEBAL));
                gb.setColor(i == hover ? Tema.GRAFIK.brighter() : Tema.GRAFIK);
                gb.fillRoundRect(kiri - RADIUS * 2, yBatang, panjang + RADIUS * 2, TEBAL, RADIUS * 2, RADIUS * 2);
                gb.dispose();
            }
            g2.setColor(Tema.TEKS);
            g2.drawString(Format.rupiah(nilai[i]), kiri + panjang + 8, yBatang + TEBAL / 2 + fm.getAscent() / 2 - 2);
        }
        g2.dispose();
    }
}
