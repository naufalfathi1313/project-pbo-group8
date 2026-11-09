package id.unsika.workspace.ui;

import id.unsika.workspace.dao.FasilitasDao;
import id.unsika.workspace.dao.PelangganDao;
import id.unsika.workspace.model.Fasilitas;
import id.unsika.workspace.model.JenisFasilitas;
import id.unsika.workspace.model.MeetingRoom;
import id.unsika.workspace.model.MejaDesk;
import id.unsika.workspace.model.PCGaming;
import id.unsika.workspace.model.Pelanggan;
import id.unsika.workspace.model.StatusFasilitas;
import id.unsika.workspace.model.TipePC;
import id.unsika.workspace.util.Format;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/** CRUD (tambah, ubah, hapus) data fasilitas dan pelanggan. */
public class KelolaDataPanel extends JPanel implements Refreshable {

    private final FasilitasForm fasilitasForm = new FasilitasForm();
    private final PelangganForm pelangganForm = new PelangganForm();

    public KelolaDataPanel() {
        super(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Fasilitas", fasilitasForm);
        tabs.addTab("Pelanggan", pelangganForm);
        add(tabs);
        refresh();
    }

    @Override
    public void refresh() {
        fasilitasForm.muat();
        pelangganForm.muat();
    }

    /** Tabel tidak bisa diedit langsung; perubahan lewat form di bawahnya. */
    private static DefaultTableModel modelTabel(String... kolom) {
        return new DefaultTableModel(kolom, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static void tampilkanErrorHapus(Component parent, Exception ex) {
        if (ex instanceof SQLIntegrityConstraintViolationException) {
            Pesan.info(parent, "Data tidak bisa dihapus karena sudah punya riwayat transaksi.");
        } else {
            Pesan.error(parent, ex);
        }
    }

    // =====================================================================
    // Form Fasilitas
    // =====================================================================
    private static class FasilitasForm extends JPanel {

        private final FasilitasDao dao = new FasilitasDao();
        private final DefaultTableModel model = modelTabel("ID", "Kode", "Nama", "Jenis", "Tarif/Jam", "Status", "Keterangan");
        private final JTable table = new JTable(model);
        private List<Fasilitas> daftar = new ArrayList<>();

        private final JTextField txtKode = new JTextField();
        private final JTextField txtNama = new JTextField();
        private final JComboBox<JenisFasilitas> cbJenis = new JComboBox<>(JenisFasilitas.values());
        private final JTextField txtTarif = new JTextField();
        private final JCheckBox chkMaintenance = new JCheckBox("Sedang maintenance");
        private final JCheckBox chkStopKontak = new JCheckBox("Ada stop kontak");
        private final JSpinner spKapasitas = new JSpinner(new SpinnerNumberModel(4, 1, 100, 1));
        private final JTextField txtSpesifikasi = new JTextField();
        private final JComboBox<TipePC> cbTipe = new JComboBox<>(TipePC.values());

        private Fasilitas sedangDiedit = null;

        FasilitasForm() {
            super(new BorderLayout(10, 10));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                    isiForm(daftar.get(table.getSelectedRow()));
                }
            });
            cbJenis.addActionListener(e -> aturFieldKhusus());

            JPanel form = new JPanel(new GridLayout(0, 4, 8, 6));
            form.add(new JLabel("Kode"));
            form.add(txtKode);
            form.add(new JLabel("Nama"));
            form.add(txtNama);
            form.add(new JLabel("Jenis"));
            form.add(cbJenis);
            form.add(new JLabel("Tarif per jam (Rp)"));
            form.add(txtTarif);
            form.add(new JLabel("Meja:"));
            form.add(chkStopKontak);
            form.add(new JLabel("Meeting: kapasitas"));
            form.add(spKapasitas);
            form.add(new JLabel("PC: spesifikasi"));
            form.add(txtSpesifikasi);
            form.add(new JLabel("PC: tipe"));
            form.add(cbTipe);
            form.add(new JLabel("Status"));
            form.add(chkMaintenance);

            JButton btnBaru = new JButton("Baru");
            btnBaru.addActionListener(e -> kosongkan());
            JButton btnSimpan = new JButton("Simpan");
            btnSimpan.addActionListener(e -> simpan());
            JButton btnHapus = new JButton("Hapus");
            btnHapus.addActionListener(e -> hapus());
            JPanel tombol = new JPanel(new FlowLayout(FlowLayout.LEFT));
            tombol.add(btnBaru);
            tombol.add(btnSimpan);
            tombol.add(btnHapus);

            JPanel bawah = new JPanel(new BorderLayout());
            bawah.add(form, BorderLayout.CENTER);
            bawah.add(tombol, BorderLayout.SOUTH);

            add(new JScrollPane(table), BorderLayout.CENTER);
            add(bawah, BorderLayout.SOUTH);
            kosongkan();
        }

        void muat() {
            try {
                daftar = dao.semua();
                model.setRowCount(0);
                for (Fasilitas f : daftar) {
                    model.addRow(new Object[]{f.getId(), f.getKode(), f.getNama(), f.getJenis().getLabel(),
                            Format.rupiah(f.getTarifPerJam()), f.getStatus(), f.getInfoTambahan()});
                }
            } catch (Exception ex) {
                Pesan.error(this, ex);
            }
        }

        /** Hanya field yang relevan dengan jenis terpilih yang bisa diisi. */
        private void aturFieldKhusus() {
            JenisFasilitas j = (JenisFasilitas) cbJenis.getSelectedItem();
            chkStopKontak.setEnabled(j == JenisFasilitas.MEJA);
            spKapasitas.setEnabled(j == JenisFasilitas.MEETING);
            txtSpesifikasi.setEnabled(j == JenisFasilitas.PC);
            cbTipe.setEnabled(j == JenisFasilitas.PC);
        }

        private void isiForm(Fasilitas f) {
            sedangDiedit = f;
            txtKode.setText(f.getKode());
            txtNama.setText(f.getNama());
            cbJenis.setSelectedItem(f.getJenis());
            txtTarif.setText(String.valueOf((long) f.getTarifPerJam()));
            chkMaintenance.setSelected(f.getStatus() == StatusFasilitas.MAINTENANCE);
            if (f instanceof MejaDesk meja) {
                chkStopKontak.setSelected(meja.isAdaStopKontak());
            } else if (f instanceof MeetingRoom room) {
                spKapasitas.setValue(room.getKapasitas());
            } else if (f instanceof PCGaming pc) {
                txtSpesifikasi.setText(pc.getSpesifikasi());
                cbTipe.setSelectedItem(pc.getTipe());
            }
        }

        private void kosongkan() {
            sedangDiedit = null;
            table.clearSelection();
            txtKode.setText("");
            txtNama.setText("");
            txtTarif.setText("");
            txtSpesifikasi.setText("");
            chkMaintenance.setSelected(false);
            chkStopKontak.setSelected(false);
            aturFieldKhusus();
        }

        private void simpan() {
            try {
                int id = (sedangDiedit == null) ? 0 : sedangDiedit.getId();
                StatusFasilitas statusLama = (sedangDiedit == null) ? StatusFasilitas.TERSEDIA : sedangDiedit.getStatus();
                if (statusLama == StatusFasilitas.DIPAKAI || statusLama == StatusFasilitas.DIPESAN) {
                    Pesan.info(this, "Fasilitas sedang dipesan/dipakai. Selesaikan sesinya dulu sebelum mengubah data.");
                    return;
                }
                StatusFasilitas status = chkMaintenance.isSelected() ? StatusFasilitas.MAINTENANCE : StatusFasilitas.TERSEDIA;
                double tarif;
                try {
                    tarif = Double.parseDouble(txtTarif.getText().trim());
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Tarif harus berupa angka, contoh: 12000");
                }
                String kode = txtKode.getText();
                String nama = txtNama.getText();

                // Membuat object subclass sesuai jenis yang dipilih
                Fasilitas f = switch ((JenisFasilitas) cbJenis.getSelectedItem()) {
                    case MEJA -> new MejaDesk(id, kode, nama, tarif, status, chkStopKontak.isSelected());
                    case MEETING -> new MeetingRoom(id, kode, nama, tarif, status, (Integer) spKapasitas.getValue());
                    case PC -> new PCGaming(id, kode, nama, tarif, status, txtSpesifikasi.getText().trim(),
                            (TipePC) cbTipe.getSelectedItem());
                };
                dao.simpan(f);
                muat();
                kosongkan();
            } catch (SQLIntegrityConstraintViolationException ex) {
                Pesan.info(this, "Kode fasilitas sudah dipakai. Gunakan kode lain.");
            } catch (Exception ex) {
                Pesan.error(this, ex);
            }
        }

        private void hapus() {
            if (sedangDiedit == null) {
                Pesan.info(this, "Pilih fasilitas di tabel terlebih dahulu.");
                return;
            }
            if (!Pesan.konfirmasi(this, "Hapus " + sedangDiedit.getKode() + "?")) {
                return;
            }
            try {
                dao.hapus(sedangDiedit.getId());
                muat();
                kosongkan();
            } catch (Exception ex) {
                tampilkanErrorHapus(this, ex);
            }
        }
    }

