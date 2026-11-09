package id.unsika.workspace.ui;

import id.unsika.workspace.dao.FasilitasDao;
import id.unsika.workspace.dao.PemesananDao;
import id.unsika.workspace.model.Fasilitas;
import id.unsika.workspace.model.JenisFasilitas;
import id.unsika.workspace.model.Pelanggan;
import id.unsika.workspace.model.Pemesanan;
import id.unsika.workspace.model.StatusFasilitas;
import id.unsika.workspace.service.PemesananService;
import id.unsika.workspace.util.Format;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

/**
 * Grid jadwal per jam: baris = jam, kolom = fasilitas.
 * Klik kotak "Tersedia" untuk memilih slot (boleh beberapa jam berurutan pada satu fasilitas),
 * lalu klik Konfirmasi Booking. Estimasi biaya dihitung oleh fasilitas.hitungBiaya() (polymorphism).
 */
public class JadwalPanel extends JPanel implements Refreshable {

    /** Status satu kotak di grid. Berbeda dengan StatusFasilitas karena terikat pada jam tertentu. */
    private enum StatusSlot {
        TERSEDIA("Tersedia", Tema.HIJAU),
        DIPILIH("Dipilih", Tema.AKSEN),
        DIBOOKING("Dibooking", Tema.KUNING),
        DIPAKAI("Dipakai", Tema.MERAH),
        TUTUP("Tutup / Lewat", Tema.LEWAT);

        final String label;
        final Color warna;

        StatusSlot(String label, Color warna) {
            this.label = label;
            this.warna = warna;
        }
    }

    private static final String SEMUA = "Semua jenis";

    private final FasilitasDao fasilitasDao = new FasilitasDao();
    private final PemesananDao pemesananDao = new PemesananDao();
    private final PemesananService service = new PemesananService();

    private final JSpinner spTanggal = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
    private final JComboBox<Object> cbJenis = new JComboBox<>();
    private final JPanel grid = new JPanel();
    private final JLabel lblRingkasan = new JLabel();
    private final PelangganPicker pelangganPicker = new PelangganPicker();

    private List<Fasilitas> daftarFasilitas = new ArrayList<>();
    private List<Pemesanan> bookingHariItu = new ArrayList<>();

    // Slot yang sedang dipilih: satu fasilitas, beberapa jam
    private Fasilitas fasilitasDipilih = null;
    private final TreeSet<Integer> jamDipilih = new TreeSet<>();

    public JadwalPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        spTanggal.setEditor(new JSpinner.DateEditor(spTanggal, "EEEE, dd/MM/yyyy"));
        spTanggal.addChangeListener(e -> {
            resetPilihan();
            refresh();
        });
        cbJenis.addItem(SEMUA);
        for (JenisFasilitas j : JenisFasilitas.values()) {
            cbJenis.addItem(j);
        }
        cbJenis.addActionListener(e -> {
            resetPilihan();
            refresh();
        });

        JPanel atas = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        atas.add(new JLabel("Tanggal"));
        atas.add(spTanggal);
        atas.add(new JLabel("Jenis"));
        atas.add(cbJenis);
        atas.add(new JLabel("    "));
        for (StatusSlot s : StatusSlot.values()) {
            atas.add(kotakLegenda(s));
        }

        JButton btnKonfirmasi = new JButton("Konfirmasi Booking");
        btnKonfirmasi.addActionListener(e -> konfirmasi());
        JButton btnReset = new JButton("Reset Pilihan");
        btnReset.addActionListener(e -> {
            resetPilihan();
            gambarGrid();
        });
        lblRingkasan.setFont(lblRingkasan.getFont().deriveFont(Font.BOLD, 14f));

        JPanel aksi = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        aksi.add(new JLabel("Pelanggan"));
        aksi.add(pelangganPicker);
        aksi.add(btnReset);
        aksi.add(btnKonfirmasi);

        JPanel bawah = new JPanel(new BorderLayout(0, 6));
        bawah.add(lblRingkasan, BorderLayout.NORTH);
        bawah.add(aksi, BorderLayout.SOUTH);

