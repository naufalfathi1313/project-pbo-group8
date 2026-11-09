package id.unsika.workspace.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Membuktikan aturan tarif tiap jenis fasilitas berjalan sesuai rancangan. */
class FasilitasTest {

    private final MejaDesk meja = new MejaDesk(1, "M-01", "Meja 01", 10000, StatusFasilitas.TERSEDIA, true);
    private final MeetingRoom ruang = new MeetingRoom(2, "R-01", "Ruang Melati", 50000, StatusFasilitas.TERSEDIA, 6);
    private final PCGaming pc = new PCGaming(3, "PC-01", "PC 01", 12000, StatusFasilitas.TERSEDIA, "RTX 3060", TipePC.REGULER);

    @Test
    void mejaDihitungPer30Menit() {
        assertEquals(5000, meja.hitungBiaya(1));    // 1 menit -> 30 menit
        assertEquals(5000, meja.hitungBiaya(30));
        assertEquals(10000, meja.hitungBiaya(31));  // 31 menit -> 60 menit
        assertEquals(25000, meja.hitungBiaya(150)); // 2,5 jam
    }

    @Test
    void meetingRoomMinimalSatuJamDanPerJamPenuh() {
        assertEquals(50000, ruang.hitungBiaya(10));
        assertEquals(50000, ruang.hitungBiaya(60));
        assertEquals(100000, ruang.hitungBiaya(61));
    }

    @Test
    void pcDihitungPer15Menit() {
        assertEquals(3000, pc.hitungBiaya(10));
        assertEquals(6000, pc.hitungBiaya(16));
        assertEquals(24000, pc.hitungBiaya(120));
    }

    @Test
    void durasiNolTidakDitagihKecualiMeetingRoom() {
        assertEquals(0, meja.hitungBiaya(0));
        assertEquals(0, pc.hitungBiaya(0));
        assertEquals(50000, ruang.hitungBiaya(0));
    }

    @Test
    void polymorphismSatuMethodBedaHasil() {
        List<Fasilitas> semua = List.of(meja, ruang, pc);
        double[] harapan = {10000, 50000, 12000};   // biaya pemakaian tepat 1 jam
        for (int i = 0; i < semua.size(); i++) {
            assertEquals(harapan[i], semua.get(i).hitungBiaya(60));
        }
    }

    @Test
    void setterMenolakDataTidakValid() {
        assertThrows(IllegalArgumentException.class, () -> meja.setTarifPerJam(-1));
        assertThrows(IllegalArgumentException.class, () -> meja.setNama(" "));
        assertThrows(IllegalArgumentException.class, () -> ruang.setKapasitas(0));
        assertThrows(IllegalArgumentException.class, () -> new Pelanggan(0, "Andi", "abc"));
    }

    @Test
    void siklusPemesananMengubahStatusDanMenghitungTotal() {
        Pelanggan andi = new Pelanggan(1, "Andi", "081234567890");
        Pemesanan p = new Pemesanan(andi, pc);

        LocalDateTime mulai = LocalDateTime.of(2026, 11, 20, 10, 0);
        p.mulai(mulai);
        assertEquals(StatusFasilitas.DIPAKAI, pc.getStatus());

        p.selesaikan(mulai.plusMinutes(95)); // 95 menit -> 7 blok x 15 menit = 105 menit
        assertEquals(StatusPemesanan.SELESAI, p.getStatus());
        assertEquals(21000, p.getTotalBiaya());
        assertTrue(pc.bisaDipesan());
        assertThrows(IllegalStateException.class, p::batalkan);
    }

    @Test
    void bookingJadwalMenghitungEstimasiDanMendeteksiBentrok() {
        Pelanggan siti = new Pelanggan(2, "Siti", "082198765432");
        LocalDateTime jam13 = LocalDateTime.of(2026, 11, 20, 13, 0);
        Pemesanan booking = new Pemesanan(siti, ruang, jam13, jam13.plusHours(3));

        assertEquals(150000, booking.getEstimasiBiaya());                          // 3 jam x Rp50.000
        assertTrue(booking.bentrokDengan(jam13.plusHours(2), jam13.plusHours(4)));  // 15:00-17:00 beririsan
        assertTrue(!booking.bentrokDengan(jam13.plusHours(3), jam13.plusHours(4))); // 16:00-17:00 aman
        assertThrows(IllegalArgumentException.class, () -> new Pemesanan(siti, ruang, jam13, jam13));

        booking.batalkan();
        assertEquals(StatusPemesanan.DIBATALKAN, booking.getStatus());
    }
}
