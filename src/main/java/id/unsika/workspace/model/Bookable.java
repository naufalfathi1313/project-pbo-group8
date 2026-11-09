package id.unsika.workspace.model;

/**
 * ABSTRACTION lewat interface: kontrak untuk semua benda yang bisa dipesan.
 * Class yang mengimplementasikan interface ini WAJIB menyediakan method-method di bawah.
 */
public interface Bookable {

    /** true jika saat ini boleh dipesan / dipakai. */
    boolean bisaDipesan();

    void tandaiDipakai();

    void tandaiTersedia();
}
