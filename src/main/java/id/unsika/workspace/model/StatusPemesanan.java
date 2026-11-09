package id.unsika.workspace.model;

/** Siklus hidup sebuah pemesanan: DIPESAN -> AKTIF -> SELESAI (atau DIBATALKAN). */
public enum StatusPemesanan {
    DIPESAN,
    AKTIF,
    SELESAI,
    DIBATALKAN
}
