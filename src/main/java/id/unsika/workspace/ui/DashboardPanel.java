package id.unsika.workspace.ui;

import id.unsika.workspace.dao.FasilitasDao;
import id.unsika.workspace.dao.PemesananDao;
import id.unsika.workspace.model.Fasilitas;
import id.unsika.workspace.model.JenisFasilitas;
import id.unsika.workspace.model.Pemesanan;
import id.unsika.workspace.model.StatusFasilitas;
import id.unsika.workspace.model.StatusPemesanan;
import id.unsika.workspace.util.Format;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dashboard: angka ringkasan di atas, lalu kartu tiap fasilitas.
 * - Setiap detik: teks timer di kartu yang sedang dipakai diperbarui (tanpa ke database).
 * - Setiap 3 detik: data dimuat ulang dari database, jadi perubahan dari komputer lain ikut tampil (REAL-TIME).
 */
public class DashboardPanel extends JPanel implements Refreshable {

    private static final int DETIK_RELOAD = 3;

    private final FasilitasDao fasilitasDao = new FasilitasDao();
    private final PemesananDao pemesananDao = new PemesananDao();

    private final StatTile tileTersedia = new StatTile("Tersedia sekarang");
    private final StatTile tileDipakai = new StatTile("Sedang dipakai");
    private final StatTile tileBooking = new StatTile("Booking hari ini");
    private final StatTile tilePendapatan = new StatTile("Pendapatan hari ini");

    private final JPanel grid = new JPanel(new GridLayout(0, 4, 14, 14));
    private final List<KartuFasilitas> kartu = new ArrayList<>();
    private JenisFasilitas filter = null; // null = semua jenis
    private int hitungDetik = 0;
    private boolean errorDitampilkan = false;

    public DashboardPanel() {
        super(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));

        JPanel tiles = new JPanel(new GridLayout(1, 4, 16, 0));
        tiles.add(tileTersedia);
        tiles.add(tileDipakai);
        tiles.add(tileBooking);
        tiles.add(tilePendapatan);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ButtonGroup grup = new ButtonGroup();
        JToggleButton semua = Tema.chip("Semua");
        semua.setSelected(true);
        semua.addActionListener(e -> pilihFilter(null));
        grup.add(semua);
        filterBar.add(semua);
        for (JenisFasilitas j : JenisFasilitas.values()) {
            JToggleButton b = Tema.chip(j.getLabel());
            b.addActionListener(e -> pilihFilter(j));
            grup.add(b);
            filterBar.add(b);
        }
        filterBar.add(Box.createHorizontalStrut(16));
        for (StatusFasilitas s : StatusFasilitas.values()) {
            filterBar.add(legenda(s));
        }

        JPanel atas = new JPanel(new BorderLayout(0, 16));
        atas.add(tiles, BorderLayout.NORTH);
        atas.add(filterBar, BorderLayout.SOUTH);

        JScrollPane gulir = new JScrollPane(new PanelGulir(grid)); // kartu tidak melar & tanpa scroll ke samping
        gulir.setBorder(null);
        gulir.getVerticalScrollBar().setUnitIncrement(16);

        add(atas, BorderLayout.NORTH);
        add(gulir, BorderLayout.CENTER);