    // =====================================================================
    // Form Pelanggan
    // =====================================================================
    private static class PelangganForm extends JPanel {

        private final PelangganDao dao = new PelangganDao();
        private final DefaultTableModel model = modelTabel("ID", "Nama", "No HP");
        private final JTable table = new JTable(model);
        private List<Pelanggan> daftar = new ArrayList<>();
        private final JTextField txtNama = new JTextField(18);
        private final JTextField txtHp = new JTextField(14);
        private Pelanggan sedangDiedit = null;

        PelangganForm() {
            super(new BorderLayout(10, 10));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                    sedangDiedit = daftar.get(table.getSelectedRow());
                    txtNama.setText(sedangDiedit.getNama());
                    txtHp.setText(sedangDiedit.getNoHp());
                }
            });

            JButton btnBaru = new JButton("Baru");
            btnBaru.addActionListener(e -> kosongkan());
            JButton btnSimpan = new JButton("Simpan");
            btnSimpan.addActionListener(e -> simpan());
            JButton btnHapus = new JButton("Hapus");
            btnHapus.addActionListener(e -> hapus());

            JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
            form.add(new JLabel("Nama"));
            form.add(txtNama);
            form.add(new JLabel("No HP"));
            form.add(txtHp);
            form.add(btnBaru);
            form.add(btnSimpan);
            form.add(btnHapus);

            add(new JScrollPane(table), BorderLayout.CENTER);
            add(form, BorderLayout.SOUTH);
        }

        void muat() {
            try {
                daftar = dao.semua();
                model.setRowCount(0);
                for (Pelanggan p : daftar) {
                    model.addRow(new Object[]{p.getId(), p.getNama(), p.getNoHp()});
                }
            } catch (Exception ex) {
                Pesan.error(this, ex);
            }
        }

        private void kosongkan() {
            sedangDiedit = null;
            table.clearSelection();
            txtNama.setText("");
            txtHp.setText("");
        }

        private void simpan() {
            try {
                int id = (sedangDiedit == null) ? 0 : sedangDiedit.getId();
                dao.simpan(new Pelanggan(id, txtNama.getText(), txtHp.getText()));
                muat();
                kosongkan();
            } catch (Exception ex) {
                Pesan.error(this, ex);
            }
        }

        private void hapus() {
            if (sedangDiedit == null) {
                Pesan.info(this, "Pilih pelanggan di tabel terlebih dahulu.");
                return;
            }
            if (!Pesan.konfirmasi(this, "Hapus pelanggan " + sedangDiedit.getNama() + "?")) {
                return;
            }
            try {
                dao.hapus(sedangDiedit.getId());
                muat();
                kosongkan();
            } catch (Exception ex) {
                tampilkanErrorHapus(this, ex);
            }
        }
    }
}
