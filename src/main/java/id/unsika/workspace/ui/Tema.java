package id.unsika.workspace.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import id.unsika.workspace.model.StatusFasilitas;

import java.awt.Color;
import java.util.Map;

/** Semua warna aplikasi di satu tempat, supaya tema gelap konsisten di setiap panel. */
public final class Tema {

    public static final Color LATAR = new Color(0x0B0D1F);       // navy sangat gelap
    public static final Color AKSEN = new Color(0x7C4DFF);       // ungu neon
    public static final Color TEKS = new Color(0xE6E8F2);
    public static final Color TEKS_REDUP = new Color(0x8A8FB0);
    public static final Color HIJAU = new Color(0x1E8E5A);
    public static final Color KUNING = new Color(0xF2B705);
    public static final Color MERAH = new Color(0xE5484D);
    public static final Color ABU = new Color(0x4A5068);
    public static final Color LEWAT = new Color(0x1A1D33);

    private Tema() {
    }

    /** Dipanggil sekali di App.main sebelum jendela pertama dibuat. */
    public static void pasang() {
        FlatLaf.setGlobalExtraDefaults(Map.of(
                "@background", "#0B0D1F",
                "@accentColor", "#7C4DFF",
                "@foreground", "#E6E8F2"));
        FlatDarkLaf.setup();
    }

    public static Color warna(StatusFasilitas status) {
        return switch (status) {
            case TERSEDIA -> HIJAU;
            case DIPESAN -> KUNING;
            case DIPAKAI -> MERAH;
            case MAINTENANCE -> ABU;
        };
    }

    /** Teks gelap di atas kuning, teks terang di atas warna lain, supaya tetap terbaca. */
    public static Color teksDiAtas(Color latar) {
        return latar.equals(KUNING) ? new Color(0x1A1A1A) : Color.WHITE;
    }
}
