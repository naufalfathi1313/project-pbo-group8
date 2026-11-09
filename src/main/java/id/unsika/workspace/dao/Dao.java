package id.unsika.workspace.dao;

import java.sql.SQLException;
import java.util.List;

/**
 * Interface GENERIC untuk akses database. Huruf T diganti dengan tipe datanya,
 * misalnya Dao<Pelanggan> atau Dao<Fasilitas>. Dengan generics, compiler memastikan
 * PelangganDao hanya menerima dan mengembalikan object Pelanggan (type safety).
 */
public interface Dao<T> {

    List<T> semua() throws SQLException;

    T cariById(int id) throws SQLException;

    /** Insert jika id masih 0, update jika id sudah ada. */
    void simpan(T data) throws SQLException;

    void hapus(int id) throws SQLException;
}
