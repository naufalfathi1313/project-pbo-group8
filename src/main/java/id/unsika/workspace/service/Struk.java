package id.unsika.workspace.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import id.unsika.workspace.model.Pemesanan;
import id.unsika.workspace.util.Format;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Struk sewa: teksnya, file PDF-nya, dan link WhatsApp ke nomor pelanggan.
 *
 * Catatan: link wa.me hanya bisa membawa TEKS, tidak bisa melampirkan file.
 * Jadi PDF disimpan di folder "Struk-Workspace", lalu kasir melampirkannya sendiri di chat WhatsApp.
 */
public final class Struk {

    /** Folder tempat PDF struk disimpan: di folder home pengguna (Mac & Windows). */
    public static final File FOLDER = new File(System.getProperty("user.home"), "Struk-Workspace");

    private Struk() {
    }

    public static String teks(Pemesanan p, String metode) {
        return "===== STRUK SEWA =====\n"
                + "Pelanggan : " + p.getPelanggan().getNama() + "\n"
                + "Fasilitas : " + p.getFasilitas() + "\n"
                + "Tarif     : " + Format.rupiah(p.getFasilitas().getTarifPerJam()) + " / jam\n"
                + "Mulai     : " + Format.waktu(p.getWaktuMulai()) + "\n"
                + "Selesai   : " + Format.waktu(p.getWaktuSelesai()) + "\n"
                + "Durasi    : " + Format.durasi(p.getDurasiDetik()) + "\n"
                + "TOTAL     : " + Format.rupiah(p.getTotalBiaya()) + "\n"
                + "Dibayar   : " + metode;
    }

    /** Menyimpan struk sebagai PDF (ukuran kertas struk 80 mm) dan mengembalikan file-nya. */
    public static File simpanPdf(Pemesanan p, String metode) throws IOException {
        FOLDER.mkdirs();
        File file = new File(FOLDER, "struk-" + p.getId() + ".pdf");
        Document doc = new Document(new Rectangle(227, 340), 14, 14, 18, 18); // 80 mm x 120 mm
        try (FileOutputStream out = new FileOutputStream(file)) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font huruf = FontFactory.getFont(FontFactory.COURIER, 8); // huruf rata lebar supaya kolom lurus
            doc.add(new Paragraph("WORKSPACE & PC GAMING\n\n", FontFactory.getFont(FontFactory.COURIER_BOLD, 10)));
            doc.add(new Paragraph(teks(p, metode) + "\n\nTerima kasih!", huruf));
            doc.close();
        }
        return file;
    }

    /** Link wa.me ke nomor pelanggan, dengan isi struk sudah terketik di kolom chat. */
    public static String linkWa(String noHp, String teks) {
        return "https://wa.me/" + nomorWa(noHp) + "?text=" + URLEncoder.encode(teks, StandardCharsets.UTF_8)
                .replace("+", "%20"); // spasi harus %20, bukan "+"
    }

    /** wa.me butuh kode negara tanpa "+" dan tanpa 0 di depan: 0812... / +62812... menjadi 62812... */
    static String nomorWa(String noHp) {
        String angka = noHp.replaceAll("[^0-9]", "");
        return angka.startsWith("0") ? "62" + angka.substring(1) : angka;
    }
}
