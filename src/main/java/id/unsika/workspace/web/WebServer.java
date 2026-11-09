package id.unsika.workspace.web;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import id.unsika.workspace.dao.FasilitasDao;
import id.unsika.workspace.dao.PelangganDao;
import id.unsika.workspace.dao.PemesananDao;
import id.unsika.workspace.dao.UserDao;
import id.unsika.workspace.model.Fasilitas;
import id.unsika.workspace.model.Pelanggan;
import id.unsika.workspace.model.Pemesanan;
import id.unsika.workspace.model.StatusPemesanan;
import id.unsika.workspace.service.PembayaranQr;
import id.unsika.workspace.service.PemesananService;
import id.unsika.workspace.util.Format;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VERSI WEB aplikasi kasir: server HTTP kecil yang bisa dibuka dari browser mana saja lewat internet.
 *
 * Server ini TIDAK menulis ulang logika bisnis. Ia memanggil class yang sama dengan versi Swing:
 * UserDao (login), FasilitasDao, PelangganDao, PemesananDao, PemesananService, dan PembayaranQr.
 * Bedanya hanya "tampilan": Swing menggambar jendela, server ini mengirim data JSON ke halaman web.
 *
 * Server memakai com.sun.net.httpserver yang sudah ada di JDK, jadi tidak perlu framework tambahan.
 * Permintaan dilayani satu per satu (satu thread), sehingga aman berbagi satu koneksi database (Koneksi).
 */
public class WebServer {

    /** Tagihan yang sedang menunggu dibayar: jam selesai dibekukan, sama seperti DialogBayar. */
    private record Tagihan(LocalDateTime waktu, PembayaranQr qr, String link) {
    }

    private final UserDao userDao = new UserDao();
    private final FasilitasDao fasilitasDao = new FasilitasDao();
    private final PelangganDao pelangganDao = new PelangganDao();
    private final PemesananDao pemesananDao = new PemesananDao();
    private final PemesananService service = new PemesananService();

