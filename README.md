# Working Space & Rental PC Gaming

Aplikasi desktop **Java Swing + MySQL** untuk mengelola penyewaan meja kerja, ruang meeting, dan PC gaming.
Tugas UAS **Pemrograman Berorientasi Objek (SYS61516)**, Fakultas Ilmu Komputer, Universitas Singaperbangsa Karawang.

**Kelompok 8:** Salwa & Fathir

![Dashboard](docs/screenshots/1-Dashboard.png)

## Fitur

| Fitur | Keterangan |
|---|---|
| Login | Akun default `admin` / `admin123` |
| Dashboard real-time | Kartu tiap fasilitas berwarna sesuai status, diperbarui otomatis setiap 2 detik |
| Jadwal & Booking | Grid jadwal per jam (Tersedia / Dipilih / Dibooking / Dipakai / Tutup-Lewat). Pilih beberapa jam berurutan, lihat estimasi biaya, lalu konfirmasi |
| Walk-in | Pelanggan tanpa booking langsung main, timer langsung jalan |
| Timer durasi | Tab Sesi Aktif: check-in booking, durasi dan biaya berjalan setiap detik, struk saat selesai |
| Rekap | Transaksi per periode, filter jenis, total pendapatan |
| Kelola data | Tambah / ubah / hapus fasilitas dan pelanggan |
| Tema gelap | Tampilan gelap ala gaming memakai library FlatLaf |

![Jadwal](docs/screenshots/2-Jadwal.png)

## Konsep OOP

Class abstrak **`Fasilitas`** diturunkan menjadi **`MejaDesk`**, **`MeetingRoom`**, dan **`PCGaming`**. Ketiganya punya tarif per jam dan aturan hitung biaya yang berbeda:

| Class | Tarif contoh | Aturan `hitungBiaya()` |
|---|---|---|
| `MejaDesk` | Rp10.000/jam | per 30 menit, dibulatkan ke atas |
| `MeetingRoom` | Rp50.000–80.000/jam | per jam penuh, minimal 1 jam |
| `PCGaming` | Rp12.000 (Reguler) / Rp20.000 (VIP) | per 15 menit, dibulatkan ke atas |

Biaya akhir selalu dihitung dari **timer** (check-in sampai selesai). Jadwal booking hanya untuk memesan tempat dan menampilkan estimasi.

Penjelasan lengkap ada di [docs/DESAIN.md](docs/DESAIN.md).

## Cara Menjalankan

### 1. Siapkan JDK 17 dan VS Code
- Install **JDK 17** atau lebih baru.
  - Windows: [Adoptium Temurin 17](https://adoptium.net)
  - Mac: `brew install openjdk@17`
- Di VS Code, install extension **Extension Pack for Java** (sudah termasuk dukungan Maven).

### 2. Siapkan MySQL

**Windows: XAMPP**
1. Install [XAMPP](https://www.apachefriends.org), buka XAMPP Control Panel, klik **Start** pada **MySQL** (dan Apache untuk phpMyAdmin).
2. Buka <http://localhost/phpmyadmin>, pilih tab **Import**, pilih file `database/workspace_db.sql`, klik **Go**.

**Mac: Homebrew** (XAMPP tidak tersedia untuk Mac)
```bash
brew install mysql
brew services start mysql
mysql -u root < database/workspace_db.sql
```
Untuk melihat isi tabel tanpa phpMyAdmin, pakai extension VS Code **"MySQL" (Weijan Chen)** atau aplikasi **DBeaver**.

> Kedua cara memakai setting yang sama: `localhost:3306`, user `root`, password kosong.
> Kalau setting kalian berbeda, ubah konstanta di `src/main/java/id/unsika/workspace/db/Koneksi.java`.

### 3. Jalankan aplikasi
- Buka folder project ini di VS Code, tunggu Maven selesai mengunduh dependency.
- Buka `src/main/java/id/unsika/workspace/App.java`, klik **Run**.
- Atau lewat terminal: `mvn compile exec:java`

### 4. Jalankan unit test
```bash
mvn test
```
Test di `src/test` membuktikan perhitungan biaya tiap jenis fasilitas dan siklus pemesanan.

## Struktur Folder

```
src/main/java/id/unsika/workspace/
├── App.java            titik masuk program
├── model/              class OOP: Fasilitas, MejaDesk, MeetingRoom, PCGaming, Pelanggan, Pemesanan
├── db/Koneksi.java     koneksi MySQL (Singleton)
├── dao/                akses database (interface generik Dao<T>)
├── service/            logika pemesanan: pesan, check-in, selesai, batal
├── ui/                 tampilan Java Swing (Login, Dashboard, Jadwal, Walk-in, Sesi Aktif, Rekap, Kelola Data, Tema)
└── util/Format.java    format Rupiah, durasi, tanggal
database/workspace_db.sql   struktur tabel + data contoh
docs/                       desain dan screenshot
```
