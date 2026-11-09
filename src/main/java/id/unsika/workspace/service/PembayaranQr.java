package id.unsika.workspace.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * Pembayaran lewat QR code. Ini SIMULASI untuk tugas: tidak ada uang sungguhan yang berpindah.
 *
 * Alurnya:
 * 1. Aplikasi membuat kode tagihan acak dan link ke halaman bayar di website (Vercel).
 *    Link itu digambar menjadi QR oleh PanelQr.
 * 2. Pelanggan scan QR, halaman bayar terbuka di HP-nya, lalu ia menekan "Ya" atau "Tidak".
 * 3. Halaman bayar mengirim pesan "LUNAS" / "BATAL" ke ntfy.sh, layanan pesan gratis,
 *    dengan nama topik = kode tagihan.
 * 4. Aplikasi menanyakan topik itu ke ntfy.sh setiap beberapa detik (cekStatus) sampai jawabannya datang.
 *
 * Perantara dibutuhkan karena HP pelanggan tidak bisa menghubungi laptop kasir secara langsung lewat internet.
 */
public class PembayaranQr {

    public enum Status { MENUNGGU, LUNAS, DITOLAK }

    /** Alamat bisa diganti saat menjalankan, misalnya -Dbayar.halaman=... (dipakai waktu uji coba). */
    private static final String HALAMAN = System.getProperty("bayar.halaman",
            "https://project-pbo-group8-web.vercel.app/bayar.html");
    private static final String PERANTARA = System.getProperty("bayar.perantara", "https://ntfy.sh");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** Kode acak yang sulit ditebak, supaya orang lain tidak bisa "membayar" tagihan ini. */
    private final String kode = "wsrent-" + UUID.randomUUID().toString().replace("-", "");

    public String getKode() {
        return kode;
    }

    /** Link yang dijadikan QR. Rincian tagihan ikut di link supaya halaman bayar bisa menampilkannya. */
    public String link(String fasilitas, String pelanggan, String durasi, double total) {
        return HALAMAN + "?id=" + kode
                + "&f=" + encode(fasilitas)
                + "&n=" + encode(pelanggan)
                + "&d=" + encode(durasi)
                + "&t=" + Math.round(total);
    }

    /** Bertanya ke perantara apakah HP pelanggan sudah menjawab. Jalankan di luar thread GUI karena butuh internet. */
    public Status cekStatus() throws IOException, InterruptedException {
        HttpRequest permintaan = HttpRequest.newBuilder(URI.create(PERANTARA + "/" + kode + "/json?poll=1&since=all"))
                .timeout(Duration.ofSeconds(8))
                .build();
        HttpResponse<String> jawaban = HTTP.send(permintaan, HttpResponse.BodyHandlers.ofString());
        if (jawaban.statusCode() != 200) {
            throw new IOException("Server pembayaran menjawab kode " + jawaban.statusCode());
        }
        return bacaStatus(jawaban.body());
    }

    /**
     * Membaca jawaban ntfy.sh: satu baris JSON untuk setiap pesan, contohnya
     * {"id":"x1","time":1700000000,"event":"message","topic":"wsrent-...","message":"LUNAS"}.
     * LUNAS selalu menang, walaupun sebelumnya pelanggan sempat menekan "Tidak".
     */
    static Status bacaStatus(String isi) {
        Status hasil = Status.MENUNGGU;
        for (String baris : isi.split("\n")) {
            if (baris.contains("\"message\":\"LUNAS\"")) {
                return Status.LUNAS;
            }
            if (baris.contains("\"message\":\"BATAL\"")) {
                hasil = Status.DITOLAK;
            }
        }
        return hasil;
    }

    private static String encode(String teks) {
        return URLEncoder.encode(teks, StandardCharsets.UTF_8);
    }
}
