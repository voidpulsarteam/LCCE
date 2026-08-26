package dev.voidpulsar.lc_claim_economy.web;

/**
 * Minimal JSON string builder for the handful of flat shapes the web
 * server's API needs (objects of primitives/strings, arrays of objects).
 * Deliberately dependency-free rather than relying on Gson being present
 * on the runtime classpath, since that isn't a declared dependency of this
 * mod.
 */
final class JsonWriter {
    private final StringBuilder sb = new StringBuilder();
    private boolean needsComma = false;

    static JsonWriter object() {
        JsonWriter writer = new JsonWriter();
        writer.sb.append('{');
        return writer;
    }

    JsonWriter field(String name, String value) {
        comma();
        key(name);
        sb.append(quote(value));
        return this;
    }

    JsonWriter field(String name, long value) {
        comma();
        key(name);
        sb.append(value);
        return this;
    }

    JsonWriter field(String name, boolean value) {
        comma();
        key(name);
        sb.append(value);
        return this;
    }

    JsonWriter field(String name, JsonWriter nested) {
        comma();
        key(name);
        sb.append(nested.sb).append(nested.closer());
        return this;
    }

    JsonWriter arrayField(String name, java.util.List<JsonWriter> items) {
        comma();
        key(name);
        sb.append('[');
        boolean first = true;
        for (JsonWriter item : items) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(item.sb).append(item.closer());
        }
        sb.append(']');
        return this;
    }

    String build() {
        return sb.append('}').toString();
    }

    private String closer() {
        return "}";
    }

    private void comma() {
        if (needsComma) {
            sb.append(',');
        }
        needsComma = true;
    }

    private void key(String name) {
        sb.append(quote(name)).append(':');
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
