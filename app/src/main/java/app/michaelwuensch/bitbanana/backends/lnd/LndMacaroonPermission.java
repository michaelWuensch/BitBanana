package app.michaelwuensch.bitbanana.backends.lnd;

import androidx.annotation.NonNull;

public class LndMacaroonPermission {
    String identifier;
    boolean canRead;
    boolean canWrite;

    public LndMacaroonPermission(String identifier) {
        this.identifier = identifier;
        this.canRead = false;
        this.canWrite = false;
    }

    @NonNull
    @Override
    public String toString() {
        return "Permission{" +
                "identifier='" + identifier + '\'' +
                ", canRead=" + canRead +
                ", canWrite=" + canWrite +
                '}';
    }
}
