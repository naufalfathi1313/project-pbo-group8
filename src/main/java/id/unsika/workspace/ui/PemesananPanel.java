package id.unsika.workspace.ui;

import id.unsika.workspace.dao.FasilitasDao;
import id.unsika.workspace.model.Fasilitas;
import id.unsika.workspace.model.JenisFasilitas;
import id.unsika.workspace.model.Pelanggan;
import id.unsika.workspace.model.Pemesanan;
import id.unsika.workspace.service.PemesananService;
import id.unsika.workspace.util.Format;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/** Walk-in: pelanggan datang tanpa booking dan langsung main. Booking jam tertentu ada di tab Jadwal. */
public class PemesananPanel extends JPanel implements Refreshable {

    private static final String SEMUA = "Semua jenis";

    private final FasilitasDao fasilitasDao = new FasilitasDao();
    private final PemesananService service = new PemesananService();

    private final PelangganPicker pelangganPicker = new PelangganPicker();
    private final JComboBox<Object> cbJenis = new JComboBox<>();
    private final JComboBox<Fasilitas> cbFasilitas = new JComboBox<>();
    private final JLabel lblInfo = new JLabel(" ");

    public PemesananPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));

        cbJenis.addItem(SEMUA);
        for (JenisFasilitas j : JenisFasilitas.values()) {
            cbJenis.addItem(j);
        }
        cbJenis.addActionListener(e -> muatFasilitas());
        cbFasilitas.addActionListener(e -> tampilkanInfo());

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        baris(form, c, 0, "Pelanggan", pelangganPicker);
        baris(form, c, 1, "Jenis", cbJenis);
        baris(form, c, 2, "Fasilitas tersedia", cbFasilitas);
        baris(form, c, 3, "Keterangan", lblInfo);

        JButton btnMulai = Tema.tombolUtama("Mulai Sekarang");
        btnMulai.addActionListener(e -> mulai());
        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tombol.setOpaque(false);
        tombol.add(btnMulai);
        baris(form, c, 4, "", tombol);

        Kartu kartu = new Kartu(new BorderLayout(0, 12));
        kartu.add(Tema.label("Mulai sesi tanpa booking", 15f, java.awt.Font.BOLD, Tema.TEKS), BorderLayout.NORTH);
        kartu.add(form, BorderLayout.CENTER);
        JPanel batas = new JPanel(new BorderLayout()); // batasi lebar kartu supaya form tidak terlalu melebar
        batas.add(kartu, BorderLayout.WEST);
        kartu.setPreferredSize(new java.awt.Dimension(720, kartu.getPreferredSize().height));
        add(batas, BorderLayout.NORTH);
        refresh();
    }

    private void baris(JPanel form, GridBagConstraints c, int y, String label, Component komponen) {
        c.gridy = y;
        c.gridx = 0;
        c.weightx = 0;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(komponen, c);
    }

    @Override
    public void refresh() {
        pelangganPicker.muat();
        muatFasilitas();
    }

    private void muatFasilitas() {
        try {
            List<Fasilitas> tersedia = fasilitasDao.tersedia();
            Object jenis = cbJenis.getSelectedItem();
            if (jenis instanceof JenisFasilitas j) {
                tersedia.removeIf(f -> f.getJenis() != j);
            }
            cbFasilitas.setModel(new DefaultComboBoxModel<>(tersedia.toArray(new Fasilitas[0])));
            tampilkanInfo();
        } catch (Exception ex) {
            Pesan.error(this, ex);
        }
    }

    private void tampilkanInfo() {
        Fasilitas f = (Fasilitas) cbFasilitas.getSelectedItem();
        lblInfo.setText(f == null ? "Tidak ada fasilitas tersedia"
                : Format.rupiah(f.getTarifPerJam()) + " / jam  |  " + f.getInfoTambahan());
    }

    private void mulai() {
        Pelanggan pelanggan = pelangganPicker.getTerpilih();
        Fasilitas fasilitas = (Fasilitas) cbFasilitas.getSelectedItem();
        if (pelanggan == null || fasilitas == null) {
            Pesan.info(this, "Pilih pelanggan dan fasilitas terlebih dahulu.");
            return;
        }
        try {
            Pemesanan p = service.mulaiSekarang(pelanggan, fasilitas);
            Pesan.info(this, "Timer dimulai untuk " + p.getFasilitas().getKode() + " atas nama "
                    + pelanggan.getNama() + ".\nPantau di tab Sesi Aktif.");
        } catch (Exception ex) {
            Pesan.error(this, ex);
        }
        muatFasilitas();
    }
}
