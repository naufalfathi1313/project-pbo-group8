package id.unsika.workspace.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrukTest {

    @Test
    void nomorDiubahKeFormatWhatsApp() {
        assertEquals("6281234567890", Struk.nomorWa("081234567890"));
        assertEquals("6281234567890", Struk.nomorWa("+6281234567890"));
        assertEquals("6281234567890", Struk.nomorWa("6281234567890"));
    }

    @Test
    void linkWaMembawaTeksStruk() {
        String link = Struk.linkWa("0812345678", "TOTAL : Rp 10.000");
        assertTrue(link.startsWith("https://wa.me/62812345678?text="));
        assertTrue(link.contains("TOTAL"));
    }
}
