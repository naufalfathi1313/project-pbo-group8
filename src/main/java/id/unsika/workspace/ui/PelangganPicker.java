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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Komponen kecil: kolom cari + ComboBox pelanggan + tombol untuk menambah pelanggan baru. Dipakai di dua tab.
 * Mengetik di kolom cari menyaring isi ComboBox berdasarkan nama atau nomor HP.
 */
class PelangganPicker extends JPanel {

    private final PelangganDao dao = new PelangganDao();
    private final JComboBox<Pelanggan> combo = new JComboBox<>();
    private final JTextField txtCari = new JTextField(12);
    private List<Pelanggan> semua = new ArrayList<>(); // daftar lengkap dari database

    PelangganPicker() {
        super(new BorderLayout(6, 0));
        setOpaque(false);
        txtCari.putClientProperty("JTextField.placeholderText", "Cari nama / no HP");
        txtCari.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                saring();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                saring();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                saring();
            }
        });
        JButton btnBaru = new JButton("+ Pelanggan Baru");
        btnBaru.addActionListener(e -> tambah());
        add(txtCari, BorderLayout.WEST);
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
            semua = dao.semua();
            saring();
            if (lama != null) {
                pilih(lama.getId());
            }
        } catch (Exception ex) {
            Pesan.error(this, ex);
        }
    }

    /** Isi ComboBox hanya dengan pelanggan yang nama atau no HP-nya mengandung kata yang diketik. */
    private void saring() {
        String kata = txtCari.getText().trim().toLowerCase();
        List<Pelanggan> cocok = new ArrayList<>();
        for (Pelanggan p : semua) {
            if (p.getNama().toLowerCase().contains(kata) || p.getNoHp().contains(kata)) {
                cocok.add(p);
            }
        }
        combo.setModel(new DefaultComboBoxModel<>(cocok.toArray(new Pelanggan[0])));
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
            txtCari.setText(""); // kosongkan pencarian supaya pelanggan baru pasti terlihat
            muat();
            pilih(p.getId());
        } catch (Exception ex) {
            Pesan.error(this, ex);
        }
    }
}
