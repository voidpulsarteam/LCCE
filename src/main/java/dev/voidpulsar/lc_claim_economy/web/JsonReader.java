package dev.voidpulsar.lc_claim_economy.web;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal, dependency-free parser for the flat {@code {"key": "value", ...}}
 * request bodies the dashboard API accepts - string/boolean/number values
 * only, no nesting or arrays, since that's all any POST endpoint here needs.
 * Mirrors {@link JsonWriter}'s "no Gson dependency" rationale.
 */
final class JsonReader {
    private final Map<String, String> values = new HashMap<>();

    private JsonReader() {
    }

    static JsonReader parse(String body) {
        JsonReader reader = new JsonReader();
        if (body == null) {
            return reader;
        }
        int i = skipWhitespace(body, 0);
        if (i >= body.length() || body.charAt(i) != '{') {
            return reader;
        }
        i++;
        while (i < body.length()) {
            i = skipWhitespace(body, i);
            if (i >= body.length() || body.charAt(i) == '}') {
                break;
            }
            if (body.charAt(i) != '"') {
                break;
            }
            int[] keyEnd = new int[1];
            String key = parseString(body, i, keyEnd);
            i = skipWhitespace(body, keyEnd[0]);
            if (i >= body.length() || body.charAt(i) != ':') {
                break;
            }
            i = skipWhitespace(body, i + 1);
            if (i >= body.length()) {
                break;
            }
            String value;
            int[] valueEnd = new int[1];
            char c = body.charAt(i);
            if (c == '"') {
                value = parseString(body, i, valueEnd);
                i = valueEnd[0];
            } else {
                int start = i;
                while (i < body.length() && body.charAt(i) != ',' && body.charAt(i) != '}') {
                    i++;
                }
                value = body.substring(start, i).trim();
            }
            reader.values.put(key, value);
            i = skipWhitespace(body, i);
            if (i < body.length() && body.charAt(i) == ',') {
                i++;
            }
        }
        return reader;
    }

    private static String parseString(String body, int quoteStart, int[] endOut) {
        StringBuilder sb = new StringBuilder();
        int i = quoteStart + 1;
        while (i < body.length() && body.charAt(i) != '"') {
            char c = body.charAt(i);
            if (c == '\\' && i + 1 < body.length()) {
                char next = body.charAt(i + 1);
                switch (next) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(next);
                }
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        endOut[0] = i + 1;
        return sb.toString();
    }

    private static int skipWhitespace(String body, int i) {
        while (i < body.length() && Character.isWhitespace(body.charAt(i))) {
            i++;
        }
        return i;
    }

    String getString(String key) {
        return values.get(key);
    }

    boolean getBoolean(String key, boolean fallback) {
        String raw = values.get(key);
        if (raw == null) {
            return fallback;
        }
        return Boolean.parseBoolean(raw);
    }
}
