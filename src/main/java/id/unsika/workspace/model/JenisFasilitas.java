package id.unsika.workspace.model;

/** Tiga jenis fasilitas yang bisa disewa. Nilainya sama dengan kolom `jenis` di database. */
public enum JenisFasilitas {
    MEJA("Meja Desk"),
    MEETING("Meeting Room"),
    PC("PC Gaming");

    private final String label;

    JenisFasilitas(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
