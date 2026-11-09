package id.unsika.workspace.ui;

import id.unsika.workspace.dao.PelangganDao;
import id.unsika.workspace.model.Pelanggan;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** Komponen kecil: ComboBox pelanggan + tombol untuk menambah pelanggan baru. Dipakai di dua tab. */
class PelangganPicker extends JPanel {

    private final PelangganDao dao = new PelangganDao();
    private final JComboBox<Pelanggan> combo = new JComboBox<>();

    PelangganPicker() {
        super(new BorderLayout(6, 0));
        setOpaque(false);
        JButton btnBaru = new JButton("+ Pelanggan Baru");
        btnBaru.addActionListener(e -> tambah());
        add(combo, BorderLayout.CENTER);
        add(btnBaru, BorderLayout.EAST);
    }

    Pelanggan getTerpilih() {
        return (Pelanggan) combo.getSelectedItem();
    }

    /** Muat ulang daftar dari database, dan pertahankan pilihan sebelumnya jika masih ada. */
    void muat() {
        try {
            Pelanggan lama = getTerpilih();
            combo.setModel(new DefaultComboBoxModel<>(dao.semua().toArray(new Pelanggan[0])));
            if (lama != null) {
                pilih(lama.getId());
            }
        } catch (Exception ex) {
            Pesan.error(this, ex);
        }
    }

    private void pilih(int id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).getId() == id) {
                combo.setSelectedIndex(i);
            }
        }
    }

    private void tambah() {
        JTextField txtNama = new JTextField(16);
        JTextField txtHp = new JTextField(16);
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Nama"));
        form.add(txtNama);
        form.add(new JLabel("No HP"));
        form.add(txtHp);
        if (JOptionPane.showConfirmDialog(this, form, "Pelanggan Baru", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            Pelanggan p = new Pelanggan(0, txtNama.getText(), txtHp.getText());
            dao.simpan(p);
            muat();
            pilih(p.getId());
        } catch (Exception ex) {
            Pesan.error(this, ex);
        }
    }
}
