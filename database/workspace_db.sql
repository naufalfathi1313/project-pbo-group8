-- =====================================================================
--  Database: Working Space & Rental PC Gaming
--  Cara pakai: buka phpMyAdmin (XAMPP) -> tab Import -> pilih file ini.
--  File ini akan MENGHAPUS dan membuat ulang database workspace_db.
-- =====================================================================

DROP DATABASE IF EXISTS workspace_db;
CREATE DATABASE workspace_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE workspace_db;

-- Akun untuk login. Password disimpan dalam bentuk hash SHA-256, bukan teks asli.
CREATE TABLE users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash CHAR(64)     NOT NULL,
    nama          VARCHAR(100) NOT NULL
);

-- Satu tabel untuk semua jenis fasilitas (single table inheritance).
-- Kolom `jenis` menentukan object Java mana yang dibuat:
--   MEJA -> MejaDesk, MEETING -> MeetingRoom, PC -> PCGaming
-- Kolom khusus yang tidak dipakai oleh suatu jenis dibiarkan NULL.
CREATE TABLE fasilitas (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    kode            VARCHAR(10)   NOT NULL UNIQUE,
    nama            VARCHAR(100)  NOT NULL,
    jenis           ENUM('MEJA','MEETING','PC') NOT NULL,
    tarif_per_jam   DECIMAL(10,2) NOT NULL,
    -- DIPESAN tidak disimpan: aplikasi menghitungnya dari jadwal booking yang sedang berlangsung
    status          ENUM('TERSEDIA','DIPAKAI','MAINTENANCE') NOT NULL DEFAULT 'TERSEDIA',
    ada_stop_kontak BOOLEAN       NULL,   -- khusus MEJA
    kapasitas       INT           NULL,   -- khusus MEETING
    spesifikasi     VARCHAR(200)  NULL,   -- khusus PC
    tipe_pc         ENUM('REGULER','VIP') NULL  -- khusus PC
);

CREATE TABLE pelanggan (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nama        VARCHAR(100) NOT NULL,
    no_hp       VARCHAR(20)  NOT NULL,
    dibuat_pada DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pemesanan (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    pelanggan_id  INT      NOT NULL,
    fasilitas_id  INT      NOT NULL,
    waktu_pesan   DATETIME NOT NULL,
    jadwal_mulai  DATETIME NULL,          -- terisi jika booking lewat grid jadwal (NULL = walk-in)
    jadwal_selesai DATETIME NULL,
    waktu_mulai   DATETIME NULL,          -- terisi saat timer dimulai
    waktu_selesai DATETIME NULL,          -- terisi saat sesi selesai
    total_biaya   DECIMAL(12,2) NULL,
    status        ENUM('DIPESAN','AKTIF','SELESAI','DIBATALKAN') NOT NULL,
    FOREIGN KEY (pelanggan_id) REFERENCES pelanggan(id),
    FOREIGN KEY (fasilitas_id) REFERENCES fasilitas(id)
);

-- ------------------------- Data contoh -------------------------------

-- Login: admin / admin123
INSERT INTO users (username, password_hash, nama)
VALUES ('admin', SHA2('admin123', 256), 'Administrator');

INSERT INTO fasilitas (kode, nama, jenis, tarif_per_jam, ada_stop_kontak) VALUES
('M-01', 'Meja 01', 'MEJA', 10000, TRUE),
('M-02', 'Meja 02', 'MEJA', 10000, TRUE),
('M-03', 'Meja 03', 'MEJA', 10000, FALSE),
('M-04', 'Meja 04', 'MEJA', 10000, FALSE);

INSERT INTO fasilitas (kode, nama, jenis, tarif_per_jam, kapasitas) VALUES
('R-01', 'Ruang Melati',  'MEETING', 50000, 6),
('R-02', 'Ruang Anggrek', 'MEETING', 80000, 12);

INSERT INTO fasilitas (kode, nama, jenis, tarif_per_jam, spesifikasi, tipe_pc) VALUES
('PC-01', 'PC Gaming 01', 'PC', 12000, 'RTX 3060, Monitor 24" 144Hz', 'REGULER'),
('PC-02', 'PC Gaming 02', 'PC', 12000, 'RTX 3060, Monitor 24" 144Hz', 'REGULER'),
('PC-03', 'PC Gaming 03', 'PC', 12000, 'RTX 3060, Monitor 24" 144Hz', 'REGULER'),
('PC-04', 'PC Gaming VIP 1', 'PC', 20000, 'RTX 4070, Monitor 27" 240Hz', 'VIP'),
('PC-05', 'PC Gaming VIP 2', 'PC', 20000, 'RTX 4070, Monitor 27" 240Hz', 'VIP');

INSERT INTO pelanggan (nama, no_hp) VALUES
('Andi Pratama', '081234567890'),
('Siti Rahma',   '082198765432'),
('Budi Santoso', '085711223344');

-- Beberapa transaksi lama supaya halaman Rekap langsung ada isinya
INSERT INTO pemesanan (pelanggan_id, fasilitas_id, waktu_pesan, waktu_mulai, waktu_selesai, total_biaya, status) VALUES
(1, 7, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY + INTERVAL 2 HOUR, 24000, 'SELESAI'),
(2, 1, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY + INTERVAL 3 HOUR, 30000, 'SELESAI'),
(3, 5, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 1 HOUR, 50000, 'SELESAI');

-- Booking jadwal contoh: hari ini (1-3 jam lagi) dan besok
INSERT INTO pemesanan (pelanggan_id, fasilitas_id, waktu_pesan, jadwal_mulai, jadwal_selesai, status) VALUES
(1, 6,  NOW(), DATE_FORMAT(NOW() + INTERVAL 1 HOUR, '%Y-%m-%d %H:00:00'), DATE_FORMAT(NOW() + INTERVAL 3 HOUR, '%Y-%m-%d %H:00:00'), 'DIPESAN'),
(2, 10, NOW(), CURDATE() + INTERVAL 1 DAY + INTERVAL 13 HOUR, CURDATE() + INTERVAL 1 DAY + INTERVAL 16 HOUR, 'DIPESAN'),
(3, 5,  NOW(), CURDATE() + INTERVAL 1 DAY + INTERVAL 9 HOUR,  CURDATE() + INTERVAL 1 DAY + INTERVAL 11 HOUR, 'DIPESAN');
