package id.unsika.workspace.model;

/** Data pelanggan yang menyewa fasilitas. */
public class Pelanggan {

    private int id;
    private String nama;
    private String noHp;

    public Pelanggan(int id, String nama, String noHp) {
        this.id = id;
        setNama(nama);
        setNoHp(noHp);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        if (nama == null || nama.isBlank()) {
            throw new IllegalArgumentException("Nama pelanggan wajib diisi");
        }
        this.nama = nama.trim();
    }

    public String getNoHp() {
        return noHp;
    }

    public void setNoHp(String noHp) {
        if (noHp == null || !noHp.trim().matches("\\+?[0-9]{8,15}")) {
            throw new IllegalArgumentException("No HP harus berisi 8-15 digit angka");
        }
        this.noHp = noHp.trim();
    }

    @Override
    public String toString() {
        return nama + " (" + noHp + ")";
    }
}
