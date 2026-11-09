package id.unsika.workspace.ui;

import id.unsika.workspace.dao.FasilitasDao;
import id.unsika.workspace.dao.PemesananDao;
import id.unsika.workspace.model.Fasilitas;
import id.unsika.workspace.model.StatusFasilitas;
import id.unsika.workspace.util.Format;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Menampilkan semua fasilitas sebagai kartu berwarna sesuai status.
 * javax.swing.Timer memuat ulang data setiap 2 detik, jadi status selalu REAL-TIME,
 * termasuk perubahan yang dilakukan dari komputer kasir lain.
 */
public class DashboardPanel extends JPanel implements Refreshable {

    private static final int INTERVAL_MS = 2000;

    private final FasilitasDao fasilitasDao = new FasilitasDao();
    private final PemesananDao pemesananDao = new PemesananDao();
    private final JPanel grid = new JPanel(new GridLayout(0, 4, 10, 10));
    private final JLabel lblRingkasan = new JLabel();
    private boolean errorDitampilkan = false;

    public DashboardPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel atas = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        lblRingkasan.setFont(lblRingkasan.getFont().deriveFont(Font.BOLD, 14f));
        atas.add(lblRingkasan);
        for (StatusFasilitas s : StatusFasilitas.values()) {
            atas.add(legenda(s));
        }

        add(atas, BorderLayout.NORTH);
        JPanel pembungkus = new JPanel(new BorderLayout()); // agar kartu tidak melar mengisi seluruh tinggi
        pembungkus.add(grid, BorderLayout.NORTH);
        add(new JScrollPane(pembungkus), BorderLayout.CENTER);

        refresh();
        new Timer(INTERVAL_MS, e -> {
            if (isShowing()) {  // hanya refresh jika tab ini sedang dilihat
                refresh();
            }
        }).start();
    }

    @Override
    public void refresh() {
        try {
            List<Fasilitas> daftar = fasilitasDao.semua();
            Set<Integer> dipesan = pemesananDao.fasilitasDipesanSekarang();
            grid.removeAll();
            int tersedia = 0;
            for (Fasilitas f : daftar) {
                // Fasilitas yang kosong tapi sedang masuk jam booking orang lain ditampilkan kuning
                StatusFasilitas status = (f.bisaDipesan() && dipesan.contains(f.getId()))
                        ? StatusFasilitas.DIPESAN : f.getStatus();
                grid.add(buatKartu(f, status));
                if (status == StatusFasilitas.TERSEDIA) {
                    tersedia++;
                }
            }
            lblRingkasan.setText("Tersedia " + tersedia + " dari " + daftar.size() + " fasilitas");
            grid.revalidate();
            grid.repaint();
            errorDitampilkan = false;
        } catch (SQLException ex) {
            if (!errorDitampilkan) {  // jangan munculkan popup yang sama setiap 2 detik
                errorDitampilkan = true;
                Pesan.error(this, ex);
            }
        }
    }

    private JPanel buatKartu(Fasilitas f, StatusFasilitas status) {
        Color latar = Tema.warna(status);
        Color teks = Tema.teksDiAtas(latar);
        JPanel kartu = new JPanel();
        kartu.setLayout(new BoxLayout(kartu, BoxLayout.Y_AXIS));
        kartu.setBackground(latar);
        kartu.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        kartu.setPreferredSize(new Dimension(200, 120));

        kartu.add(label(f.getKode() + "  " + f.getNama(), teks, Font.BOLD, 15f));
        kartu.add(label(f.getJenis().getLabel(), teks, Font.PLAIN, 12f));
        kartu.add(label(Format.rupiah(f.getTarifPerJam()) + " / jam", teks, Font.BOLD, 13f));
        kartu.add(label(f.getInfoTambahan(), teks, Font.PLAIN, 11f));
        kartu.add(label(status.name(), teks, Font.BOLD, 12f));
        return kartu;
    }

    private static JLabel label(String teks, Color warna, int gaya, float ukuran) {
        JLabel l = new JLabel(teks);
        l.setForeground(warna);
        l.setFont(l.getFont().deriveFont(gaya, ukuran));
        return l;
    }

    private JLabel legenda(StatusFasilitas s) {
        JLabel l = new JLabel("  " + s.name() + "  ");
        l.setOpaque(true);
        l.setBackground(Tema.warna(s));
        l.setForeground(Tema.teksDiAtas(Tema.warna(s)));
        return l;
    }
}
