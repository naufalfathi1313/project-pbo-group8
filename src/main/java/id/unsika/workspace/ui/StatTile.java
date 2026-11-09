package id.unsika.workspace.ui;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;

/** Kotak angka ringkasan: judul kecil, angka besar, keterangan di bawahnya. */
public class StatTile extends Kartu {

    private final JLabel lblNilai = Tema.label("-", 26f, Font.BOLD, Tema.TEKS);
    private final JLabel lblKeterangan = Tema.label(" ", 12f, Font.PLAIN, Tema.TEKS_REDUP);

    public StatTile(String judul) {
        super(new BorderLayout());
        JPanel isi = new JPanel();
        isi.setOpaque(false);
        isi.setLayout(new BoxLayout(isi, BoxLayout.Y_AXIS));
        isi.add(Tema.label(judul.toUpperCase(), 11f, Font.BOLD, Tema.TEKS_REDUP));
        isi.add(javax.swing.Box.createVerticalStrut(6));
        isi.add(lblNilai);
        isi.add(javax.swing.Box.createVerticalStrut(2));
        isi.add(lblKeterangan);
        add(isi, BorderLayout.CENTER);
    }

    public void set(String nilai, String keterangan) {
        lblNilai.setText(nilai);
        lblKeterangan.setText(keterangan == null || keterangan.isEmpty() ? " " : keterangan);
    }
}
