package id.unsika.workspace.ui;

import id.unsika.workspace.dao.PemesananDao;
import id.unsika.workspace.model.JenisFasilitas;
import id.unsika.workspace.model.Pemesanan;
import id.unsika.workspace.util.Format;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SpinnerDateModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Rekap transaksi selesai: angka ringkasan, grafik pendapatan, dan tabel detail. */
public class RekapPanel extends JPanel implements Refreshable {

    private static final String SEMUA = "Semua jenis";
    private static final int MAKS_HARI_HARIAN = 45; // di atas ini, grafik dikelompokkan per bulan

    private final PemesananDao pemesananDao = new PemesananDao();

    private final JSpinner spDari = tanggal(LocalDate.now().minusDays(13));
    private final JSpinner spSampai = tanggal(LocalDate.now());
    private final JComboBox<Object> cbJenis = new JComboBox<>();

    private final StatTile tileTotal = new StatTile("Total pendapatan");
    private final StatTile tileTransaksi = new StatTile("Transaksi");
    private final StatTile tileJam = new StatTile("Total pemakaian");
    private final StatTile tileRata = new StatTile("Rata-rata / transaksi");

    private final GrafikKolom grafikHarian = new GrafikKolom();
    private final GrafikBatang grafikJenis = new GrafikBatang();
    private final JLabel lblSubHarian = Tema.label(" ", 12f, Font.PLAIN, Tema.TEKS_REDUP);

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Selesai", "Pelanggan", "Fasilitas", "Jenis", "Durasi", "Total"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public RekapPanel() {
        super(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));

        cbJenis.addItem(SEMUA);
        for (JenisFasilitas j : JenisFasilitas.values()) {
            cbJenis.addItem(j);
        }
        cbJenis.addActionListener(e -> refresh());

        add(barisFilter(), BorderLayout.NORTH);

        JPanel isi = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 16, 0);

        JPanel tiles = new JPanel(new GridLayout(1, 4, 16, 0));
        tiles.add(tileTotal);
        tiles.add(tileTransaksi);
        tiles.add(tileJam);
        tiles.add(tileRata);
        c.gridy = 0;
        isi.add(tiles, c);

        JPanel grafik = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        g.gridx = 0;
        g.weightx = 0.62;
        g.insets = new Insets(0, 0, 0, 16);
        grafik.add(kartuGrafik("Pendapatan per hari", lblSubHarian, grafikHarian), g);
        g.gridx = 1;
        g.weightx = 0.38;
        g.insets = new Insets(0, 0, 0, 0);
        grafik.add(kartuGrafik("Pendapatan per jenis", Tema.label("Arahkan mouse ke batang untuk detail", 12f, Font.PLAIN, Tema.TEKS_REDUP), grafikJenis), g);
        c.gridy = 1;
        isi.add(grafik, c);

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        Kartu kartuTabel = new Kartu(new BorderLayout(0, 12));
        kartuTabel.add(Tema.label("Detail transaksi", 15f, Font.BOLD, Tema.TEKS), BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(400, 260));
        kartuTabel.add(sp, BorderLayout.CENTER);
        c.gridy = 2;
        c.insets = new Insets(0, 0, 0, 0);
        isi.add(kartuTabel, c);

        JScrollPane gulir = new JScrollPane(new PanelGulir(isi));
        gulir.setBorder(null);
        gulir.getVerticalScrollBar().setUnitIncrement(16);
        add(gulir, BorderLayout.CENTER);
        refresh();
    }

    private JPanel barisFilter() {
        JPanel baris = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ButtonGroup grup = new ButtonGroup();
        JToggleButton c7 = Tema.chip("14 hari");
        JToggleButton c30 = Tema.chip("30 hari");
        JToggleButton cBulan = Tema.chip("Bulan ini");
        c7.setSelected(true);
        c7.addActionListener(e -> setRentang(LocalDate.now().minusDays(13), LocalDate.now()));
        c30.addActionListener(e -> setRentang(LocalDate.now().minusDays(29), LocalDate.now()));
        cBulan.addActionListener(e -> setRentang(LocalDate.now().withDayOfMonth(1), LocalDate.now()));
        for (JToggleButton b : new JToggleButton[]{c7, c30, cBulan}) {
            grup.add(b);
            baris.add(b);
        }
        baris.add(Box.createHorizontalStrut(12));
        baris.add(Tema.label("Dari", 13f, Font.PLAIN, Tema.TEKS_REDUP));
        baris.add(spDari);
        baris.add(Tema.label("Sampai", 13f, Font.PLAIN, Tema.TEKS_REDUP));
        baris.add(spSampai);
        baris.add(cbJenis);
        JButton btn = new JButton("Tampilkan");
        btn.addActionListener(e -> {
            grup.clearSelection(); // rentang manual: tidak ada chip yang aktif
            refresh();
        });
        baris.add(btn);
        return baris;
    }

    private void setRentang(LocalDate dari, LocalDate sampai) {
        spDari.setValue(keDate(dari));
        spSampai.setValue(keDate(sampai));
        refresh();
    }

    private static Kartu kartuGrafik(String judul, JLabel sub, JComponent grafik) {
        Kartu k = new Kartu(new BorderLayout(0, 14));
        JPanel kepala = new JPanel(new GridLayout(2, 1));
        kepala.setOpaque(false);
        kepala.add(Tema.label(judul, 15f, Font.BOLD, Tema.TEKS));
        kepala.add(sub);
        k.add(kepala, BorderLayout.NORTH);
        k.add(grafik, BorderLayout.CENTER);
        return k;
    }

    @Override
    public void refresh() {
        LocalDate dari = keLocalDate(spDari);
        LocalDate sampai = keLocalDate(spSampai);
        if (dari.isAfter(sampai)) {
            Pesan.info(this, "Tanggal 'Dari' tidak boleh setelah tanggal 'Sampai'.");
            return;
        }
        try {
            List<Pemesanan> daftar = pemesananDao.rekap(dari, sampai);
            Object jenisDipilih = cbJenis.getSelectedItem();
            if (jenisDipilih instanceof JenisFasilitas j) {
                daftar.removeIf(p -> p.getFasilitas().getJenis() != j);
            }
            isiTabel(daftar);
            isiRingkasan(daftar);
            isiGrafikHarian(daftar, dari, sampai);
            isiGrafikJenis(daftar);
        } catch (Exception ex) {
            Pesan.error(this, ex);
        }
    }

    private void isiTabel(List<Pemesanan> daftar) {
        model.setRowCount(0);
        for (Pemesanan p : daftar) {
            model.addRow(new Object[]{p.getId(), Format.waktu(p.getWaktuSelesai()), p.getPelanggan().getNama(),
                    p.getFasilitas().getKode(), p.getFasilitas().getJenis().getLabel(),
                    Format.durasi(p.getDurasiDetik()), Format.rupiah(p.getTotalBiaya())});
        }
    }

    private void isiRingkasan(List<Pemesanan> daftar) {
        double total = 0;
        long detik = 0;
        for (Pemesanan p : daftar) {
            total += p.getTotalBiaya();
            detik += p.getDurasiDetik();
        }
        int n = daftar.size();
        tileTotal.set(Format.rupiah(total), "periode yang dipilih");
        tileTransaksi.set(String.valueOf(n), n == 0 ? "belum ada" : "sesi selesai");
        tileJam.set(String.format(new java.util.Locale("id", "ID"), "%.1f jam", detik / 3600.0), "durasi dari timer");
        tileRata.set(n == 0 ? "-" : Format.rupiah(total / n), "per sesi");
    }

    /** Menjumlahkan pendapatan per hari (atau per bulan jika rentangnya panjang). Hari kosong tetap tampil sebagai 0. */
    private void isiGrafikHarian(List<Pemesanan> daftar, LocalDate dari, LocalDate sampai) {
        boolean perBulan = ChronoUnit.DAYS.between(dari, sampai) + 1 > MAKS_HARI_HARIAN;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(perBulan ? "MM/yy" : "dd/MM");
        Map<String, Double> kelompok = new LinkedHashMap<>();
        for (LocalDate d = dari; !d.isAfter(sampai); d = d.plusDays(1)) {
            kelompok.putIfAbsent(d.format(fmt), 0.0);
        }
        for (Pemesanan p : daftar) {
            kelompok.merge(p.getWaktuSelesai().toLocalDate().format(fmt), p.getTotalBiaya(), Double::sum);
        }
        String[] label = kelompok.keySet().toArray(new String[0]);
        double[] nilai = kelompok.values().stream().mapToDouble(Double::doubleValue).toArray();
        grafikHarian.setData(label, nilai);
        lblSubHarian.setText(perBulan ? "Dikelompokkan per bulan" : "Arahkan mouse ke kolom untuk detail");
    }

    private void isiGrafikJenis(List<Pemesanan> daftar) {
        Map<JenisFasilitas, Double> total = new EnumMap<>(JenisFasilitas.class);
        Map<JenisFasilitas, Integer> jumlah = new EnumMap<>(JenisFasilitas.class);
        for (Pemesanan p : daftar) {
            JenisFasilitas j = p.getFasilitas().getJenis();
            total.merge(j, p.getTotalBiaya(), Double::sum);
            jumlah.merge(j, 1, Integer::sum);
        }
        JenisFasilitas[] semua = JenisFasilitas.values();
        String[] label = new String[semua.length];
        double[] nilai = new double[semua.length];
        String[] ket = new String[semua.length];
        for (int i = 0; i < semua.length; i++) {
            label[i] = semua[i].getLabel();
            nilai[i] = total.getOrDefault(semua[i], 0.0);
            ket[i] = jumlah.getOrDefault(semua[i], 0) + " transaksi";
        }
        grafikJenis.setData(label, nilai, ket);
    }

    private static JSpinner tanggal(LocalDate awal) {
        JSpinner sp = new JSpinner(new SpinnerDateModel(keDate(awal), null, null, Calendar.DAY_OF_MONTH));
        sp.setEditor(new JSpinner.DateEditor(sp, "dd/MM/yyyy"));
        return sp;
    }

    private static Date keDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate keLocalDate(JSpinner sp) {
        return ((Date) sp.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
