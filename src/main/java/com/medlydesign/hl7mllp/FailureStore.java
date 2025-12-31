package com.medlydesign.hl7mllp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class FailureStore {
    private final Path dir;

    public FailureStore(Path dir) {
        this.dir = dir;
    }

    public void save(String controlId, String outboundHl7, String ackRaw) {
        try {
            Files.createDirectories(dir);

            Files.writeString(dir.resolve(controlId + "_out.hl7"),
                    outboundHl7,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            if (ackRaw != null && !ackRaw.isBlank()) {
                Files.writeString(dir.resolve(controlId + "_ack.hl7"),
                        ackRaw,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException ignored) {
            // never crash the sender because archival failed
        }
    }
}

