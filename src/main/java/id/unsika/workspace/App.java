package id.unsika.workspace;

import id.unsika.workspace.db.Koneksi;
import id.unsika.workspace.ui.LoginFrame;
import id.unsika.workspace.ui.Tema;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.sql.SQLException;

/** Titik awal program. Jalankan class ini (tombol Run di VS Code). */
public class App {

    public static void main(String[] args) {
        java.util.Locale.setDefault(new java.util.Locale("id", "ID")); // nama hari & bulan dalam Bahasa Indonesia
        Tema.pasang(); // tema gelap ala gaming (library FlatLaf)
        // Semua kode Swing wajib dijalankan di Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            if (databaseSiap()) {
                new LoginFrame().setVisible(true);
            }
        });
    }

    /** Cek koneksi database sebelum login. Jika gagal, beri tahu cara memperbaikinya dan tawarkan coba lagi. */
    private static boolean databaseSiap() {
        while (true) {
            try {
                Koneksi.get();
                return true;
            } catch (SQLException e) {
                Object[] pilihan = {"Coba Lagi", "Keluar"};
                int jawab = JOptionPane.showOptionDialog(null,
                        "Aplikasi tidak bisa terhubung ke database MySQL.\n\n"
                                + "Coba periksa:\n"
                                + "• Windows: buka XAMPP Control Panel, klik Start pada MySQL\n"
                                + "• Mac: jalankan  brew services start mysql  di Terminal\n"
                                + "• Database workspace_db sudah di-import (database/workspace_db.sql)\n\n"
                                + "Detail: " + e.getMessage(),
                        "Database Tidak Terhubung", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
                        null, pilihan, pilihan[0]);
                if (jawab != 0) {
                    System.exit(1);
                    return false;
                }
            }
        }
    }
}