        add(atas, BorderLayout.NORTH);
        add(new JScrollPane(grid), BorderLayout.CENTER);
        add(bawah, BorderLayout.SOUTH);

        refresh();
        new Timer(30_000, e -> {   // slot yang lewat & booking dari komputer lain ikut ter-update
            if (isShowing()) {
                refresh();
            }
        }).start();
    }

    @Override
    public void refresh() {
        try {
            daftarFasilitas = fasilitasDao.semua();
            Object jenis = cbJenis.getSelectedItem();
            if (jenis instanceof JenisFasilitas j) {
                daftarFasilitas.removeIf(f -> f.getJenis() != j);
            }
            bookingHariItu = pemesananDao.jadwalPada(tanggal());
            pelangganPicker.muat();
            gambarGrid();
        } catch (Exception ex) {
            Pesan.error(this, ex);
        }
    }

    // ------------------------------------------------------------------
    // Menggambar grid
    // ------------------------------------------------------------------

    private void gambarGrid() {
        int kolom = daftarFasilitas.size() + 1;
        grid.removeAll();
        grid.setLayout(new GridLayout(0, kolom, 3, 3));

        grid.add(header("Waktu"));
        for (Fasilitas f : daftarFasilitas) {
            grid.add(header("<html><center>" + f.getKode() + "<br><small>"
                    + Format.rupiah(f.getTarifPerJam()) + "/jam</small></center></html>"));
        }
        for (int jam = PemesananService.JAM_BUKA; jam < PemesananService.JAM_TUTUP; jam++) {
            grid.add(header(String.format("%02d:00", jam)));
            for (Fasilitas f : daftarFasilitas) {
                grid.add(kotakSlot(f, jam));
            }
        }
        perbaruiRingkasan();
        grid.revalidate();
        grid.repaint();
    }

    private JLabel kotakSlot(Fasilitas f, int jam) {
        StatusSlot status = statusSlot(f, jam);
        JLabel kotak = new JLabel(status.label, SwingConstants.CENTER);
        kotak.setOpaque(true);
        kotak.setBackground(status.warna);
        kotak.setForeground(status == StatusSlot.TUTUP ? Tema.TEKS_REDUP : Tema.teksDiAtas(status.warna));
        kotak.setFont(kotak.getFont().deriveFont(11f));
        Pemesanan b = bookingDi(f, jam);
        if (b != null) {
            kotak.setToolTipText("Dibooking: " + b.getPelanggan().getNama());
        }
        if (status == StatusSlot.TERSEDIA || status == StatusSlot.DIPILIH) {
            kotak.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            kotak.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    klikSlot(f, jam);
                }
            });
        }
        return kotak;
    }

    /** Menentukan status satu kotak (fasilitas f, jam tertentu pada tanggal terpilih). */
    private StatusSlot statusSlot(Fasilitas f, int jam) {
        LocalDateTime mulai = tanggal().atTime(jam, 0);
        LocalDateTime selesai = mulai.plusHours(1);
        LocalDateTime sekarang = LocalDateTime.now();

        if (!selesai.isAfter(sekarang) || f.getStatus() == StatusFasilitas.MAINTENANCE) {
            return StatusSlot.TUTUP;
        }
        if (bookingDi(f, jam) != null) {
            return StatusSlot.DIBOOKING;
        }
        boolean jamSekarang = !mulai.isAfter(sekarang) && selesai.isAfter(sekarang);
        if (jamSekarang && f.getStatus() == StatusFasilitas.DIPAKAI) {
            return StatusSlot.DIPAKAI;
        }
        if (fasilitasDipilih != null && fasilitasDipilih.getId() == f.getId() && jamDipilih.contains(jam)) {
            return StatusSlot.DIPILIH;
        }
        return StatusSlot.TERSEDIA;
    }

    private Pemesanan bookingDi(Fasilitas f, int jam) {
        LocalDateTime mulai = tanggal().atTime(jam, 0);
        for (Pemesanan p : bookingHariItu) {
            if (p.getFasilitas().getId() == f.getId() && p.bentrokDengan(mulai, mulai.plusHours(1))) {
                return p;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Memilih slot & konfirmasi
    // ------------------------------------------------------------------

    private void klikSlot(Fasilitas f, int jam) {
        if (fasilitasDipilih == null || fasilitasDipilih.getId() != f.getId()) {
            resetPilihan();       // pindah fasilitas = mulai pilihan baru
            fasilitasDipilih = f;
        }
        if (!jamDipilih.remove(jam)) {
            jamDipilih.add(jam);
        }
        if (jamDipilih.isEmpty()) {
            fasilitasDipilih = null;
        }
        gambarGrid();
    }

    private boolean pilihanBerurutan() {
        return jamDipilih.last() - jamDipilih.first() + 1 == jamDipilih.size();
    }

    private void perbaruiRingkasan() {
        if (fasilitasDipilih == null) {
            lblRingkasan.setText("Klik kotak hijau untuk memilih slot. Bisa beberapa jam berurutan.");
            return;
        }
        int jumlah = jamDipilih.size();
        double estimasi = fasilitasDipilih.hitungBiaya(jumlah * 60L);
        lblRingkasan.setText(String.format("%s  |  %d slot  |  %02d:00 - %02d:00  |  Estimasi %s%s",
                fasilitasDipilih, jumlah, jamDipilih.first(), jamDipilih.last() + 1, Format.rupiah(estimasi),
                pilihanBerurutan() ? "" : "  (jam harus berurutan!)"));
    }

    private void konfirmasi() {
        Pelanggan pelanggan = pelangganPicker.getTerpilih();
        if (fasilitasDipilih == null) {
            Pesan.info(this, "Pilih minimal satu slot di grid.");
            return;
        }
        if (!pilihanBerurutan()) {
            Pesan.info(this, "Slot yang dipilih harus berurutan, misalnya 13:00, 14:00, 15:00.");
            return;
        }
        if (pelanggan == null) {
            Pesan.info(this, "Pilih pelanggan terlebih dahulu.");
            return;
        }
        LocalDateTime mulai = tanggal().atTime(jamDipilih.first(), 0);
        LocalDateTime selesai = tanggal().atTime(jamDipilih.last(), 0).plusHours(1);
        try {
            Pemesanan p = service.pesanJadwal(pelanggan, fasilitasDipilih, mulai, selesai);
            Pesan.info(this, "Booking berhasil!\n\n"
                    + "Pelanggan : " + pelanggan.getNama() + "\n"
                    + "Fasilitas : " + p.getFasilitas() + "\n"
                    + "Jadwal    : " + Format.waktu(mulai) + " - " + String.format("%02d:00", selesai.getHour()) + "\n"
                    + "Estimasi  : " + Format.rupiah(p.getEstimasiBiaya()) + "\n\n"
                    + "Saat pelanggan datang, lakukan Check-in di tab Sesi Aktif.\n"
                    + "Biaya akhir dihitung dari timer.");
            resetPilihan();
        } catch (Exception ex) {
            Pesan.error(this, ex);
        }
        refresh();
    }

    private void resetPilihan() {
        fasilitasDipilih = null;
        jamDipilih.clear();
    }

    // ------------------------------------------------------------------
    // Helper tampilan
    // ------------------------------------------------------------------

    private LocalDate tanggal() {
        return ((Date) spTanggal.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static JLabel header(String teks) {
        JLabel l = new JLabel(teks, SwingConstants.CENTER);
        l.setForeground(Tema.TEKS);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        return l;
    }

    private static JLabel kotakLegenda(StatusSlot s) {
        JLabel l = new JLabel("  " + s.label + "  ");
        l.setOpaque(true);
        l.setBackground(s.warna);
        l.setForeground(s == StatusSlot.TUTUP ? Tema.TEKS_REDUP : Tema.teksDiAtas(s.warna));
        return l;
    }
}
