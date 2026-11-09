package id.unsika.workspace.model;

/**
 * Meja kerja (table) di area working space.
 * INHERITANCE: MejaDesk adalah turunan Fasilitas.
 * Aturan tarif: dihitung per 30 menit (pembulatan ke atas).
 */
public class MejaDesk extends Fasilitas {

    public static final int MENIT_PER_BLOK = 30;

    private boolean adaStopKontak;

    public MejaDesk(int id, String kode, String nama, double tarifPerJam,
                    StatusFasilitas status, boolean adaStopKontak) {
        super(id, kode, nama, tarifPerJam, status);
        this.adaStopKontak = adaStopKontak;
    }

    /** POLYMORPHISM (overriding): versi hitung biaya khusus meja. */
    @Override
    public double hitungBiaya(long menit) {
        return hitungPerBlok(menit, MENIT_PER_BLOK);
    }

    @Override
    public JenisFasilitas getJenis() {
        return JenisFasilitas.MEJA;
    }

    @Override
    public String getInfoTambahan() {
        return adaStopKontak ? "Ada stop kontak" : "Tanpa stop kontak";
    }

    public boolean isAdaStopKontak() {
        return adaStopKontak;
    }

    public void setAdaStopKontak(boolean adaStopKontak) {
        this.adaStopKontak = adaStopKontak;
    }
}
