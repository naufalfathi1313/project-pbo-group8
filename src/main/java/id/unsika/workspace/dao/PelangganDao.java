package id.unsika.workspace.dao;

import id.unsika.workspace.db.Koneksi;
import id.unsika.workspace.model.Pelanggan;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PelangganDao implements Dao<Pelanggan> {

    @Override
    public List<Pelanggan> semua() throws SQLException {
        List<Pelanggan> hasil = new ArrayList<>();
        try (Statement st = Koneksi.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM pelanggan ORDER BY nama")) {
            while (rs.next()) {
                hasil.add(map(rs));
            }
        }
        return hasil;
    }

    @Override
    public Pelanggan cariById(int id) throws SQLException {
        try (PreparedStatement ps = Koneksi.get().prepareStatement("SELECT * FROM pelanggan WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    @Override
    public void simpan(Pelanggan p) throws SQLException {
        if (p.getId() == 0) {
            String sql = "INSERT INTO pelanggan (nama, no_hp) VALUES (?, ?)";
            try (PreparedStatement ps = Koneksi.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.getNama());
                ps.setString(2, p.getNoHp());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        p.setId(keys.getInt(1));
                    }
                }
            }
        } else {
            String sql = "UPDATE pelanggan SET nama = ?, no_hp = ? WHERE id = ?";
            try (PreparedStatement ps = Koneksi.get().prepareStatement(sql)) {
                ps.setString(1, p.getNama());
                ps.setString(2, p.getNoHp());
                ps.setInt(3, p.getId());
                ps.executeUpdate();
            }
        }
    }

    @Override
    public void hapus(int id) throws SQLException {
        try (PreparedStatement ps = Koneksi.get().prepareStatement("DELETE FROM pelanggan WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Pelanggan map(ResultSet rs) throws SQLException {
        return new Pelanggan(rs.getInt("id"), rs.getString("nama"), rs.getString("no_hp"));
    }
}
