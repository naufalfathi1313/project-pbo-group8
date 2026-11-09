package id.unsika.workspace.ui;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * Panel untuk dimasukkan ke JScrollPane yang lebarnya selalu mengikuti jendela,
 * jadi hanya bisa digulir ke bawah (tidak muncul scroll ke samping).
 */
class PanelGulir extends JPanel implements Scrollable {

    PanelGulir(Component isi) {
        super(new BorderLayout());
        add(isi, BorderLayout.NORTH);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle r, int orientasi, int arah) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle r, int orientasi, int arah) {
        return r.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
