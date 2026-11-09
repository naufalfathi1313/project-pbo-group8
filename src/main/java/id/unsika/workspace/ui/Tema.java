package id.unsika.workspace.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import id.unsika.workspace.model.StatusFasilitas;

import javax.swing.JLabel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.util.Map;

/** Semua warna dan gaya teks aplikasi di satu tempat, supaya tema gelap konsisten di setiap layar. */
public final class Tema {

    // Permukaan, dari yang paling gelap (latar) ke yang paling terang (kartu yang di-hover)
    public static final Color LATAR = new Color(0x0B0D1F);
    public static final Color SIDEBAR = new Color(0x090B1A);
    public static final Color KARTU = new Color(0x141833);
    public static final Color KARTU_HOVER = new Color(0x1B2044);
    public static final Color GARIS = new Color(0x262B52);

    public static final Color AKSEN = new Color(0x7C4DFF);       // ungu: tombol & pilihan
    public static final Color GRAFIK = new Color(0x8B6CFF);      // ungu untuk batang grafik (lolos uji kontras)
    public static final Color TEKS = new Color(0xE6E8F2);
    public static final Color TEKS_REDUP = new Color(0x8A8FB0);
    public static final Color TEKS_SAMAR = new Color(0x5C6080);

    // Warna status: hanya dipakai untuk status, tidak untuk grafik
    public static final Color HIJAU = new Color(0x1E8E5A);
    public static final Color KUNING = new Color(0xF2B705);
    public static final Color MERAH = new Color(0xE5484D);
    public static final Color ABU = new Color(0x4A5068);
    public static final Color LEWAT = new Color(0x161A33);

    private Tema() {
    }

    /** Dipanggil sekali di App.main sebelum jendela pertama dibuat. */
    public static void pasang() {
        FlatInterFont.install();
        FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
        FlatLaf.setGlobalExtraDefaults(Map.of(
                "@background", "#0B0D1F",
                "@accentColor", "#7C4DFF",
                "@foreground", "#E6E8F2"));
        FlatDarkLaf.setup();

        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Table.rowHeight", 34);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.gridColor", GARIS);
        UIManager.put("Table.background", KARTU);
        UIManager.put("Table.alternateRowColor", new Color(0x171B3A));
        UIManager.put("TableHeader.background", SIDEBAR);
        UIManager.put("TableHeader.height", 36);
        UIManager.put("TableHeader.separatorColor", GARIS);
        UIManager.put("TableHeader.bottomSeparatorColor", GARIS);
        UIManager.put("ScrollPane.border", javax.swing.BorderFactory.createLineBorder(GARIS));
        UIManager.put("TabbedPane.selectedBackground", KARTU);
        UIManager.put("OptionPane.background", KARTU);
        UIManager.put("Panel.background", LATAR);
        UIManager.put("ToggleButton.selectedBackground", AKSEN);
        UIManager.put("ToggleButton.selectedForeground", Color.WHITE);
        UIManager.put("ToolTip.background", new Color(0x1F2450));
        UIManager.put("ToolTip.foreground", TEKS);
    }

    /** Tombol aksi utama berwarna aksen (misalnya "Konfirmasi Booking"). */
    public static javax.swing.JButton tombolUtama(String teks) {
        javax.swing.JButton b = new javax.swing.JButton(teks);
        b.setBackground(AKSEN);
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        return b;
    }

    /** Tombol pilihan berbentuk pil, untuk filter (Semua / Meja / Meeting / PC). */
    public static javax.swing.JToggleButton chip(String teks) {
        javax.swing.JToggleButton b = new javax.swing.JToggleButton(teks);
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setFocusable(false);
        return b;
    }

    public static Color warna(StatusFasilitas status) {
        return switch (status) {
            case TERSEDIA -> HIJAU;
            case DIPESAN -> KUNING;
            case DIPAKAI -> MERAH;
            case MAINTENANCE -> ABU;
        };
    }

    public static String labelStatus(StatusFasilitas status) {
        return switch (status) {
            case TERSEDIA -> "Tersedia";
            case DIPESAN -> "Dipesan";
            case DIPAKAI -> "Dipakai";
            case MAINTENANCE -> "Maintenance";
        };
    }

    /** Teks gelap di atas kuning, teks terang di atas warna lain, supaya tetap terbaca. */
    public static Color teksDiAtas(Color latar) {
        return latar.equals(KUNING) ? new Color(0x1A1A1A) : Color.WHITE;
    }

    /** Membuat JLabel dengan ukuran, ketebalan, dan warna tertentu. */
    public static JLabel label(String teks, float ukuran, int gaya, Color warna) {
        JLabel l = new JLabel(teks);
        l.setFont(l.getFont().deriveFont(gaya, ukuran));
        l.setForeground(warna);
        return l;
    }

    public static JLabel judul(String teks) {
        return label(teks, 22f, Font.BOLD, TEKS);
    }
}
