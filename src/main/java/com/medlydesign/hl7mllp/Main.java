package com.medlydesign.hl7mllp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] argv) {
        Args args = new Args(argv);

        String host = env("HL7_HOST", args.get("host", "127.0.0.1"));
        int port = Integer.parseInt(env("HL7_PORT", args.get("port", "2575")));
        int readTimeoutMs = Integer.parseInt(env("HL7_READ_TIMEOUT_MS", args.get("timeoutMs", "5000")));
        int maxAttempts = Integer.parseInt(env("HL7_MAX_ATTEMPTS", args.get("maxAttempts", "3")));
        long backoffMs = Long.parseLong(env("HL7_BACKOFF_MS", args.get("backoffMs", "500")));

        String mode = args.get("mode", "good"); // good | bad
        int count = args.getInt("count", 1);

        FailureStore failures = new FailureStore(Path.of("failed"));
        int overallExit = 0;

        for (int i = 1; i <= count; i++) {
            String good = Hl7AdtBuilder.buildAdtA01(
                    "JAVA_SENDER", "HOSP",
                    "MIRTH", "HOSP",
                    "123456", "DOE", "JANE",
                    "19800101", "F",
                    "V0001",
                    "E",
                    "ER^01^01^HOSP"
            );

            String hl7 = "bad".equalsIgnoreCase(mode)
                    ? Hl7AdtBuilder.buildBadAdtMissingPid(good)
                    : good;

            String controlId = Hl7AdtBuilder.getControlIdFromMsh(hl7);

            Exception last = null;
            boolean success = false;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                long start = System.currentTimeMillis();
                String ackRaw = null;

                try {
                    log.info("Send ADT^A01 {}/{} mode={} attempt={} controlId={} to {}:{}",
                            i, count, mode, attempt, controlId, host, port);

                    ackRaw = MllpClient.sendAndReceiveAck(host, port, readTimeoutMs, hl7);
                    Hl7Ack ack = Hl7AckParser.parse(ackRaw);

                    long ms = System.currentTimeMillis() - start;
                    boolean correlated = controlId.equals(ack.msaControlId());

                    log.info("ACK msaCode={} msaControlId={} correlated={} latencyMs={} msaText={}",
                            ack.msaCode(), ack.msaControlId(), correlated, ms,
                            (ack.msaText() == null ? "" : ack.msaText()));

                    if (!correlated) throw new IllegalStateException("ACK controlId mismatch");

                    String code = (ack.msaCode() == null) ? "" : ack.msaCode().toUpperCase();

                    if ("AA".equals(code)) {
                        success = true;
                        break;
                    }

                    // Save the failure for replay/debug
                    failures.save(controlId, hl7, ackRaw);

                    if ("AR".equals(code)) {
                        // Reject: usually a data/format problem. Don't hammer retries.
                        throw new IllegalStateException("Application Reject (AR) - not retrying");
                    }

                    if ("AE".equals(code)) {
                        throw new IllegalStateException("Application Error (AE)");
                    }

                    throw new IllegalStateException("Unexpected ACK code: " + code);

                } catch (Exception e) {
                    last = e;

                    // If AR, stop retry loop
                    if (e.getMessage() != null && e.getMessage().contains("not retrying")) {
                        break;
                    }

                    log.warn("FAILED attempt={} controlId={} err={}", attempt, controlId, e.toString());

                    // if we never got an ACK (timeout/connection), still archive outbound
                    if (ackRaw == null) {
                        failures.save(controlId, hl7, "");
                    }

                    try { Thread.sleep(backoffMs); } catch (InterruptedException ignored) {}
                }
            }

            if (success) {
                log.info("SUCCESS controlId={}", controlId);
            } else {
                overallExit = 2;
                log.error("FAILED controlId={} lastErr={}", controlId, last);
            }
        }

        System.exit(overallExit);
    }

    private static String env(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }
}
