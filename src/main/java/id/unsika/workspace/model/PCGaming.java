package id.unsika.workspace.model;

/**
 * Unit PC gaming (monitor + PC).
 * Aturan tarif: dihitung per 15 menit (pembulatan ke atas).
 * Tipe VIP biasanya diberi tarif per jam lebih tinggi di data fasilitas.
 */
public class PCGaming extends Fasilitas {

    public static final int MENIT_PER_BLOK = 15;

    private String spesifikasi;
    private TipePC tipe;

    public PCGaming(int id, String kode, String nama, double tarifPerJam,
                    StatusFasilitas status, String spesifikasi, TipePC tipe) {
        super(id, kode, nama, tarifPerJam, status);
        this.spesifikasi = spesifikasi;
        this.tipe = tipe;
    }

    @Override
    public double hitungBiaya(long menit) {
        return hitungPerBlok(menit, MENIT_PER_BLOK);
    }

    @Override
    public JenisFasilitas getJenis() {
        return JenisFasilitas.PC;
    }

    @Override
    public String getInfoTambahan() {
        return tipe + " | " + spesifikasi;
    }

    public String getSpesifikasi() {
        return spesifikasi;
    }

    public void setSpesifikasi(String spesifikasi) {
        this.spesifikasi = spesifikasi;
    }

    public TipePC getTipe() {
        return tipe;
    }

    public void setTipe(TipePC tipe) {
        this.tipe = tipe;
    }
}
