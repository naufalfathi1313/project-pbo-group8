package id.unsika.workspace.ui;

import id.unsika.workspace.dao.PemesananDao;
import id.unsika.workspace.model.Pemesanan;
import id.unsika.workspace.model.StatusPemesanan;
import id.unsika.workspace.service.PemesananService;
import id.unsika.workspace.util.Format;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Daftar pemesanan yang sedang berjalan, dengan TIMER DURASI live.
 * - Timer 1 detik: memperbarui kolom Durasi & Biaya (dihitung dari waktu mulai, tanpa ke database).
 * - Setiap 5 detik: memuat ulang daftar dari database (menangkap booking dari komputer lain).
 */
public class SesiAktifPanel extends JPanel implements Refreshable {

    private static final int DETIK_RELOAD = 5;

    private final PemesananDao pemesananDao = new PemesananDao();
    private final PemesananService service = new PemesananService();
    private final SesiTableModel model = new SesiTableModel();
    private final JTable table = new JTable(model);
    private int hitungDetik = 0;

    public SesiAktifPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        int[] lebar = {50, 150, 230, 150, 200, 140, 90, 130};
        for (int i = 0; i < lebar.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(lebar[i]);
        }

        JButton btnCheckIn = new JButton("Check-in (Mulai Timer)");
        btnCheckIn.addActionListener(e -> aksi(Aksi.CHECK_IN));
        JButton btnSelesai = Tema.tombolUtama("Selesai & Bayar");
        btnSelesai.addActionListener(e -> aksi(Aksi.SELESAI));
        JButton btnBatal = new JButton("Batalkan Booking");
        btnBatal.addActionListener(e -> aksi(Aksi.BATAL));

        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tombol.setOpaque(false);
        tombol.add(btnCheckIn);
        tombol.add(javax.swing.Box.createHorizontalStrut(8));
        tombol.add(btnBatal);
        tombol.add(javax.swing.Box.createHorizontalStrut(8));
        tombol.add(btnSelesai);

        Kartu kartu = new Kartu(new BorderLayout(0, 14));
        JPanel kepala = new JPanel(new BorderLayout());
        kepala.setOpaque(false);
        kepala.add(Tema.label("Pilih baris, lalu klik aksi di kanan", 13f, java.awt.Font.PLAIN, Tema.TEKS_REDUP), BorderLayout.WEST);
        kepala.add(tombol, BorderLayout.EAST);
        kartu.add(kepala, BorderLayout.NORTH);
        kartu.add(new JScrollPane(table), BorderLayout.CENTER);
        add(kartu, BorderLayout.CENTER);

        refresh();
        new Timer(1000, e -> {
            if (!isShowing()) {
                return;
            }
            if (++hitungDetik >= DETIK_RELOAD) {
                hitungDetik = 0;
                refresh();
            } else {
                model.perbaruiTimer();
            }
        }).start();
    }

    @Override
    public void refresh() {
        try {
            Pemesanan terpilih = terpilih();
            model.setData(pemesananDao.berjalan());
            if (terpilih != null) {  // pertahankan baris yang sedang dipilih
                int baris = model.cariBaris(terpilih.getId());
                if (baris >= 0) {
                    table.setRowSelectionInterval(baris, baris);
                }
            }
        } catch (SQLException ex) {
            Pesan.error(this, ex);
        }
    }

    private Pemesanan terpilih() {
        int baris = table.getSelectedRow();
        return baris < 0 ? null : model.get(baris);
    }

    private enum Aksi { CHECK_IN, SELESAI, BATAL }

    private void aksi(Aksi aksi) {
        Pemesanan p = terpilih();
        if (p == null) {
            Pesan.info(this, "Pilih salah satu baris terlebih dahulu.");
            return;
        }
        try {
            switch (aksi) {
                case CHECK_IN -> service.checkIn(p);
                case SELESAI -> {
                    if (!Pesan.konfirmasi(this, "Selesaikan sesi " + p.getFasilitas().getKode() + "?")) {
                        return;
                    }
                    service.selesaikan(p);
                    tampilkanStruk(p);
                }
                case BATAL -> {
                    if (!Pesan.konfirmasi(this, "Batalkan booking " + p.getFasilitas().getKode() + "?")) {
                        return;
                    }
                    service.batalkan(p);
                }
            }
        } catch (Exception ex) {
            Pesan.error(this, ex);
        }
        refresh();
    }

    private void tampilkanStruk(Pemesanan p) {
        Pesan.info(this, "===== STRUK SEWA =====\n"
                + "Pelanggan : " + p.getPelanggan().getNama() + "\n"
                + "Fasilitas : " + p.getFasilitas() + "\n"
                + "Tarif     : " + Format.rupiah(p.getFasilitas().getTarifPerJam()) + " / jam\n"
                + "Mulai     : " + Format.waktu(p.getWaktuMulai()) + "\n"
                + "Selesai   : " + Format.waktu(p.getWaktuSelesai()) + "\n"
                + "Durasi    : " + Format.durasi(p.getDurasiDetik()) + "\n"
                + "TOTAL     : " + Format.rupiah(p.getTotalBiaya()));
    }

    /** Model tabel: menjembatani List<Pemesanan> dengan JTable. */
    private static class SesiTableModel extends AbstractTableModel {

        private static final String[] KOLOM = {"ID", "Pelanggan", "Fasilitas", "Status", "Jadwal", "Mulai", "Durasi", "Biaya Sementara"};
        private List<Pemesanan> data = new ArrayList<>();

        void setData(List<Pemesanan> data) {
            this.data = data;
            fireTableDataChanged();
        }

        Pemesanan get(int baris) {
            return data.get(baris);
        }

        int cariBaris(int id) {
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i).getId() == id) {
                    return i;
                }
            }
            return -1;
        }

        /** Hanya memberi tahu JTable untuk menggambar ulang; nilai durasi dihitung ulang di getValueAt. */
        void perbaruiTimer() {
            if (!data.isEmpty()) {
                fireTableRowsUpdated(0, data.size() - 1);
            }
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return KOLOM.length;
        }

        @Override
        public String getColumnName(int kolom) {
            return KOLOM[kolom];
        }

        @Override
        public Object getValueAt(int baris, int kolom) {
            Pemesanan p = data.get(baris);
            boolean aktif = p.getStatus() == StatusPemesanan.AKTIF;
            return switch (kolom) {
                case 0 -> p.getId();
                case 1 -> p.getPelanggan().getNama();
                case 2 -> p.getFasilitas().toString();
                case 3 -> aktif ? "Berjalan" : "Menunggu check-in";
                case 4 -> p.punyaJadwal()
                        ? Format.waktu(p.getJadwalMulai()) + " - " + String.format("%02d:00", p.getJadwalSelesai().getHour())
                        : "Walk-in";
                case 5 -> Format.waktu(p.getWaktuMulai());
                case 6 -> aktif ? Format.durasi(p.getDurasiDetik()) : "-";
                case 7 -> aktif ? Format.rupiah(p.getBiayaSementara()) : "-";
                default -> "";
            };
        }
    }
}
