package com.medlydesign.hl7mllp;

import java.util.HashMap;
import java.util.Map;

public final class Args {
    private final Map<String, String> map = new HashMap<>();

    public Args(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--")) {
                String key = a.substring(2);
                String val = "true";
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    val = args[++i];
                }
                map.put(key, val);
            }
        }
    }

    public String get(String key, String def) {
        return map.getOrDefault(key, def);
    }

    public int getInt(String key, int def) {
        try { return Integer.parseInt(get(key, String.valueOf(def))); }
        catch (Exception e) { return def; }
    }
}

