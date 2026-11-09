package id.unsika.workspace.dao;

import id.unsika.workspace.db.Koneksi;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {

    /**
     * Mengembalikan nama user jika username & password cocok, atau null jika salah.
     * Password di-hash dengan SHA-256 oleh MySQL lalu dibandingkan dengan hash yang tersimpan.
     */
    public String login(String username, String password) throws SQLException {
        String sql = "SELECT nama FROM users WHERE username = ? AND password_hash = SHA2(?, 256)";
        try (PreparedStatement ps = Koneksi.get().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("nama") : null;
            }
        }
    }
}
