package com.medlydesign.hl7mllp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        String host = env("HL7_HOST", "127.0.0.1");
        int port = Integer.parseInt(env("HL7_PORT", "2575"));
        int readTimeoutMs = Integer.parseInt(env("HL7_READ_TIMEOUT_MS", "5000"));
        int maxAttempts = Integer.parseInt(env("HL7_MAX_ATTEMPTS", "3"));
        long backoffMs = Long.parseLong(env("HL7_BACKOFF_MS", "500"));

        String hl7 = Hl7AdtBuilder.buildAdtA01(
                "JAVA_SENDER", "HOSP",
                "MIRTH", "HOSP",
                "123456", "DOE", "JANE",
                "19800101", "F",
                "V0001",
                "E",
                "ER^01^01^HOSP"
        );

        String controlId = Hl7AdtBuilder.getControlIdFromMsh(hl7);

        Exception last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long start = System.currentTimeMillis();
            try {
                log.info("Sending ADT^A01 attempt={} controlId={} to {}:{}", attempt, controlId, host, port);

                String ackRaw = MllpClient.sendAndReceiveAck(host, port, readTimeoutMs, hl7);
                Hl7Ack ack = Hl7AckParser.parse(ackRaw);

                long ms = System.currentTimeMillis() - start;
                boolean correlated = controlId.equals(ack.msaControlId());

                log.info("ACK msaCode={} msaControlId={} correlated={} latencyMs={}",
                        ack.msaCode(), ack.msaControlId(), correlated, ms);

                if (!correlated) throw new IllegalStateException("ACK controlId mismatch");
                if (!"AA".equalsIgnoreCase(ack.msaCode())) throw new IllegalStateException("Negative ACK: " + ack.msaCode());

                log.info("SUCCESS");
                return;
            } catch (Exception e) {
                last = e;
                log.warn("FAILED attempt={} err={}", attempt, e.toString());
                try { Thread.sleep(backoffMs); } catch (InterruptedException ignored) {}
            }
        }

        log.error("FAILED after {} attempts. Last error: {}", maxAttempts, last);
        System.exit(1);
    }

    private static String env(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }
}
