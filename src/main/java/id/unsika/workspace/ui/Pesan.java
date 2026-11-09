package id.unsika.workspace.ui;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.sql.SQLException;

/** Helper untuk menampilkan pesan error yang mudah dipahami pengguna. */
final class Pesan {

    private Pesan() {
    }

    static void error(Component parent, Exception e) {
        String teks;
        if (e instanceof SQLException) {
            teks = "Gagal mengakses database.\nPastikan MySQL di XAMPP sudah menyala.\n\nDetail: " + e.getMessage();
        } else {
            teks = e.getMessage();
        }
        JOptionPane.showMessageDialog(parent, teks, "Terjadi Kesalahan", JOptionPane.ERROR_MESSAGE);
    }

    static void info(Component parent, String teks) {
        JOptionPane.showMessageDialog(parent, teks, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    static boolean konfirmasi(Component parent, String teks) {
        return JOptionPane.showConfirmDialog(parent, teks, "Konfirmasi", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }
}
