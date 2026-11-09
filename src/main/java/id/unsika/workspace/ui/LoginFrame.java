package id.unsika.workspace.ui;

import id.unsika.workspace.dao.UserDao;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;

/** Jendela login. INHERITANCE: LoginFrame adalah turunan JFrame milik Swing. */
public class LoginFrame extends JFrame {

    private final JTextField txtUsername = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final UserDao userDao = new UserDao();

    public LoginFrame() {
        super("Login - Working Space & Rental PC Gaming");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        txtUsername.putClientProperty("JTextField.placeholderText", "admin");
        txtPassword.putClientProperty("JTextField.placeholderText", "••••••••");
        txtPassword.putClientProperty("JPasswordField.showRevealButton", true);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(rata(Tema.label("WORKSPACE", 24f, Font.BOLD, Tema.TEKS)));
        form.add(rata(Tema.label("& PC GAMING", 24f, Font.BOLD, Tema.AKSEN)));
        form.add(Box.createVerticalStrut(6));
        form.add(rata(Tema.label("Masuk untuk mulai melayani pelanggan", 13f, Font.PLAIN, Tema.TEKS_REDUP)));
        form.add(Box.createVerticalStrut(26));
        form.add(rata(Tema.label("Username", 12f, Font.BOLD, Tema.TEKS_REDUP)));
        form.add(Box.createVerticalStrut(6));
        form.add(rata(lebar(txtUsername)));
        form.add(Box.createVerticalStrut(14));
        form.add(rata(Tema.label("Password", 12f, Font.BOLD, Tema.TEKS_REDUP)));
        form.add(Box.createVerticalStrut(6));
        form.add(rata(lebar(txtPassword)));
        form.add(Box.createVerticalStrut(22));

        JButton btnLogin = Tema.tombolUtama("Masuk");
        btnLogin.addActionListener(e -> login());
        form.add(rata(lebar(btnLogin)));

        Kartu kartu = new Kartu(new BorderLayout());
        kartu.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));
        kartu.add(form);

        JPanel latar = new JPanel(new GridBagLayout()); // GridBagLayout tanpa constraint = isi di tengah
        latar.setPreferredSize(new Dimension(520, 520));
        latar.add(kartu);
        add(latar);

        getRootPane().setDefaultButton(btnLogin); // tekan Enter = klik Masuk
        pack();
        setLocationRelativeTo(null); // posisi di tengah layar
    }

    private static JComponent rata(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static JComponent lebar(JComponent c) {
        Dimension d = new Dimension(320, 40);
        c.setPreferredSize(d);
        c.setMaximumSize(d);
        return c;
    }

    private void login() {
        try {
            String nama = userDao.login(txtUsername.getText().trim(), new String(txtPassword.getPassword()));
            if (nama == null) {
                Pesan.info(this, "Username atau password salah.");
                txtPassword.setText("");
                return;
            }
            new MainFrame(nama).setVisible(true);
            dispose(); // tutup jendela login
        } catch (Exception ex) {
            Pesan.error(this, ex);
        }
    }
}