        refresh();
        new Timer(1000, e -> {
            if (!isShowing()) {
                return;
            }
            if (++hitungDetik >= DETIK_RELOAD) {
                hitungDetik = 0;
                refresh();
            } else {
                for (KartuFasilitas k : kartu) {
                    k.perbaruiTimer();
                }
            }
        }).start();
    }

    private void pilihFilter(JenisFasilitas j) {
        filter = j;
        refresh();
    }

    @Override
    public void refresh() {
        try {
            List<Fasilitas> semua = fasilitasDao.semua();
            Set<Integer> dipesanSekarang = pemesananDao.fasilitasDipesanSekarang();
            List<Pemesanan> berjalan = pemesananDao.berjalan();
            List<Pemesanan> selesaiHariIni = pemesananDao.rekap(LocalDate.now(), LocalDate.now());

            // Cari sesi aktif & booking terdekat untuk tiap fasilitas
            Map<Integer, Pemesanan> aktif = new HashMap<>();
            Map<Integer, Pemesanan> bookingBerikut = new HashMap<>();
            int bookingHariIni = 0;
            LocalDateTime sekarang = LocalDateTime.now();
            for (Pemesanan p : berjalan) {
                int id = p.getFasilitas().getId();
                if (p.getStatus() == StatusPemesanan.AKTIF) {
                    aktif.put(id, p);
                } else if (p.punyaJadwal() && p.getJadwalSelesai().isAfter(sekarang)) {
                    if (p.getJadwalMulai().toLocalDate().equals(LocalDate.now())) {
                        bookingHariIni++;
                    }
                    Pemesanan lama = bookingBerikut.get(id);
                    if (lama == null || p.getJadwalMulai().isBefore(lama.getJadwalMulai())) {
                        bookingBerikut.put(id, p);
                    }
                }
            }

            int tersedia = 0;
            int dipakai = 0;
            grid.removeAll();
            kartu.clear();
            for (Fasilitas f : semua) {
                StatusFasilitas status = (f.bisaDipesan() && dipesanSekarang.contains(f.getId()))
                        ? StatusFasilitas.DIPESAN : f.getStatus();
                if (status == StatusFasilitas.TERSEDIA) {
                    tersedia++;
                } else if (status == StatusFasilitas.DIPAKAI) {
                    dipakai++;
                }
                if (filter == null || f.getJenis() == filter) {
                    KartuFasilitas k = new KartuFasilitas(f, status, aktif.get(f.getId()), bookingBerikut.get(f.getId()));
                    kartu.add(k);
                    grid.add(k);
                }
            }
            double pendapatan = 0;
            for (Pemesanan p : selesaiHariIni) {
                pendapatan += p.getTotalBiaya();
            }

            tileTersedia.set(tersedia + " / " + semua.size(), "fasilitas siap dipakai");
            tileDipakai.set(String.valueOf(dipakai), dipakai == 0 ? "tidak ada sesi berjalan" : "timer sedang berjalan");
            tileBooking.set(String.valueOf(bookingHariIni), "belum check-in");
            tilePendapatan.set(Format.rupiah(pendapatan), selesaiHariIni.size() + " sesi selesai");

            grid.revalidate();
            grid.repaint();
            errorDitampilkan = false;
        } catch (SQLException ex) {
            if (!errorDitampilkan) {  // jangan munculkan popup yang sama berulang kali
                errorDitampilkan = true;
                Pesan.error(this, ex);
            }
        }
    }

    private static JPanel legenda(StatusFasilitas s) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JLabel titik = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Tema.warna(s));
                g.fillRoundRect(0, 3, 10, 10, 4, 4);
            }
        };
        titik.setPreferredSize(new java.awt.Dimension(10, 16));
        p.add(titik);
        p.add(Tema.label(Tema.labelStatus(s), 12f, Font.PLAIN, Tema.TEKS_REDUP));
        return p;
    }

    /** Kartu satu fasilitas. Garis warna di kiri + pil status menandai statusnya. */
    private static class KartuFasilitas extends Kartu {

        private final Pemesanan sesi;
        private final JLabel lblAktivitas = Tema.label(" ", 12.5f, Font.PLAIN, Tema.TEKS);
        private final Color warnaStatus;

        KartuFasilitas(Fasilitas f, StatusFasilitas status, Pemesanan sesi, Pemesanan booking) {
            super(new BorderLayout(0, 10));
            this.sesi = sesi;
            this.warnaStatus = Tema.warna(status);
            setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 14));

            JPanel kepala = new JPanel(new BorderLayout());
            kepala.setOpaque(false);
            kepala.add(Tema.label(f.getKode(), 18f, Font.BOLD, Tema.TEKS), BorderLayout.WEST);
            JPanel pilWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            pilWrap.setOpaque(false);
            pilWrap.add(new Pil(Tema.labelStatus(status), warnaStatus));
            kepala.add(pilWrap, BorderLayout.EAST);

            JPanel tengah = new JPanel();
            tengah.setOpaque(false);
            tengah.setLayout(new BoxLayout(tengah, BoxLayout.Y_AXIS));
            tengah.add(Tema.label(f.getNama(), 13.5f, Font.BOLD, Tema.TEKS));
            tengah.add(Box.createVerticalStrut(2));
            tengah.add(Tema.label(f.getJenis().getLabel() + "  ·  " + Format.rupiah(f.getTarifPerJam()) + "/jam",
                    12f, Font.PLAIN, Tema.TEKS_REDUP));
            tengah.add(Tema.label(f.getInfoTambahan(), 12f, Font.PLAIN, Tema.TEKS_SAMAR));

            JPanel bawah = new JPanel(new BorderLayout());
            bawah.setOpaque(false);
            bawah.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.GARIS),
                    BorderFactory.createEmptyBorder(10, 0, 0, 0)));
            bawah.add(lblAktivitas, BorderLayout.CENTER);

            lblAktivitas.setToolTipText(null);
            if (sesi != null) {
                perbaruiTimer();
            } else if (status == StatusFasilitas.MAINTENANCE) {
                lblAktivitas.setText("Sedang diperbaiki");
                lblAktivitas.setForeground(Tema.TEKS_REDUP);
            } else if (booking != null) {
                lblAktivitas.setText("Booking " + jamRentang(booking) + "  ·  " + booking.getPelanggan().getNama());
                lblAktivitas.setForeground(status == StatusFasilitas.DIPESAN ? Tema.TEKS : Tema.TEKS_REDUP);
            } else {
                lblAktivitas.setText("Siap dipakai");
                lblAktivitas.setForeground(Tema.TEKS_REDUP);
            }

            if (sesi == null) {
                lblAktivitas.setToolTipText(lblAktivitas.getText()); // teks lengkap jika terpotong
            }
            add(kepala, BorderLayout.NORTH);
            add(tengah, BorderLayout.CENTER);
            add(bawah, BorderLayout.SOUTH);
        }

        void perbaruiTimer() {
            if (sesi != null) {
                lblAktivitas.setText(sesi.getPelanggan().getNama() + "  ·  " + Format.durasi(sesi.getDurasiDetik())
                        + "  ·  " + Format.rupiah(sesi.getBiayaSementara()));
            }
        }

        private static String jamRentang(Pemesanan p) {
            LocalDate tgl = p.getJadwalMulai().toLocalDate();
            String hari = tgl.equals(LocalDate.now()) ? ""
                    : tgl.equals(LocalDate.now().plusDays(1)) ? "besok "
                    : String.format("%02d/%02d ", tgl.getDayOfMonth(), tgl.getMonthValue());
            return hari + String.format("%02d:00–%02d:00", p.getJadwalMulai().getHour(), p.getJadwalSelesai().getHour());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(warnaStatus);
            g2.fillRoundRect(0, 14, 4, getHeight() - 28, 4, 4); // garis status di sisi kiri
            g2.dispose();
        }
    }
}
