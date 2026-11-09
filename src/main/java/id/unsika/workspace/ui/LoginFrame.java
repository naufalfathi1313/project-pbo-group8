package id.unsika.workspace.ui;

import id.unsika.workspace.dao.UserDao;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Jendela login. INHERITANCE: LoginFrame adalah turunan JFrame milik Swing. */
public class LoginFrame extends JFrame {

    private final JTextField txtUsername = new JTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);
    private final UserDao userDao = new UserDao();

    public LoginFrame() {
        super("Login - Working Space & Rental PC Gaming");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel judul = new JLabel("WORKSPACE & PC GAMING", SwingConstants.CENTER);
        judul.setFont(judul.getFont().deriveFont(Font.BOLD, 18f));
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        panel.add(judul, c);

        c.gridwidth = 1;
        c.gridy = 1; panel.add(new JLabel("Username"), c);
        c.gridx = 1; panel.add(txtUsername, c);
        c.gridx = 0; c.gridy = 2; panel.add(new JLabel("Password"), c);
        c.gridx = 1; panel.add(txtPassword, c);

        JButton btnLogin = new JButton("Login");
        btnLogin.addActionListener(e -> login());
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2;
        panel.add(btnLogin, c);

        getRootPane().setDefaultButton(btnLogin); // tekan Enter = klik Login
        add(panel);
        pack();
        setLocationRelativeTo(null); // posisi di tengah layar
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
