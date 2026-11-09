package id.unsika.workspace.ui;

import id.unsika.workspace.model.Pemesanan;
import id.unsika.workspace.model.StatusPemesanan;
import id.unsika.workspace.service.PembayaranQr;
import id.unsika.workspace.service.PemesananService;
import id.unsika.workspace.util.Format;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;

/**
 * Dialog "Selesai & Bayar".
 * Tagihan DIBEKUKAN saat dialog dibuka: jam selesai = saat kasir menekan tombol, jadi angkanya tidak
 * berubah selama pelanggan membayar. Pelanggan lalu memilih:
 * - scan QR dan bayar dari HP-nya (online), atau
 * - bayar tunai ke kasir.
 * Selama dialog terbuka, Timer menanyakan status pembayaran QR setiap 3 detik.
 */
public class DialogBayar extends JDialog {

    private static final int DETIK_CEK = 3;

    private final Pemesanan pemesanan;
    private final PemesananService service;
    private final LocalDateTime waktuTagih = LocalDateTime.now();
    private final PembayaranQr qr = new PembayaranQr();
    private final JLabel lblStatus = Tema.label("Menunggu pelanggan membayar lewat HP...", 13f, Font.PLAIN, Tema.TEKS_REDUP);
    private final Timer timer = new Timer(DETIK_CEK * 1000, e -> cekStatus());
    private SwingWorker<PembayaranQr.Status, Void> pekerja; // permintaan ke server yang sedang berjalan
    private String metode;                                   // null = belum dibayar

    public DialogBayar(Window owner, Pemesanan pemesanan, PemesananService service) {
        super(owner, "Pembayaran " + pemesanan.getFasilitas().getKode(), ModalityType.APPLICATION_MODAL);
        if (pemesanan.getStatus() != StatusPemesanan.AKTIF) {
            throw new IllegalStateException("Hanya sesi yang sedang berjalan yang bisa dibayar. Lakukan check-in dulu.");
        }
        this.pemesanan = pemesanan;
        this.service = service;

        double total = pemesanan.biayaSampai(waktuTagih);
        String durasi = Format.durasi(pemesanan.durasiDetikSampai(waktuTagih));
        String fasilitas = pemesanan.getFasilitas().getKode() + " · " + pemesanan.getFasilitas().getNama();
        String link = qr.link(fasilitas, pemesanan.getPelanggan().getNama(), durasi, total);

        // Kiri: QR code + petunjuk
        Kartu kartuQr = new Kartu(new BorderLayout(0, 10));
        kartuQr.add(new PanelQr(link), BorderLayout.CENTER);
        kartuQr.add(Tema.label("Scan pakai kamera HP untuk bayar", 12f, Font.PLAIN, Tema.TEKS_REDUP), BorderLayout.SOUTH);

        // Kanan: total + rincian tagihan
        JPanel rincian = new JPanel(new GridBagLayout());
        rincian.setOpaque(false);
        baris(rincian, 0, "Pelanggan", pemesanan.getPelanggan().getNama());
        baris(rincian, 1, "Fasilitas", fasilitas);
        baris(rincian, 2, "Tarif", Format.rupiah(pemesanan.getFasilitas().getTarifPerJam()) + " / jam");
        baris(rincian, 3, "Mulai", Format.waktu(pemesanan.getWaktuMulai()));
        baris(rincian, 4, "Selesai", Format.waktu(waktuTagih));
        baris(rincian, 5, "Durasi", durasi);
        rincian.setMaximumSize(rincian.getPreferredSize()); // jangan melar, supaya rapat di bawah total

        JPanel kanan = new JPanel();
        kanan.setOpaque(false);
        kanan.setLayout(new BoxLayout(kanan, BoxLayout.Y_AXIS));
        kanan.add(rata(Tema.label("Total tagihan", 13f, Font.PLAIN, Tema.TEKS_REDUP)));
        kanan.add(rata(Tema.label(Format.rupiah(total), 30f, Font.BOLD, Tema.TEKS)));
        kanan.add(Box.createVerticalStrut(18));
        kanan.add(rata(rincian));
        kanan.add(Box.createVerticalGlue());
        kanan.add(rata(lblStatus));

        JPanel isi = new JPanel(new BorderLayout(20, 0));
        isi.setOpaque(false);
        isi.add(kartuQr, BorderLayout.WEST);
        isi.add(kanan, BorderLayout.CENTER);

        // Bawah: tombol
        JButton btnBatal = new JButton("Batal");
        btnBatal.addActionListener(e -> dispose());
        JButton btnTunai = Tema.tombolUtama("Bayar Tunai");
        btnTunai.addActionListener(e -> lunas("Tunai"));
        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tombol.setOpaque(false);
        tombol.add(btnBatal);
        tombol.add(btnTunai);

        JPanel latar = new JPanel(new BorderLayout(0, 18));
        latar.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        latar.add(isi, BorderLayout.CENTER);
        latar.add(tombol, BorderLayout.SOUTH);
        setContentPane(latar);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                timer.stop(); // dialog ditutup: berhenti bertanya ke server
            }
        });
        pack();
        setLocationRelativeTo(owner);
        timer.start();
    }

    /** Cara pelanggan membayar ("Tunai" / "QR (online)"), atau null jika dialog ditutup tanpa bayar. */
    public String getMetode() {
        return metode;
    }

    /**
     * Dipanggil Timer setiap 3 detik. Permintaan ke internet dijalankan SwingWorker di thread lain,
     * supaya tampilan tidak macet saat menunggu jawaban server.
     */
    private void cekStatus() {
        if (pekerja != null && !pekerja.isDone()) {
            return; // pertanyaan sebelumnya belum dijawab
        }
        pekerja = new SwingWorker<>() {
            @Override
            protected PembayaranQr.Status doInBackground() throws Exception {
                return qr.cekStatus();
            }

            @Override
            protected void done() {
                if (!DialogBayar.this.isDisplayable()) {
                    return; // dialog sudah ditutup
                }
                try {
                    tampilkan(get());
                } catch (Exception ex) {
                    status("<html>Belum terhubung ke server pembayaran.<br>Mencoba lagi otomatis...</html>", Tema.TEKS_REDUP);
                }
            }
        };
        pekerja.execute();
    }

    private void tampilkan(PembayaranQr.Status hasil) {
        switch (hasil) {
            case LUNAS -> lunas("QR (online)");
            case DITOLAK -> status("<html>Pelanggan memilih <b>Tidak</b> di HP-nya.<br>Minta scan ulang, atau terima bayar tunai.</html>", Tema.KUNING);
            case MENUNGGU -> status("Menunggu pelanggan membayar lewat HP...", Tema.TEKS_REDUP);
        }
    }

    /** Tagihan lunas: sesi diselesaikan dengan jam selesai yang sudah dibekukan, lalu dialog ditutup. */
    private void lunas(String caraBayar) {
        timer.stop();
        try {
            service.selesaikan(pemesanan, waktuTagih);
            metode = caraBayar;
            dispose();
        } catch (Exception ex) {
            Pesan.error(this, ex);
        }
    }

    private void status(String teks, Color warna) {
        lblStatus.setText(teks);
        lblStatus.setForeground(warna);
    }

    private static void baris(JPanel panel, int y, String label, String nilai) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 8, 18);
        panel.add(Tema.label(label, 13f, Font.PLAIN, Tema.TEKS_REDUP), c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 8, 0);
        panel.add(Tema.label(nilai, 13f, Font.BOLD, Tema.TEKS), c);
    }

    private static JComponent rata(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }
}
