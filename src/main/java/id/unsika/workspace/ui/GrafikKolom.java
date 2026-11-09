package id.unsika.workspace.ui;

import id.unsika.workspace.util.Format;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Grafik kolom (batang tegak) yang digambar sendiri dengan Java2D.
 * Dipakai di Rekap untuk "Pendapatan per hari". Arahkan mouse ke kolom untuk melihat nilainya.
 */
public class GrafikKolom extends JComponent {

    private static final int LEBAR_MAKS_KOLOM = 24;
    private static final int RADIUS = 4;

    private String[] label = new String[0];
    private double[] nilai = new double[0];
    private int hover = -1;

    // Area plot dihitung saat menggambar, dipakai lagi untuk mendeteksi mouse
    private int plotKiri;
    private int plotKanan;

    public GrafikKolom() {
        setPreferredSize(new Dimension(520, 240));
        setToolTipText(""); // mengaktifkan tooltip; isinya ditentukan di getToolTipText(MouseEvent)
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int baru = indeksDi(e.getX());
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

    public void setData(String[] label, double[] nilai) {
        this.label = label;
        this.nilai = nilai;
        this.hover = -1;
        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int i = indeksDi(e.getX());
        return i < 0 ? null : label[i] + "  ·  " + Format.rupiah(nilai[i]);
    }

    private int indeksDi(int x) {
        if (nilai.length == 0 || x < plotKiri || x >= plotKanan) {
            return -1;
        }
        double slot = (plotKanan - plotKiri) / (double) nilai.length;
        return Math.min(nilai.length - 1, (int) ((x - plotKiri) / slot));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font kecil = getFont().deriveFont(11f);
        g2.setFont(kecil);
        FontMetrics fm = g2.getFontMetrics();

        double maks = 0;
        for (double v : nilai) {
            maks = Math.max(maks, v);
        }
        if (maks == 0) {
            g2.setColor(Tema.TEKS_REDUP);
            String kosong = "Belum ada transaksi pada periode ini";
            g2.drawString(kosong, (getWidth() - fm.stringWidth(kosong)) / 2, getHeight() / 2);
            g2.dispose();
            return;
        }

        // Skala sumbu Y dengan angka bulat (0, 25rb, 50rb, ...)
        double langkah = langkahRapi(maks / 4);
        double atasSkala = Math.ceil(maks / langkah) * langkah;
        int jumlahGaris = (int) Math.round(atasSkala / langkah);

        int lebarLabelY = 0;
        for (int i = 0; i <= jumlahGaris; i++) {
            lebarLabelY = Math.max(lebarLabelY, fm.stringWidth(Format.ringkas(i * langkah)));
        }
        plotKiri = lebarLabelY + 10;
        plotKanan = getWidth() - 4;
        int atas = fm.getHeight() + 6;       // ruang untuk label nilai di atas kolom tertinggi
        int dasar = getHeight() - fm.getHeight() - 8;
        int tinggiPlot = dasar - atas;

        // Garis bantu horizontal + label sumbu Y
        for (int i = 0; i <= jumlahGaris; i++) {
            int y = dasar - (int) Math.round(i * langkah / atasSkala * tinggiPlot);
            g2.setColor(Tema.GARIS);
            g2.drawLine(plotKiri, y, plotKanan, y);
            g2.setColor(Tema.TEKS_SAMAR);
            String t = Format.ringkas(i * langkah);
            g2.drawString(t, plotKiri - 8 - fm.stringWidth(t), y + fm.getAscent() / 2 - 1);
        }

        double slot = (plotKanan - plotKiri) / (double) nilai.length;
        int lebarKolom = (int) Math.max(4, Math.min(LEBAR_MAKS_KOLOM, slot * 0.6));
        int setiapLabel = Math.max(1, (int) Math.ceil(fm.stringWidth("00/00") * 1.6 / slot));
        int indeksMaks = 0;
        for (int i = 1; i < nilai.length; i++) {
            if (nilai[i] > nilai[indeksMaks]) {
                indeksMaks = i;
            }
        }

        for (int i = 0; i < nilai.length; i++) {
            int tengah = plotKiri + (int) Math.round(slot * i + slot / 2);
            int x = tengah - lebarKolom / 2;
            int h = (int) Math.round(nilai[i] / atasSkala * tinggiPlot);

            if (i == hover) {  // sorotan lembut di belakang kolom yang ditunjuk mouse
                g2.setColor(new java.awt.Color(255, 255, 255, 14));
                g2.fillRect(plotKiri + (int) (slot * i), atas, (int) Math.ceil(slot), tinggiPlot);
            }
            if (h > 0) {
                // Ujung atas membulat, dasar tetap rata: gambar rounded rect lalu potong di garis dasar
                Graphics2D gk = (Graphics2D) g2.create();
                gk.clip(new Rectangle(x, dasar - h, lebarKolom, h));
                gk.setColor(i == hover ? Tema.GRAFIK.brighter() : Tema.GRAFIK);
                gk.fillRoundRect(x, dasar - h, lebarKolom, h + RADIUS * 2, RADIUS * 2, RADIUS * 2);
                gk.dispose();
            }
            // Label nilai hanya untuk kolom tertinggi (sisanya lewat tooltip)
            if (i == indeksMaks) {
                String v = Format.ringkas(nilai[i]);
                g2.setColor(Tema.TEKS);
                g2.drawString(v, tengah - fm.stringWidth(v) / 2, dasar - h - 6);
            }
            if (i % setiapLabel == 0 || i == nilai.length - 1 && setiapLabel == 1) {
                g2.setColor(Tema.TEKS_REDUP);
                g2.drawString(label[i], tengah - fm.stringWidth(label[i]) / 2, getHeight() - 4);
            }
        }
        g2.dispose();
    }

    /** Membulatkan jarak antar garis ke 1, 2, 2.5, atau 5 kali kelipatan 10. */
    static double langkahRapi(double kasar) {
        double pangkat = Math.pow(10, Math.floor(Math.log10(kasar)));
        double n = kasar / pangkat;
        double rapi = n <= 1 ? 1 : n <= 2 ? 2 : n <= 2.5 ? 2.5 : n <= 5 ? 5 : 10;
        return rapi * pangkat;
    }
}
