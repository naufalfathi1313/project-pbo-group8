package id.unsika.workspace.util;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Kumpulan fungsi kecil untuk menampilkan angka dan waktu dalam format Indonesia. */
public final class Format {

    private static final DateTimeFormatter WAKTU = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Format() {
    }

    /** 12000 -> "Rp12.000" */
    public static String rupiah(double nilai) {
        NumberFormat nf = NumberFormat.getIntegerInstance(new Locale("id", "ID"));
        return "Rp" + nf.format(Math.round(nilai));
    }

    /** Angka ringkas untuk sumbu grafik: 25000 -> "25rb", 1500000 -> "1,5jt". */
    public static String ringkas(double nilai) {
        if (nilai >= 1_000_000) {
            return trim(nilai / 1_000_000) + "jt";
        }
        if (nilai >= 1_000) {
            return trim(nilai / 1_000) + "rb";
        }
        return String.valueOf(Math.round(nilai));
    }

    private static String trim(double x) {
        return (x == Math.rint(x)) ? String.valueOf((long) x) : String.format(new Locale("id", "ID"), "%.1f", x);
    }

    /** 3725 detik -> "01:02:05" */
    public static String durasi(long detik) {
        return String.format("%02d:%02d:%02d", detik / 3600, (detik % 3600) / 60, detik % 60);
    }

    public static String waktu(LocalDateTime waktu) {
        return waktu == null ? "-" : waktu.format(WAKTU);
    }
}
