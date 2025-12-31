package com.medlydesign.hl7mllp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class Hl7AdtBuilder {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String buildAdtA01(
            String sendingApp, String sendingFacility,
            String receivingApp, String receivingFacility,
            String mrn, String lastName, String firstName,
            String dobYYYYMMDD, String sex,
            String visitNumber,
            String patientClass,
            String location
    ) {
        String timestamp = LocalDateTime.now().format(TS);
        String controlId = UUID.randomUUID().toString();

        String msh = String.join("|",
                "MSH", "^~\\&",
                sendingApp, sendingFacility,
                receivingApp, receivingFacility,
                timestamp,
                "",
                "ADT^A01",
                controlId,
                "P",
                "2.3"
        );

        String pid = String.join("|",
                "PID", "1",
                "",
                mrn + "^^^" + sendingFacility + "^MR",
                "",
                lastName + "^" + firstName,
                "",
                dobYYYYMMDD,
                sex
        );

        String pv1 = String.join("|",
                "PV1", "1",
                patientClass,
                location,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                visitNumber
        );

        return msh + "\r" + pid + "\r" + pv1 + "\r";
    }

    public static String getControlIdFromMsh(String hl7) {
        String msh = hl7.split("\r")[0];
        String[] f = msh.split("\\|", -1);
        return (f.length > 9) ? f[9] : "";
    }

    public static String buildBadAdtMissingPid(String goodAdt) {
        StringBuilder sb = new StringBuilder();
        for (String seg : goodAdt.split("\r")) {
            if (seg.startsWith("PID|")) continue; // remove PID to trigger reject/error
            if (!seg.isBlank()) sb.append(seg).append("\r");
        }
        return sb.toString();
    }
}
