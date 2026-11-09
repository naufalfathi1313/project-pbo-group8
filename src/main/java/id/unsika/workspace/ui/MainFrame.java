package id.unsika.workspace.ui;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;

/** Jendela utama berisi 5 tab. Setiap kali tab dibuka, datanya dimuat ulang dari database. */
public class MainFrame extends JFrame {

    public MainFrame(String namaUser) {
        super("Working Space & Rental PC Gaming");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 650));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Dashboard", new DashboardPanel());
        tabs.addTab("Jadwal & Booking", new JadwalPanel());
        tabs.addTab("Walk-in", new PemesananPanel());
        tabs.addTab("Sesi Aktif", new SesiAktifPanel());
        tabs.addTab("Rekap", new RekapPanel());
        tabs.addTab("Kelola Data", new KelolaDataPanel());

        // POLYMORPHISM: semua panel diperlakukan sebagai `Refreshable`, apa pun class aslinya
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() instanceof Refreshable panel) {
                panel.refresh();
            }
        });

        JLabel status = new JLabel("Login sebagai: " + namaUser);
        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        add(tabs, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }
}
