package id.unsika.workspace.ui;

import id.unsika.workspace.dao.PemesananDao;
import id.unsika.workspace.model.JenisFasilitas;
import id.unsika.workspace.model.Pemesanan;
import id.unsika.workspace.util.Format;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Rekap transaksi yang sudah selesai per periode, beserta total pendapatan. */
public class RekapPanel extends JPanel implements Refreshable {

    private static final String SEMUA = "Semua jenis";

    private final PemesananDao pemesananDao = new PemesananDao();
    private final JSpinner spDari = tanggal(LocalDate.now().minusDays(7));
    private final JSpinner spSampai = tanggal(LocalDate.now());
    private final JComboBox<Object> cbJenis = new JComboBox<>();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Selesai", "Pelanggan", "Fasilitas", "Jenis", "Durasi", "Total"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel lblTotal = new JLabel();
    private final JLabel lblPerJenis = new JLabel();

    public RekapPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        cbJenis.addItem(SEMUA);
        for (JenisFasilitas j : JenisFasilitas.values()) {
            cbJenis.addItem(j);
        }

        JButton btnTampil = new JButton("Tampilkan");
        btnTampil.addActionListener(e -> refresh());

        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filter.add(new JLabel("Dari"));
        filter.add(spDari);
        filter.add(new JLabel("Sampai"));
        filter.add(spSampai);
        filter.add(new JLabel("Jenis"));
        filter.add(cbJenis);
        filter.add(btnTampil);

        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 15f));
        JPanel bawah = new JPanel(new BorderLayout());
        bawah.add(lblTotal, BorderLayout.NORTH);
        bawah.add(lblPerJenis, BorderLayout.SOUTH);

        add(filter, BorderLayout.NORTH);
        add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        add(bawah, BorderLayout.SOUTH);
        refresh();
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

            model.setRowCount(0);
            double total = 0;
            long totalDetik = 0;
            Map<JenisFasilitas, Double> perJenis = new EnumMap<>(JenisFasilitas.class);

            for (Pemesanan p : daftar) {
                JenisFasilitas jenis = p.getFasilitas().getJenis();
                if (jenisDipilih instanceof JenisFasilitas j && jenis != j) {
                    continue;
                }
                model.addRow(new Object[]{p.getId(), Format.waktu(p.getWaktuSelesai()), p.getPelanggan().getNama(),
                        p.getFasilitas().getKode(), jenis.getLabel(), Format.durasi(p.getDurasiDetik()),
                        Format.rupiah(p.getTotalBiaya())});
                total += p.getTotalBiaya();
                totalDetik += p.getDurasiDetik();
                perJenis.merge(jenis, p.getTotalBiaya(), Double::sum);
            }

            lblTotal.setText("Total pendapatan: " + Format.rupiah(total) + "   |   "
                    + model.getRowCount() + " transaksi   |   Total pemakaian " + Format.durasi(totalDetik));
            StringBuilder sb = new StringBuilder("Per jenis: ");
            for (JenisFasilitas j : JenisFasilitas.values()) {
                sb.append(j.getLabel()).append(" ").append(Format.rupiah(perJenis.getOrDefault(j, 0.0))).append("   ");
            }
            lblPerJenis.setText(sb.toString());
        } catch (Exception ex) {
            Pesan.error(this, ex);
        }
    }

    private static JSpinner tanggal(LocalDate awal) {
        Date d = Date.from(awal.atStartOfDay(ZoneId.systemDefault()).toInstant());
        JSpinner sp = new JSpinner(new SpinnerDateModel(d, null, null, java.util.Calendar.DAY_OF_MONTH));
        sp.setEditor(new JSpinner.DateEditor(sp, "dd/MM/yyyy"));
        return sp;
    }

    private static LocalDate keLocalDate(JSpinner sp) {
        return ((Date) sp.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
