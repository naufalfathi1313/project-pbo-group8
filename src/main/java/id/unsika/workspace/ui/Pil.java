package id.unsika.workspace.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Label kecil berbentuk pil, misalnya penanda status "Tersedia". */
public class Pil extends JLabel {

    public Pil(String teks, Color latar) {
        super(teks);
        setFont(getFont().deriveFont(Font.BOLD, 11f));
        setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        set(teks, latar);
    }

    public void set(String teks, Color latar) {
        setText(teks);
        setBackground(latar);
        setForeground(Tema.teksDiAtas(latar));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