    /** token cookie -> nama user yang login */
    private final Map<String, String> sesiLogin = new ConcurrentHashMap<>();
    /** id pemesanan -> tagihan yang dibekukan */
    private final Map<Integer, Tagihan> tagihan = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        java.util.Locale.setDefault(new java.util.Locale("id", "ID"));
        // Di server online (Render), alamat database diberikan lewat environment variable
        salinEnv("DB_URL", "db.url");
        salinEnv("DB_USER", "db.user");
        salinEnv("DB_PASSWORD", "db.password");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new WebServer()::layani);
        server.start();
        System.out.println("Kasir web berjalan di http://localhost:" + port);
    }

    private static void salinEnv(String env, String properti) {
        String nilai = System.getenv(env);
        if (nilai != null && !nilai.isBlank()) {
            System.setProperty(properti, nilai);
        }
    }

    /** Satu pintu untuk semua permintaan: arahkan ke method sesuai alamatnya. */
    private void layani(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        try {
            if (path.equals("/") || path.equals("/index.html")) {
                kirimFile(ex, "/kasir/index.html", "text/html; charset=utf-8");
                return;
            }
            Map<String, String> form = bacaForm(ex);
            if (path.equals("/api/login")) {
                login(ex, form);
                return;
            }
            String user = sesiLogin.get(cookie(ex));
            if (user == null) {
                kirimJson(ex, 401, "{\"error\":\"Silakan login dulu\"}");
                return;
            }
            switch (path) {
                case "/api/saya" -> kirimJson(ex, 200, "{\"nama\":" + str(user) + "}");
                case "/api/logout" -> {
                    sesiLogin.remove(cookie(ex));
                    kirimJson(ex, 200, "{}");
                }
                case "/api/data" -> kirimJson(ex, 200, data());
                case "/api/walkin" -> {
                    service.mulaiSekarang(pelanggan(form), fasilitasDao.cariById(angka(form, "fasilitasId")));
                    kirimJson(ex, 200, "{}");
                }
                case "/api/checkin" -> {
                    service.checkIn(pemesanan(form));
                    kirimJson(ex, 200, "{}");
                }
                case "/api/batal" -> {
                    service.batalkan(pemesanan(form));
                    kirimJson(ex, 200, "{}");
                }
                case "/api/tagih" -> kirimJson(ex, 200, tagih(pemesanan(form)));
                case "/api/status-bayar" -> kirimJson(ex, 200, statusBayar(pemesanan(form)));
                case "/api/tunai" -> {
                    lunas(pemesanan(form));
                    kirimJson(ex, 200, "{}");
                }
                case "/api/qr" -> kirimQr(ex, tagihan.get(angka(form, "id")));
                default -> kirimJson(ex, 404, "{\"error\":\"Alamat tidak ditemukan\"}");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            kirimJson(ex, 400, "{\"error\":" + str(e.getMessage()) + "}");    // aturan bisnis dilanggar
        } catch (SQLException e) {
            kirimJson(ex, 500, "{\"error\":" + str("Database bermasalah: " + e.getMessage()) + "}");
        } catch (Exception e) {
            kirimJson(ex, 500, "{\"error\":" + str(e.toString()) + "}");
        }
    }

    // ------------------------------------------------------------------
    // Fitur
    // ------------------------------------------------------------------

    private void login(HttpExchange ex, Map<String, String> form) throws SQLException, IOException {
        String nama = userDao.login(form.getOrDefault("username", ""), form.getOrDefault("password", ""));
        if (nama == null) {
            kirimJson(ex, 401, "{\"error\":\"Username atau password salah\"}");
            return;
        }
        String token = UUID.randomUUID().toString();
        sesiLogin.put(token, nama);
        // HttpOnly: cookie tidak bisa dibaca JavaScript. Secure: hanya dikirim lewat https (di server online).
        boolean https = "https".equals(ex.getRequestHeaders().getFirst("X-Forwarded-Proto"));
        ex.getResponseHeaders().add("Set-Cookie",
                "sesi=" + token + "; Path=/; HttpOnly; SameSite=Lax" + (https ? "; Secure" : ""));
        kirimJson(ex, 200, "{\"nama\":" + str(nama) + "}");
    }

    /** Semua yang ditampilkan dashboard dalam satu JSON: fasilitas, pelanggan, sesi berjalan, rekap hari ini. */
    private String data() throws SQLException {
        StringBuilder fasilitas = new StringBuilder();
        for (Fasilitas f : fasilitasDao.semua()) {
            pisah(fasilitas).append("{\"id\":").append(f.getId())
                    .append(",\"kode\":").append(str(f.getKode()))
                    .append(",\"nama\":").append(str(f.getNama()))
                    .append(",\"jenis\":").append(str(f.getJenis().getLabel()))
                    .append(",\"tarif\":").append(str(Format.rupiah(f.getTarifPerJam()) + "/jam"))
                    .append(",\"info\":").append(str(f.getInfoTambahan()))
                    .append(",\"status\":").append(str(f.getStatus().name())).append('}');
        }
        StringBuilder pelanggan = new StringBuilder();
        for (Pelanggan p : pelangganDao.semua()) {
            pisah(pelanggan).append("{\"id\":").append(p.getId())
                    .append(",\"nama\":").append(str(p.toString())).append('}');
        }
        StringBuilder sesi = new StringBuilder();
        for (Pemesanan p : pemesananDao.berjalan()) {
            pisah(sesi).append("{\"id\":").append(p.getId())
                    .append(",\"status\":").append(str(p.getStatus().name()))
                    .append(",\"fasilitas\":").append(str(p.getFasilitas().getKode() + " · " + p.getFasilitas().getNama()))
                    .append(",\"pelanggan\":").append(str(p.getPelanggan().getNama()))
                    .append(",\"jadwal\":").append(str(p.punyaJadwal()
                            ? Format.waktu(p.getJadwalMulai()) + " - " + Format.waktu(p.getJadwalSelesai()) : "Walk-in"))
                    .append(",\"mulai\":").append(p.getWaktuMulai() == null ? "null" : epoch(p.getWaktuMulai()))
                    .append(",\"biaya\":").append(str(p.getWaktuMulai() == null ? "-" : Format.rupiah(p.getBiayaSementara())))
                    .append('}');
        }
        List<Pemesanan> hariIni = pemesananDao.rekap(LocalDate.now(), LocalDate.now());
        double pendapatan = 0;
        for (Pemesanan p : hariIni) {
            pendapatan += p.getTotalBiaya();
        }
        return "{\"fasilitas\":[" + fasilitas + "],\"pelanggan\":[" + pelanggan + "],\"sesi\":[" + sesi
                + "],\"transaksiHariIni\":" + hariIni.size()
                + ",\"pendapatanHariIni\":" + str(Format.rupiah(pendapatan)) + "}";
    }

    /** Pelanggan lama (dipilih dari daftar) atau pelanggan baru (nama + no HP diisi). */
    private Pelanggan pelanggan(Map<String, String> form) throws SQLException {
        String id = form.getOrDefault("pelangganId", "");
        if (!id.isBlank()) {
            return pelangganDao.cariById(Integer.parseInt(id));
        }
        Pelanggan baru = new Pelanggan(0, form.get("nama"), form.get("noHp")); // validasi di setter
        pelangganDao.simpan(baru);
        return baru;
    }

    /** Membekukan tagihan, lalu mengirim total + link QR. Sama seperti saat DialogBayar dibuka. */
    private String tagih(Pemesanan p) {
        if (p.getStatus() != StatusPemesanan.AKTIF) {
            throw new IllegalStateException("Hanya sesi yang sedang berjalan yang bisa dibayar. Lakukan check-in dulu.");
        }
        LocalDateTime waktu = LocalDateTime.now();
        String durasi = Format.durasi(p.durasiDetikSampai(waktu));
        double total = p.biayaSampai(waktu);
        String fasilitas = p.getFasilitas().getKode() + " · " + p.getFasilitas().getNama();
        PembayaranQr qr = new PembayaranQr();
        tagihan.put(p.getId(), new Tagihan(waktu, qr, qr.link(fasilitas, p.getPelanggan().getNama(), durasi, total)));
        return "{\"total\":" + str(Format.rupiah(total)) + ",\"durasi\":" + str(durasi)
                + ",\"fasilitas\":" + str(fasilitas) + ",\"pelanggan\":" + str(p.getPelanggan().getNama()) + "}";
    }

    /** Ditanya halaman web setiap 3 detik: sudah dibayar lewat HP atau belum? */
    private String statusBayar(Pemesanan p) throws Exception {
        Tagihan t = tagihan.get(p.getId());
        if (t == null) {
            throw new IllegalStateException("Tagihan belum dibuat");
        }
        PembayaranQr.Status status = t.qr().cekStatus();
        if (status == PembayaranQr.Status.LUNAS) {
            lunas(p);
        }
        return "{\"status\":" + str(status.name()) + "}";
    }

    private void lunas(Pemesanan p) throws SQLException {
        Tagihan t = tagihan.remove(p.getId());
        if (t == null) {
            throw new IllegalStateException("Tagihan belum dibuat");
        }
        service.selesaikan(p, t.waktu());
    }

    private Pemesanan pemesanan(Map<String, String> form) throws SQLException {
        Pemesanan p = pemesananDao.cariById(angka(form, "id"));
        if (p == null) {
            throw new IllegalArgumentException("Pemesanan tidak ditemukan");
        }
        return p;
    }

    /** QR digambar sebagai SVG: satu kotak hitam untuk setiap titik QR. Tidak perlu library gambar tambahan. */
    private void kirimQr(HttpExchange ex, Tagihan t) throws Exception {
        if (t == null) {
            throw new IllegalStateException("Tagihan belum dibuat");
        }
        BitMatrix kotak = new QRCodeWriter().encode(t.link(), BarcodeFormat.QR_CODE, 0, 0, Map.of(EncodeHintType.MARGIN, 1));
        int n = kotak.getWidth();
        StringBuilder svg = new StringBuilder("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 " + n + " " + n
                + "' shape-rendering='crispEdges'><rect width='100%' height='100%' fill='#fff'/><path fill='#000' d='");
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (kotak.get(x, y)) {
                    svg.append('M').append(x).append(' ').append(y).append("h1v1h-1z");
                }
            }
        }
        kirim(ex, 200, "image/svg+xml", svg.append("'/></svg>").toString().getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------
    // Helper HTTP & JSON
    // ------------------------------------------------------------------

    /** Membaca parameter dari alamat (?id=1) dan dari isi form yang dikirim fetch(). */
    private static Map<String, String> bacaForm(HttpExchange ex) throws IOException {
        Map<String, String> hasil = new HashMap<>();
        String query = ex.getRequestURI().getRawQuery();
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        for (String bagian : ((query == null ? "" : query) + "&" + body).split("&")) {
            int sama = bagian.indexOf('=');
            if (sama > 0) {
                hasil.put(decode(bagian.substring(0, sama)), decode(bagian.substring(sama + 1)));
            }
        }
        return hasil;
    }

    private static int angka(Map<String, String> form, String nama) {
        try {
            return Integer.parseInt(form.getOrDefault(nama, ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Data " + nama + " tidak valid");
        }
    }

    private static String cookie(HttpExchange ex) {
        String header = ex.getRequestHeaders().getFirst("Cookie");
        if (header != null) {
            for (String c : header.split(";")) {
                if (c.trim().startsWith("sesi=")) {
                    return c.trim().substring(5);
                }
            }
        }
        return "";
    }

    private static void kirimFile(HttpExchange ex, String resource, String tipe) throws IOException {
        try (InputStream in = WebServer.class.getResourceAsStream(resource)) {
            kirim(ex, 200, tipe, in.readAllBytes());
        }
    }

    private static void kirimJson(HttpExchange ex, int kode, String json) throws IOException {
        kirim(ex, kode, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private static void kirim(HttpExchange ex, int kode, String tipe, byte[] isi) throws IOException {
        ex.getResponseHeaders().set("Content-Type", tipe);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(kode, isi.length);
        ex.getResponseBody().write(isi);
        ex.close();
    }

    /** Teks Java -> teks JSON yang aman (tanda kutip dan karakter khusus di-escape). */
    static String str(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '<' -> sb.append("\\u003c");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    private static StringBuilder pisah(StringBuilder sb) {
        return sb.isEmpty() ? sb : sb.append(',');
    }

    private static long epoch(LocalDateTime waktu) {
        return waktu.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
