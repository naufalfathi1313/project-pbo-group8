package id.unsika.workspace;

import id.unsika.workspace.ui.LoginFrame;
import id.unsika.workspace.ui.Tema;

import javax.swing.SwingUtilities;

/** Titik awal program. Jalankan class ini (tombol Run di VS Code). */
public class App {

    public static void main(String[] args) {
        java.util.Locale.setDefault(new java.util.Locale("id", "ID")); // nama hari & bulan dalam Bahasa Indonesia
        Tema.pasang(); // tema gelap ala gaming (library FlatLaf)
        // Semua kode Swing wajib dijalankan di Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
