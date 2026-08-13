package ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Puts the whole chat in the middle of the terminal window.
 *
 * Instead of changing every print statement, System.out is replaced by a
 * stream that adds a left margin at the start of every line. Everything the
 * program prints - masthead, panels, tables, even the algorithm trace - is
 * shifted together, so the layout never breaks.
 *
 * The terminal width is read once at start up. If that fails we assume a
 * comfortable default, and the farmer can always set it by typing  width 120
 * in the chat.
 */
public class Centering {

    /** How wide our printed content actually is. One grid for the whole app. */
    public static final int CONTENT_WIDTH = Theme.WIDTH;

    private static final PrintStream ORIGINAL = System.out;

    private static int terminalWidth = detectWidth();
    private static boolean enabled = true;

    public static int terminalWidth() { return terminalWidth; }

    public static int margin() {
        if (!enabled) return 0;
        return Math.max(0, (terminalWidth - CONTENT_WIDTH) / 2);
    }

    private static MarginStream current;

    /** Start centring, or re-apply after the width changed. */
    public static void apply() {
        current = new MarginStream(ORIGINAL, margin());
        System.setOut(new PrintStream(current, true));
    }

    /**
     * Was the last line printed an empty one? This lets the chat keep exactly
     * one blank line between blocks without every caller having to know what
     * the caller before it printed.
     */
    public static boolean lastLineWasBlank() {
        return current == null || current.lastLineEmpty;
    }

    /** How many lines have been printed so far, used to group the trace. */
    public static long lineCount() {
        return current == null ? 0 : current.lines;
    }

    public static void setWidth(int columns) {
        terminalWidth = Math.max(CONTENT_WIDTH, columns);
        enabled = true;
        apply();
    }

    public static void off() {
        enabled = false;
        apply();                     // margin of zero, but keep the tracking
    }

    public static boolean isOn() { return enabled; }

    /** Ask the console how many columns it has. */
    private static int detectWidth() {
        int fromEnv = parse(System.getenv("COLUMNS"));
        if (fromEnv > 0) return fromEnv;

        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            try {
                Process p = new ProcessBuilder("cmd", "/c", "mode", "con")
                        .redirectErrorStream(true).start();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.toLowerCase().contains("column")) {
                            int w = parse(line.replaceAll("[^0-9]", ""));
                            if (w > 0) return w;
                        }
                    }
                }
                p.waitFor();
            } catch (Exception ignored) {
                // no console attached - fall through to the default
            }
        }
        return 100;
    }

    private static int parse(String digits) {
        if (digits == null || digits.isEmpty()) return -1;
        try {
            int w = Integer.parseInt(digits.trim());
            return (w >= 40 && w <= 400) ? w : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Adds the left margin at the beginning of every line. */
    private static class MarginStream extends OutputStream {
        private final OutputStream out;
        private final byte[] pad;
        private boolean atLineStart = true;
        private boolean lineHasContent = false;
        boolean lastLineEmpty = true;
        long lines = 0;

        MarginStream(OutputStream out, int margin) {
            this.out = out;
            this.pad = new byte[Math.max(0, margin)];
            java.util.Arrays.fill(this.pad, (byte) ' ');
        }

        @Override public void write(int b) throws IOException {
            if (atLineStart && b != '\n' && b != '\r') {
                out.write(pad);              // indent this line
                atLineStart = false;
            }
            out.write(b);
            note(b);
        }

        /** Remember whether the line we just finished had anything on it. */
        private void note(int b) {
            if (b == '\n') {
                atLineStart = true;
                lastLineEmpty = !lineHasContent;
                lineHasContent = false;
                lines++;
            } else if (b != '\r') {
                lineHasContent = true;
            }
        }

        /**
         * Written out block by block rather than byte by byte. Without this the
         * default implementation calls write(int) once per character, which is
         * noticeably slow for a screen full of colour codes.
         */
        @Override public void write(byte[] b, int off, int len) throws IOException {
            int start = off;
            for (int i = off; i < off + len; i++) {
                if (b[i] == '\n') {
                    flushChunk(b, start, i - start + 1);
                    start = i + 1;
                }
            }
            if (start < off + len) flushChunk(b, start, off + len - start);
        }

        private void flushChunk(byte[] b, int off, int len) throws IOException {
            if (len <= 0) return;
            if (atLineStart && b[off] != '\n' && b[off] != '\r') {
                out.write(pad);
                atLineStart = false;
            }
            out.write(b, off, len);
            for (int i = off; i < off + len; i++) note(b[i]);
        }

        @Override public void flush() throws IOException { out.flush(); }
    }
}
