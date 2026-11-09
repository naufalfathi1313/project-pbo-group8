package id.unsika.workspace.dao;

import id.unsika.workspace.db.Koneksi;
import id.unsika.workspace.model.Fasilitas;
import id.unsika.workspace.model.Pelanggan;
import id.unsika.workspace.model.Pemesanan;
import id.unsika.workspace.model.StatusPemesanan;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PemesananDao implements Dao<Pemesanan> {

    private final FasilitasDao fasilitasDao = new FasilitasDao();
    private final PelangganDao pelangganDao = new PelangganDao();

    @Override
    public List<Pemesanan> semua() throws SQLException {
        return query("SELECT * FROM pemesanan ORDER BY waktu_pesan DESC");
    }

    /** Pemesanan yang belum selesai (DIPESAN atau AKTIF), untuk tab Sesi Aktif. */
    public List<Pemesanan> berjalan() throws SQLException {
        return query("SELECT * FROM pemesanan WHERE status IN ('DIPESAN','AKTIF') "
                + "ORDER BY status, COALESCE(jadwal_mulai, waktu_mulai)");
    }

    /** Transaksi SELESAI di antara dua tanggal (inklusif), untuk tab Rekap. */
    public List<Pemesanan> rekap(LocalDate dari, LocalDate sampai) throws SQLException {
        return query("SELECT * FROM pemesanan WHERE status = 'SELESAI' AND waktu_selesai >= ? AND waktu_selesai < ? "
                        + "ORDER BY waktu_selesai DESC",
                Timestamp.valueOf(dari.atStartOfDay()), Timestamp.valueOf(sampai.plusDays(1).atStartOfDay()));
    }

    /** Booking (DIPESAN/AKTIF) yang jadwalnya jatuh pada tanggal tertentu, untuk grid Jadwal. */
    public List<Pemesanan> jadwalPada(LocalDate tanggal) throws SQLException {
        return query("SELECT * FROM pemesanan WHERE status IN ('DIPESAN','AKTIF') AND jadwal_mulai < ? AND jadwal_selesai > ?",
                Timestamp.valueOf(tanggal.plusDays(1).atStartOfDay()), Timestamp.valueOf(tanggal.atStartOfDay()));
    }

    /** true jika fasilitas sudah punya booking aktif yang beririsan dengan rentang [mulai, selesai). */
    public boolean adaBentrok(int fasilitasId, LocalDateTime mulai, LocalDateTime selesai) throws SQLException {
        String sql = "SELECT COUNT(*) FROM pemesanan WHERE fasilitas_id = ? AND status IN ('DIPESAN','AKTIF') "
                + "AND jadwal_mulai < ? AND jadwal_selesai > ?";
        try (PreparedStatement ps = Koneksi.get().prepareStatement(sql)) {
            ps.setInt(1, fasilitasId);
            ps.setTimestamp(2, Timestamp.valueOf(selesai));
            ps.setTimestamp(3, Timestamp.valueOf(mulai));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** ID fasilitas yang saat ini sedang masuk jadwal booking tapi pelanggannya belum check-in. */
    public Set<Integer> fasilitasDipesanSekarang() throws SQLException {
        Set<Integer> hasil = new HashSet<>();
        String sql = "SELECT fasilitas_id FROM pemesanan WHERE status = 'DIPESAN' AND jadwal_mulai <= NOW() AND jadwal_selesai > NOW()";
        try (Statement st = Koneksi.get().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                hasil.add(rs.getInt(1));
            }
        }
        return hasil;
    }

    @Override
    public Pemesanan cariById(int id) throws SQLException {
        List<Pemesanan> hasil = query("SELECT * FROM pemesanan WHERE id = ?", id);
        return hasil.isEmpty() ? null : hasil.get(0);
    }

    @Override
    public void simpan(Pemesanan p) throws SQLException {
        if (p.getId() == 0) {
            String sql = "INSERT INTO pemesanan (pelanggan_id, fasilitas_id, waktu_pesan, waktu_mulai, waktu_selesai, total_biaya, status, "
                    + "jadwal_mulai, jadwal_selesai) VALUES (?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = Koneksi.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, p.getPelanggan().getId());
                ps.setInt(2, p.getFasilitas().getId());
                ps.setTimestamp(3, Timestamp.valueOf(p.getWaktuPesan()));
                ps.setTimestamp(4, toTimestamp(p.getWaktuMulai()));
                ps.setTimestamp(5, toTimestamp(p.getWaktuSelesai()));
                ps.setDouble(6, p.getTotalBiaya());
                ps.setString(7, p.getStatus().name());
                ps.setTimestamp(8, toTimestamp(p.getJadwalMulai()));
                ps.setTimestamp(9, toTimestamp(p.getJadwalSelesai()));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        p.setId(keys.getInt(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE pemesanan SET waktu_mulai = ?, waktu_selesai = ?, total_biaya = ?, status = ? WHERE id = ?";
            try (PreparedStatement ps = Koneksi.get().prepareStatement(sql)) {
                ps.setTimestamp(1, toTimestamp(p.getWaktuMulai()));
                ps.setTimestamp(2, toTimestamp(p.getWaktuSelesai()));
                ps.setDouble(3, p.getTotalBiaya());
                ps.setString(4, p.getStatus().name());
                ps.setInt(5, p.getId());
                ps.executeUpdate();
            }
        }
    }

    @Override
    public void hapus(int id) throws SQLException {
        try (PreparedStatement ps = Koneksi.get().prepareStatement("DELETE FROM pemesanan WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private List<Pemesanan> query(String sql, Object... params) throws SQLException {
        // Baris pemesanan dibaca dulu semua, baru kemudian dicari pelanggan & fasilitasnya.
        List<Object[]> baris = new ArrayList<>();
        try (PreparedStatement ps = Koneksi.get().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    baris.add(new Object[]{
                            rs.getInt("id"), rs.getInt("pelanggan_id"), rs.getInt("fasilitas_id"),
                            toLocal(rs.getTimestamp("waktu_pesan")),
                            toLocal(rs.getTimestamp("jadwal_mulai")), toLocal(rs.getTimestamp("jadwal_selesai")),
                            toLocal(rs.getTimestamp("waktu_mulai")),
                            toLocal(rs.getTimestamp("waktu_selesai")), rs.getDouble("total_biaya"),
                            StatusPemesanan.valueOf(rs.getString("status"))});
                }
            }
        }
        List<Pemesanan> hasil = new ArrayList<>();
        for (Object[] b : baris) {
            Pelanggan pelanggan = pelangganDao.cariById((int) b[1]);
            Fasilitas fasilitas = fasilitasDao.cariById((int) b[2]);
            hasil.add(new Pemesanan((int) b[0], pelanggan, fasilitas, (LocalDateTime) b[3],
                    (LocalDateTime) b[4], (LocalDateTime) b[5], (LocalDateTime) b[6], (LocalDateTime) b[7],
                    (double) b[8], (StatusPemesanan) b[9]));
        }
        return hasil;
    }

    private static Timestamp toTimestamp(LocalDateTime waktu) {
        return waktu == null ? null : Timestamp.valueOf(waktu);
    }

    private static LocalDateTime toLocal(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
