package id.unsika.workspace.model;

/**
 * Class induk untuk semua fasilitas (MejaDesk, MeetingRoom, PCGaming).
 *
 * - ABSTRACTION  : class ini abstract, jadi tidak bisa dibuat langsung dengan `new Fasilitas()`.
 *                  Method hitungBiaya(), getJenis(), getInfoTambahan() hanya dideklarasikan di sini,
 *                  isinya ditulis oleh masing-masing subclass.
 * - ENCAPSULATION: semua atribut private. Perubahan nilai lewat setter yang memvalidasi input.
 * - INHERITANCE  : subclass mewarisi atribut dan method umum di bawah ini.
 */
public abstract class Fasilitas implements Bookable {

    private int id;
    private String kode;
    private String nama;
    private double tarifPerJam;
    private StatusFasilitas status;

    protected Fasilitas(int id, String kode, String nama, double tarifPerJam, StatusFasilitas status) {
        this.id = id;
        setKode(kode);
        setNama(nama);
        setTarifPerJam(tarifPerJam);
        this.status = status;
    }

    // ------------------------------------------------------------------
    // Method abstrak: tiap subclass punya cara sendiri (POLYMORPHISM)
    // ------------------------------------------------------------------

    /** Menghitung biaya sewa untuk durasi pemakaian dalam menit. */
    public abstract double hitungBiaya(long menit);

    public abstract JenisFasilitas getJenis();

    /** Keterangan khusus tiap jenis, misalnya kapasitas ruangan atau spesifikasi PC. */
    public abstract String getInfoTambahan();

    // ------------------------------------------------------------------
    // Helper yang dipakai subclass
    // ------------------------------------------------------------------

    /**
     * Membulatkan durasi KE ATAS ke kelipatan `menitPerBlok`, lalu dikali tarif.
     * Contoh: blok 30 menit, durasi 31 menit -> dihitung 60 menit.
     */
    protected double hitungPerBlok(long menit, int menitPerBlok) {
        if (menit <= 0) {
            return 0;
        }
        long jumlahBlok = (menit + menitPerBlok - 1) / menitPerBlok; // pembagian bulat ke atas
        long menitDitagih = jumlahBlok * menitPerBlok;
        return menitDitagih * tarifPerJam / 60.0;
    }

    // ------------------------------------------------------------------
    // Implementasi interface Bookable
    // ------------------------------------------------------------------

    @Override
    public boolean bisaDipesan() {
        return status == StatusFasilitas.TERSEDIA;
    }

    @Override
    public void tandaiDipakai() {
        status = StatusFasilitas.DIPAKAI;
    }

    @Override
    public void tandaiTersedia() {
        status = StatusFasilitas.TERSEDIA;
    }

    // ------------------------------------------------------------------
    // Getter & setter (ENCAPSULATION)
    // ------------------------------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKode() {
        return kode;
    }

    public void setKode(String kode) {
        if (kode == null || kode.isBlank()) {
            throw new IllegalArgumentException("Kode fasilitas wajib diisi");
        }
        this.kode = kode.trim();
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        if (nama == null || nama.isBlank()) {
            throw new IllegalArgumentException("Nama fasilitas wajib diisi");
        }
        this.nama = nama.trim();
    }

    public double getTarifPerJam() {
        return tarifPerJam;
    }

    public void setTarifPerJam(double tarifPerJam) {
        if (tarifPerJam <= 0) {
            throw new IllegalArgumentException("Tarif per jam harus lebih dari 0");
        }
        this.tarifPerJam = tarifPerJam;
    }

    public StatusFasilitas getStatus() {
        return status;
    }

    public void setStatus(StatusFasilitas status) {
        this.status = status;
    }

    /** Teks yang tampil di ComboBox, misalnya "PC-01 - PC Gaming 01 (PC Gaming)". */
    @Override
    public String toString() {
        return kode + " - " + nama + " (" + getJenis() + ")";
    }
}
