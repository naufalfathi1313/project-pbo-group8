package id.unsika.workspace.ui;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Map;

/**
 * Menggambar QR code dari sebuah teks (di sini: link halaman bayar).
 * Library ZXing menghitung pola kotak hitam-putihnya, lalu kita menggambarnya sendiri dengan Java2D,
 * sama seperti grafik di halaman Rekap.
 */
public class PanelQr extends JComponent {

    /** Tepi putih selebar 4 kotak di sekeliling QR. Wajib ada supaya kamera HP mudah membacanya. */
    private static final int TEPI = 4;

    private final BitMatrix kotak;

    public PanelQr(String teks) {
        try {
            kotak = new QRCodeWriter().encode(teks, BarcodeFormat.QR_CODE, 0, 0, Map.of(
                    EncodeHintType.MARGIN, 0,
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.CHARACTER_SET, "UTF-8"));
        } catch (WriterException e) {
            throw new IllegalStateException("QR code gagal dibuat: " + e.getMessage(), e);
        }
        setPreferredSize(new Dimension(260, 260));
    }

    @Override
    protected void paintComponent(Graphics g) {
        int jumlah = kotak.getWidth() + TEPI * 2;                     // jumlah kotak per sisi, termasuk tepi
        int skala = Math.max(1, Math.min(getWidth(), getHeight()) / jumlah); // piksel per kotak, bilangan bulat supaya tajam
        int sisi = skala * jumlah;
        int x0 = (getWidth() - sisi) / 2;
        int y0 = (getHeight() - sisi) / 2;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Color.WHITE); // QR harus gelap di atas terang, walaupun aplikasinya bertema gelap
        g2.fillRect(x0, y0, sisi, sisi);
        g2.setColor(Color.BLACK);
        for (int y = 0; y < kotak.getHeight(); y++) {
            for (int x = 0; x < kotak.getWidth(); x++) {
                if (kotak.get(x, y)) {
                    g2.fillRect(x0 + (x + TEPI) * skala, y0 + (y + TEPI) * skala, skala, skala);
                }
            }
        }
        g2.dispose();
    }
}
