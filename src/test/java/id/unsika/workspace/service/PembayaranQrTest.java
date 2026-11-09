package id.unsika.workspace.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Membuktikan aplikasi membaca jawaban dari HP pelanggan dengan benar, tanpa perlu internet. */
class PembayaranQrTest {

    private static String pesan(String isi) {
        return "{\"id\":\"x1\",\"time\":1700000000,\"event\":\"message\",\"topic\":\"wsrent-abc\",\"message\":\"" + isi + "\"}\n";
    }

    @Test
    void belumAdaJawabanBerartiMenunggu() {
        assertEquals(PembayaranQr.Status.MENUNGGU, PembayaranQr.bacaStatus(""));
    }

    @Test
    void tombolYaBerartiLunas() {
        assertEquals(PembayaranQr.Status.LUNAS, PembayaranQr.bacaStatus(pesan("LUNAS")));
    }

    @Test
    void tombolTidakBerartiDitolak() {
        assertEquals(PembayaranQr.Status.DITOLAK, PembayaranQr.bacaStatus(pesan("BATAL")));
    }

    @Test
    void sempatTidakLaluYaTetapLunas() {
        assertEquals(PembayaranQr.Status.LUNAS, PembayaranQr.bacaStatus(pesan("BATAL") + pesan("LUNAS")));
    }

    @Test
    void linkMemuatKodeAcakDanRincianTagihan() {
        PembayaranQr qr = new PembayaranQr();
        String link = qr.link("PC-01 - PC 01 (PC Gaming)", "Andi", "01:30:00", 24000);
        assertTrue(qr.getKode().matches("wsrent-[0-9a-f]{32}"));
        assertTrue(link.contains("bayar.html?id=" + qr.getKode() + "&"));
        assertTrue(link.contains("&f=PC-01+-+PC+01+%28PC+Gaming%29&"));
        assertTrue(link.contains("&n=Andi&d=01%3A30%3A00&"));
        assertTrue(link.endsWith("&t=24000"));
    }
}
