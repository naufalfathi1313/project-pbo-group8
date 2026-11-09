package id.unsika.workspace.model;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Satu transaksi sewa: siapa (Pelanggan) menyewa apa (Fasilitas) dan kapan.
 * Pemesanan TIDAK peduli jenis fasilitasnya. Ia cukup memanggil fasilitas.hitungBiaya(),
 * dan Java otomatis menjalankan versi milik MejaDesk / MeetingRoom / PCGaming (POLYMORPHISM).
 *
 * Ada dua cara memesan:
 * - Walk-in  : pelanggan datang dan langsung main. Tidak punya jadwal.
 * - Booking  : pelanggan memilih slot jam di grid Jadwal. jadwalMulai & jadwalSelesai terisi.
 * Biaya akhir SELALU dihitung dari timer (waktu check-in sampai selesai), bukan dari jadwal.
 */
public class Pemesanan {

    private int id;
    private final Pelanggan pelanggan;
    private final Fasilitas fasilitas;
    private final LocalDateTime waktuPesan;
    private final LocalDateTime jadwalMulai;
    private final LocalDateTime jadwalSelesai;
    private LocalDateTime waktuMulai;
    private LocalDateTime waktuSelesai;
    private double totalBiaya;
    private StatusPemesanan status;

    /** Konstruktor untuk walk-in (tanpa jadwal). */
    public Pemesanan(Pelanggan pelanggan, Fasilitas fasilitas) {
        this(pelanggan, fasilitas, null, null);
    }

    /** Konstruktor untuk booking slot jadwal. */
    public Pemesanan(Pelanggan pelanggan, Fasilitas fasilitas, LocalDateTime jadwalMulai, LocalDateTime jadwalSelesai) {
        this(0, pelanggan, fasilitas, LocalDateTime.now(), jadwalMulai, jadwalSelesai,
                null, null, 0, StatusPemesanan.DIPESAN);
        if (jadwalMulai != null && !jadwalSelesai.isAfter(jadwalMulai)) {
            throw new IllegalArgumentException("Jam selesai harus setelah jam mulai");
        }
    }

    /** Konstruktor lengkap, dipakai saat membaca data dari database. */
    public Pemesanan(int id, Pelanggan pelanggan, Fasilitas fasilitas, LocalDateTime waktuPesan,
                     LocalDateTime jadwalMulai, LocalDateTime jadwalSelesai,
                     LocalDateTime waktuMulai, LocalDateTime waktuSelesai,
                     double totalBiaya, StatusPemesanan status) {
        this.id = id;
        this.pelanggan = pelanggan;
        this.fasilitas = fasilitas;
        this.waktuPesan = waktuPesan;
        this.jadwalMulai = jadwalMulai;
        this.jadwalSelesai = jadwalSelesai;
        this.waktuMulai = waktuMulai;
        this.waktuSelesai = waktuSelesai;
        this.totalBiaya = totalBiaya;
        this.status = status;
    }

    /** Pelanggan datang / mulai sekarang: timer mulai berjalan. */
    public void mulai(LocalDateTime waktu) {
        if (status != StatusPemesanan.DIPESAN) {
            throw new IllegalStateException("Hanya pemesanan berstatus DIPESAN yang bisa dimulai");
        }
        waktuMulai = waktu;
        status = StatusPemesanan.AKTIF;
        fasilitas.tandaiDipakai();
    }

    /** Sesi selesai: timer berhenti, biaya dihitung, fasilitas kembali tersedia. */
    public void selesaikan(LocalDateTime waktu) {
        if (status != StatusPemesanan.AKTIF) {
            throw new IllegalStateException("Hanya sesi AKTIF yang bisa diselesaikan");
        }
        waktuSelesai = waktu;
        totalBiaya = fasilitas.hitungBiaya(getDurasiMenit());
        status = StatusPemesanan.SELESAI;
        fasilitas.tandaiTersedia();
    }

    /** Membatalkan booking yang belum check-in. Status fasilitas tidak berubah karena belum dipakai. */
    public void batalkan() {
        if (status != StatusPemesanan.DIPESAN) {
            throw new IllegalStateException("Hanya pemesanan yang belum dimulai yang bisa dibatalkan");
        }
        status = StatusPemesanan.DIBATALKAN;
    }

    public boolean punyaJadwal() {
        return jadwalMulai != null;
    }

    /** true jika jadwal booking ini beririsan dengan rentang waktu [mulai, selesai). */
    public boolean bentrokDengan(LocalDateTime mulai, LocalDateTime selesai) {
        return punyaJadwal() && jadwalMulai.isBefore(selesai) && jadwalSelesai.isAfter(mulai);
    }

    /** Perkiraan biaya sesuai jadwal yang dipilih, ditampilkan sebelum konfirmasi booking. */
    public double getEstimasiBiaya() {
        if (!punyaJadwal()) {
            return 0;
        }
        return fasilitas.hitungBiaya(Duration.between(jadwalMulai, jadwalSelesai).toMinutes());
    }

    /** Lama pemakaian dalam detik. Jika masih berjalan, dihitung sampai detik ini. */
    public long getDurasiDetik() {
        if (waktuMulai == null) {
            return 0;
        }
        LocalDateTime akhir = (waktuSelesai != null) ? waktuSelesai : LocalDateTime.now();
        return Math.max(0, Duration.between(waktuMulai, akhir).getSeconds());
    }

    /** Lama pemakaian dalam menit, dibulatkan ke atas (61 detik = 2 menit), minimal 1 menit. */
    public long getDurasiMenit() {
        if (waktuMulai == null) {
            return 0;
        }
        return Math.max(1, (getDurasiDetik() + 59) / 60);
    }

    /** Biaya jika sesi dihentikan sekarang, untuk ditampilkan live di tab Sesi Aktif. */
    public double getBiayaSementara() {
        return fasilitas.hitungBiaya(getDurasiMenit());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Pelanggan getPelanggan() {
        return pelanggan;
    }

    public Fasilitas getFasilitas() {
        return fasilitas;
    }

    public LocalDateTime getWaktuPesan() {
        return waktuPesan;
    }

    public LocalDateTime getJadwalMulai() {
        return jadwalMulai;
    }

    public LocalDateTime getJadwalSelesai() {
        return jadwalSelesai;
    }

    public LocalDateTime getWaktuMulai() {
        return waktuMulai;
    }

    public LocalDateTime getWaktuSelesai() {
        return waktuSelesai;
    }

    public double getTotalBiaya() {
        return totalBiaya;
    }

    public StatusPemesanan getStatus() {
        return status;
    }
}
