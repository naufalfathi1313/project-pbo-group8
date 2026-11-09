package id.unsika.workspace.model;

/** Status sebuah fasilitas, ditampilkan sebagai warna di Dashboard. */
public enum StatusFasilitas {
    TERSEDIA,     // hijau  : bisa dipesan
    DIPESAN,      // kuning : ada booking yang jadwalnya sedang berlangsung (dihitung dari jadwal, tidak disimpan di DB)
    DIPAKAI,      // merah  : timer sedang berjalan
    MAINTENANCE   // abu-abu: sedang diperbaiki, tidak bisa dipesan
}
