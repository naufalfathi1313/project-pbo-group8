package id.unsika.workspace.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tagihan yang tampil di dialog pembayaran harus sama persis dengan total yang tersimpan. */
class PemesananTest {

    @Test
    void tagihanYangDibekukanSamaDenganTotalSaatSelesai() {
        PCGaming pc = new PCGaming(3, "PC-01", "PC 01", 12000, StatusFasilitas.TERSEDIA, "RTX 3060", TipePC.REGULER);
        Pemesanan p = new Pemesanan(new Pelanggan(1, "Andi", "081234567890"), pc);
        LocalDateTime mulai = LocalDateTime.of(2026, 11, 20, 13, 0);
        LocalDateTime tagih = mulai.plusMinutes(31); // 31 menit -> 3 blok x 15 menit -> Rp9.000

        p.mulai(mulai);
        double ditampilkan = p.biayaSampai(tagih);
        p.selesaikan(tagih);

        assertEquals(9000, ditampilkan);
        assertEquals(ditampilkan, p.getTotalBiaya());
        assertEquals(StatusFasilitas.TERSEDIA, pc.getStatus());
    }
}
