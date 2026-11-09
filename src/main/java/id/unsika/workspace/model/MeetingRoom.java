package id.unsika.workspace.model;

/**
 * Ruang meeting.
 * Aturan tarif: dihitung per JAM PENUH dan minimal bayar 1 jam.
 * Contoh: pakai 10 menit -> bayar 1 jam, pakai 61 menit -> bayar 2 jam.
 */
public class MeetingRoom extends Fasilitas {

    public static final int MENIT_PER_BLOK = 60;

    private int kapasitas;

    public MeetingRoom(int id, String kode, String nama, double tarifPerJam,
                       StatusFasilitas status, int kapasitas) {
        super(id, kode, nama, tarifPerJam, status);
        setKapasitas(kapasitas);
    }

    @Override
    public double hitungBiaya(long menit) {
        long menitDitagih = Math.max(menit, MENIT_PER_BLOK); // minimal 1 jam
        return hitungPerBlok(menitDitagih, MENIT_PER_BLOK);
    }

    @Override
    public JenisFasilitas getJenis() {
        return JenisFasilitas.MEETING;
    }

    @Override
    public String getInfoTambahan() {
        return "Kapasitas " + kapasitas + " orang";
    }

    public int getKapasitas() {
        return kapasitas;
    }

    public void setKapasitas(int kapasitas) {
        if (kapasitas <= 0) {
            throw new IllegalArgumentException("Kapasitas ruangan harus lebih dari 0");
        }
        this.kapasitas = kapasitas;
    }
}
