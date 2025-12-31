package com.medlydesign.hl7mllp;

public final class Hl7AckParser {
    public static Hl7Ack parse(String ackMessage) {
        String[] segments = ackMessage.split("\r");
        for (String seg : segments) {
            if (seg.startsWith("MSA|")) {
                String[] f = seg.split("\\|", -1);
                String code = f.length > 1 ? f[1] : "";
                String controlId = f.length > 2 ? f[2] : "";
                return new Hl7Ack(code, controlId, ackMessage);
            }
        }
        return new Hl7Ack("NO_MSA", "", ackMessage);
    }
}
