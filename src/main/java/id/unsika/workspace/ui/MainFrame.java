package id.unsika.workspace.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Jendela utama: menu di kiri (sidebar), judul halaman di atas, dan isi halaman di tengah.
 * Halaman diganti dengan CardLayout. Setiap kali halaman dibuka, datanya dimuat ulang.
 */
public class MainFrame extends JFrame {

    /** Satu halaman: nama di menu, judul, keterangan, dan panelnya. */
    private record Halaman(String menu, String judul, String keterangan, JComponent panel) {
    }

    private static final DateTimeFormatter JAM =
            DateTimeFormatter.ofPattern("EEEE, d MMM yyyy  ·  HH:mm:ss", new Locale("id", "ID"));

    private final CardLayout kartu = new CardLayout();
    private final JPanel isi = new JPanel(kartu);
    private final JLabel lblJudul = Tema.judul(" ");
    private final JLabel lblKeterangan = Tema.label(" ", 13f, Font.PLAIN, Tema.TEKS_REDUP);
    private final List<TombolMenu> tombolMenu = new ArrayList<>();

    public MainFrame(String namaUser) {
        super("Working Space & Rental PC Gaming");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));

        List<Halaman> halaman = List.of(
                new Halaman("Dashboard", "Dashboard", "Status semua fasilitas, diperbarui otomatis", new DashboardPanel()),
                new Halaman("Jadwal & Booking", "Jadwal & Booking", "Pilih slot jam untuk memesan tempat", new JadwalPanel()),
                new Halaman("Walk-in", "Walk-in", "Pelanggan datang tanpa booking dan langsung main", new PemesananPanel()),
                new Halaman("Sesi Aktif", "Sesi Aktif", "Check-in booking, pantau timer, dan selesaikan sesi", new SesiAktifPanel()),
                new Halaman("Rekap", "Rekap Pendapatan", "Ringkasan transaksi yang sudah selesai", new RekapPanel()),
                new Halaman("Kelola Data", "Kelola Data", "Tambah, ubah, dan hapus fasilitas serta pelanggan", new KelolaDataPanel()));

        JPanel sidebar = buatSidebar(namaUser, halaman);
        for (Halaman h : halaman) {
            isi.add(h.panel(), h.menu());
        }

        JPanel kepala = new JPanel(new BorderLayout());
        kepala.setBorder(BorderFactory.createEmptyBorder(22, 24, 16, 24));
        JPanel teks = new JPanel(new GridLayout(2, 1, 0, 2));
        teks.setOpaque(false);
        teks.add(lblJudul);
        teks.add(lblKeterangan);
        kepala.add(teks, BorderLayout.WEST);
        JLabel lblJam = Tema.label(" ", 13f, Font.PLAIN, Tema.TEKS_REDUP);
        kepala.add(lblJam, BorderLayout.EAST);
        Runnable perbaruiJam = () -> lblJam.setText(LocalDateTime.now().format(JAM));
        perbaruiJam.run();
        new Timer(1000, e -> perbaruiJam.run()).start();

        JPanel kanan = new JPanel(new BorderLayout());
        kanan.add(kepala, BorderLayout.NORTH);
        kanan.add(isi, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(kanan, BorderLayout.CENTER);
        buka(halaman.get(0));
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buatSidebar(String namaUser, List<Halaman> halaman) {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Tema.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Tema.GARIS),
                BorderFactory.createEmptyBorder(22, 14, 18, 14)));

        JPanel atas = new JPanel();
        atas.setOpaque(false);
        atas.setLayout(new BoxLayout(atas, BoxLayout.Y_AXIS));
        JLabel logo = Tema.label("WORKSPACE", 17f, Font.BOLD, Tema.TEKS);
        JLabel logo2 = Tema.label("& PC GAMING", 17f, Font.BOLD, Tema.AKSEN);
        logo.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        logo2.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        atas.add(logo);
        atas.add(logo2);
        atas.add(Box.createVerticalStrut(28));
        JLabel menu = Tema.label("MENU", 11f, Font.BOLD, Tema.TEKS_SAMAR);
        menu.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 0));
        atas.add(menu);

        for (Halaman h : halaman) {
            TombolMenu t = new TombolMenu(h.menu());
            t.addActionListener(e -> buka(h));
            tombolMenu.add(t);
            atas.add(t);
            atas.add(Box.createVerticalStrut(4));
        }

        JPanel bawah = new JPanel(new BorderLayout(0, 10));
        bawah.setOpaque(false);
        JPanel user = new JPanel(new GridLayout(2, 1));
        user.setOpaque(false);
        user.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        user.add(Tema.label(namaUser, 13f, Font.BOLD, Tema.TEKS));
        user.add(Tema.label("Kasir aktif", 12f, Font.PLAIN, Tema.TEKS_REDUP));
        JButton keluar = new JButton("Keluar");
        keluar.addActionListener(e -> {
            if (Pesan.konfirmasi(this, "Keluar dari aplikasi dan kembali ke halaman login?")) {
                dispose();
                new LoginFrame().setVisible(true);
            }
        });
        bawah.add(user, BorderLayout.CENTER);
        bawah.add(keluar, BorderLayout.SOUTH);

        sidebar.add(atas, BorderLayout.NORTH);
        sidebar.add(bawah, BorderLayout.SOUTH);
        return sidebar;
    }

    private void buka(Halaman h) {
        kartu.show(isi, h.menu());
        lblJudul.setText(h.judul());
        lblKeterangan.setText(h.keterangan());
        for (TombolMenu t : tombolMenu) {
            t.setAktif(t.getText().equals(h.menu()));
        }
        // POLYMORPHISM: semua halaman diperlakukan sebagai Refreshable, apa pun class aslinya
        if (h.panel() instanceof Refreshable r) {
            r.refresh();
        }
    }

    /** Tombol menu di sidebar: latar ungu redup + garis aksen saat aktif. */
    private static class TombolMenu extends JButton {

        private boolean aktif;
        private boolean hover;

        TombolMenu(String teks) {
            super(teks);
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(getFont().deriveFont(Font.PLAIN, 14f));
            setForeground(Tema.TEKS_REDUP);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 10));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void setAktif(boolean aktif) {
            this.aktif = aktif;
            setForeground(aktif ? Tema.TEKS : Tema.TEKS_REDUP);
            setFont(getFont().deriveFont(aktif ? Font.BOLD : Font.PLAIN));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (aktif) {
                g2.setColor(new Color(124, 77, 255, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(Tema.AKSEN);
                g2.fillRoundRect(0, 9, 3, getHeight() - 18, 3, 3);
            } else if (hover) {
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
