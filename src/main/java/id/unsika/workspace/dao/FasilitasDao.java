package id.unsika.workspace.dao;

import id.unsika.workspace.db.Koneksi;
import id.unsika.workspace.model.Fasilitas;
import id.unsika.workspace.model.JenisFasilitas;
import id.unsika.workspace.model.MeetingRoom;
import id.unsika.workspace.model.MejaDesk;
import id.unsika.workspace.model.PCGaming;
import id.unsika.workspace.model.StatusFasilitas;
import id.unsika.workspace.model.TipePC;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class FasilitasDao implements Dao<Fasilitas> {

    @Override
    public List<Fasilitas> semua() throws SQLException {
        List<Fasilitas> hasil = new ArrayList<>();
        try (Statement st = Koneksi.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM fasilitas ORDER BY jenis, kode")) {
            while (rs.next()) {
                hasil.add(map(rs));
            }
        }
        return hasil;
    }

    /** Hanya fasilitas berstatus TERSEDIA, untuk pilihan di form pemesanan. */
    public List<Fasilitas> tersedia() throws SQLException {
        List<Fasilitas> hasil = new ArrayList<>();
        for (Fasilitas f : semua()) {  // for-each
            if (f.bisaDipesan()) {
                hasil.add(f);
            }
        }
        return hasil;
    }

    @Override
    public Fasilitas cariById(int id) throws SQLException {
        try (PreparedStatement ps = Koneksi.get().prepareStatement("SELECT * FROM fasilitas WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    @Override
    public void simpan(Fasilitas f) throws SQLException {
        String sql = (f.getId() == 0)
                ? "INSERT INTO fasilitas (kode, nama, jenis, tarif_per_jam, status, ada_stop_kontak, kapasitas, spesifikasi, tipe_pc) VALUES (?,?,?,?,?,?,?,?,?)"
                : "UPDATE fasilitas SET kode=?, nama=?, jenis=?, tarif_per_jam=?, status=?, ada_stop_kontak=?, kapasitas=?, spesifikasi=?, tipe_pc=? WHERE id=?";
        try (PreparedStatement ps = Koneksi.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, f.getKode());
            ps.setString(2, f.getNama());
            ps.setString(3, f.getJenis().name());
            ps.setDouble(4, f.getTarifPerJam());
            ps.setString(5, f.getStatus().name());
            // Kolom khusus: diisi sesuai jenisnya, sisanya NULL
            ps.setNull(6, Types.BOOLEAN);
            ps.setNull(7, Types.INTEGER);
            ps.setNull(8, Types.VARCHAR);
            ps.setNull(9, Types.VARCHAR);
            if (f instanceof MejaDesk meja) {
                ps.setBoolean(6, meja.isAdaStopKontak());
            } else if (f instanceof MeetingRoom room) {
                ps.setInt(7, room.getKapasitas());
            } else if (f instanceof PCGaming pc) {
                ps.setString(8, pc.getSpesifikasi());
                ps.setString(9, pc.getTipe().name());
            }
            if (f.getId() != 0) {
                ps.setInt(10, f.getId());
            }
            ps.executeUpdate();
            if (f.getId() == 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        f.setId(keys.getInt(1));
                    }
                }
            }
        }
    }

    public void updateStatus(int id, StatusFasilitas status) throws SQLException {
        try (PreparedStatement ps = Koneksi.get().prepareStatement("UPDATE fasilitas SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void hapus(int id) throws SQLException {
        try (PreparedStatement ps = Koneksi.get().prepareStatement("DELETE FROM fasilitas WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Mengubah satu baris tabel menjadi object. Kolom `jenis` menentukan subclass yang dibuat.
     * Hasilnya bertipe Fasilitas (induk), padahal isinya MejaDesk/MeetingRoom/PCGaming: ini POLYMORPHISM.
     */
    private Fasilitas map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String kode = rs.getString("kode");
        String nama = rs.getString("nama");
        double tarif = rs.getDouble("tarif_per_jam");
        StatusFasilitas status = StatusFasilitas.valueOf(rs.getString("status"));

        return switch (JenisFasilitas.valueOf(rs.getString("jenis"))) {
            case MEJA -> new MejaDesk(id, kode, nama, tarif, status, rs.getBoolean("ada_stop_kontak"));
            case MEETING -> new MeetingRoom(id, kode, nama, tarif, status, rs.getInt("kapasitas"));
            case PC -> new PCGaming(id, kode, nama, tarif, status,
                    rs.getString("spesifikasi"), TipePC.valueOf(rs.getString("tipe_pc")));
        };
    }
}
