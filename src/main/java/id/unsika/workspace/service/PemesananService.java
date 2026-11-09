package id.unsika.workspace.service;

import id.unsika.workspace.dao.FasilitasDao;
import id.unsika.workspace.dao.PemesananDao;
import id.unsika.workspace.model.Fasilitas;
import id.unsika.workspace.model.Pelanggan;
import id.unsika.workspace.model.Pemesanan;
import id.unsika.workspace.model.StatusFasilitas;

import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Logika bisnis pemesanan. GUI cukup memanggil method di sini, tidak perlu tahu
 * tabel mana saja yang harus diubah atau aturan apa yang harus dicek.
 */
public class PemesananService {

    /** Jam buka & tutup, dipakai juga oleh grid Jadwal. */
    public static final int JAM_BUKA = 8;
    public static final int JAM_TUTUP = 23;

    private final PemesananDao pemesananDao = new PemesananDao();
    private final FasilitasDao fasilitasDao = new FasilitasDao();

    /** Booking slot jadwal. Fasilitas belum berubah status karena belum dipakai. */
    public Pemesanan pesanJadwal(Pelanggan pelanggan, Fasilitas fasilitas,
                                 LocalDateTime mulai, LocalDateTime selesai) throws SQLException {
        if (selesai.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Slot yang dipilih sudah lewat");
        }
        Fasilitas terbaru = ambilTerbaru(fasilitas);
        if (terbaru.getStatus() == StatusFasilitas.MAINTENANCE) {
            throw new IllegalStateException(fasilitas.getKode() + " sedang maintenance");
        }
        if (pemesananDao.adaBentrok(fasilitas.getId(), mulai, selesai)) {
            throw new IllegalStateException("Slot tersebut baru saja dibooking orang lain. Pilih jam lain.");
        }
        Pemesanan p = new Pemesanan(pelanggan, terbaru, mulai, selesai);
        pemesananDao.simpan(p);
        return p;
    }

    /** Walk-in: pelanggan datang tanpa booking dan langsung main. */
    public Pemesanan mulaiSekarang(Pelanggan pelanggan, Fasilitas fasilitas) throws SQLException {
        LocalDateTime sekarang = LocalDateTime.now();
        if (pemesananDao.adaBentrok(fasilitas.getId(), sekarang, sekarang.plusMinutes(1))) {
            throw new IllegalStateException(fasilitas.getKode() + " sedang masuk jadwal booking pelanggan lain");
        }
        Fasilitas terbaru = ambilTerbaru(fasilitas);
        if (!terbaru.bisaDipesan()) {
            throw new IllegalStateException(fasilitas.getKode() + " sudah tidak tersedia");
        }
        Pemesanan p = new Pemesanan(pelanggan, terbaru);
        p.mulai(sekarang);
        simpan(p);
        return p;
    }

    /** Pelanggan yang sudah booking datang: timer dimulai. */
    public void checkIn(Pemesanan p) throws SQLException {
        Fasilitas terbaru = fasilitasDao.cariById(p.getFasilitas().getId());
        if (terbaru == null || !terbaru.bisaDipesan()) {
            throw new IllegalStateException(p.getFasilitas().getKode() + " masih dipakai / maintenance. Tunggu sampai tersedia.");
        }
        p.mulai(LocalDateTime.now());
        simpan(p);
    }

    /** Sesi selesai: biaya dihitung oleh fasilitas (polymorphism), status kembali TERSEDIA. */
    public void selesaikan(Pemesanan p) throws SQLException {
        selesaikan(p, LocalDateTime.now());
    }

    /** OVERLOADING: versi dengan jam selesai tertentu, dipakai saat tagihan sudah dibekukan di dialog pembayaran. */
    public void selesaikan(Pemesanan p, LocalDateTime waktuSelesai) throws SQLException {
        p.selesaikan(waktuSelesai);
        simpan(p);
    }

    public void batalkan(Pemesanan p) throws SQLException {
        p.batalkan();
        pemesananDao.simpan(p);
    }

    private void simpan(Pemesanan p) throws SQLException {
        pemesananDao.simpan(p);
        fasilitasDao.updateStatus(p.getFasilitas().getId(), p.getFasilitas().getStatus());
    }

    /** Ambil data terbaru dari database, jaga-jaga statusnya baru saja diubah dari komputer lain. */
    private Fasilitas ambilTerbaru(Fasilitas fasilitas) throws SQLException {
        Fasilitas terbaru = fasilitasDao.cariById(fasilitas.getId());
        if (terbaru == null) {
            throw new IllegalStateException(fasilitas.getKode() + " sudah dihapus");
        }
        return terbaru;
    }
}
