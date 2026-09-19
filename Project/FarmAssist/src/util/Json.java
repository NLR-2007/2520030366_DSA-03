package util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A very small JSON reader, enough to unpack a weather reply without pulling
 * in a library. Objects become Map, arrays become List, numbers become Double,
 * and strings, booleans and null are themselves.
 *
 *   Object o = Json.parse(text);
 *   Json.num(o, "main", "temp")            -> 26.3
 *   Json.str(o, "weather", 0, "description") -> "overcast clouds"
 */
public final class Json {

    private final String s;
    private int i = 0;

    private Json(String s) { this.s = s; }

    public static Object parse(String text) {
        Json j = new Json(text);
        j.ws();
        Object v = j.value();
        j.ws();
        return v;
    }

    // ---------------------------------------------------------------- paths

    /** Walk a path of map keys (String) and list indexes (Integer). Null if absent. */
    public static Object get(Object root, Object... path) {
        Object cur = root;
        for (Object step : path) {
            if (cur instanceof Map && step instanceof String) {
                cur = ((Map<?, ?>) cur).get(step);
            } else if (cur instanceof List && step instanceof Integer) {
                List<?> l = (List<?>) cur;
                int k = (Integer) step;
                cur = (k >= 0 && k < l.size()) ? l.get(k) : null;
            } else {
                return null;
            }
            if (cur == null) return null;
        }
        return cur;
    }

    public static double num(Object root, Object... path) {
        Object v = get(root, path);
        return v instanceof Number ? ((Number) v).doubleValue() : 0;
    }

    public static String str(Object root, Object... path) {
        Object v = get(root, path);
        return v == null ? "" : String.valueOf(v);
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Object root, Object... path) {
        Object v = get(root, path);
        return v instanceof List ? (List<Object>) v : new ArrayList<>();
    }

    // --------------------------------------------------------------- parser

    private Object value() {
        if (i >= s.length()) throw err("unexpected end");
        char c = s.charAt(i);
        if (c == '{') return object();
        if (c == '[') return array();
        if (c == '"') return string();
        if (c == 't') { expect("true");  return Boolean.TRUE; }
        if (c == 'f') { expect("false"); return Boolean.FALSE; }
        if (c == 'n') { expect("null");  return null; }
        return number();
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++;                                    // {
        ws();
        if (peek() == '}') { i++; return m; }
        while (true) {
            ws();
            String key = string();
            ws();
            if (peek() != ':') throw err("expected :");
            i++;
            ws();
            m.put(key, value());
            ws();
            char c = peek();
            if (c == ',') { i++; continue; }
            if (c == '}') { i++; return m; }
            throw err("expected , or }");
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        i++;                                    // [
        ws();
        if (peek() == ']') { i++; return l; }
        while (true) {
            ws();
            l.add(value());
            ws();
            char c = peek();
            if (c == ',') { i++; continue; }
            if (c == ']') { i++; return l; }
            throw err("expected , or ]");
        }
    }

    private String string() {
        if (peek() != '"') throw err("expected string");
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '"') return sb.toString();
            if (c != '\\') { sb.append(c); continue; }
            char e = s.charAt(i++);
            switch (e) {
                case 'n': sb.append('\n'); break;
                case 't': sb.append('\t'); break;
                case 'r': sb.append('\r'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'u':
                    sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                    i += 4;
                    break;
                default:  sb.append(e);            // \" \\ \/
            }
        }
        throw err("unterminated string");
    }

    private Double number() {
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        if (start == i) throw err("unexpected character '" + s.charAt(i) + "'");
        return Double.parseDouble(s.substring(start, i));
    }

    private void expect(String word) {
        if (!s.startsWith(word, i)) throw err("expected " + word);
        i += word.length();
    }

    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private char peek() { return i < s.length() ? s.charAt(i) : '\0'; }

    private IllegalArgumentException err(String what) {
        return new IllegalArgumentException("bad json at " + i + ": " + what);
    }
}
