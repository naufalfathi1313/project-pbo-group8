package id.unsika.workspace.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Mengatur koneksi ke MySQL (XAMPP).
 * Memakai pola SINGLETON: seluruh aplikasi berbagi satu object Connection yang sama,
 * jadi tidak membuka koneksi baru setiap kali butuh data.
 */
public final class Koneksi {

    // Sesuaikan jika setting XAMPP kalian berbeda (default XAMPP: user root, password kosong)
    private static final String URL = "jdbc:mysql://localhost:3306/workspace_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static Connection connection;

    private Koneksi() {
        // constructor private: class ini tidak boleh di-new
    }

    public static Connection get() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            connection = DriverManager.getConnection(
                    System.getProperty("db.url", URL),
                    System.getProperty("db.user", USER),
                    System.getProperty("db.password", PASSWORD));
        }
        return connection;
    }
}
