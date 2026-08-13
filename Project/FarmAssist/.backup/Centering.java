package ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Puts the whole chat in the middle of the terminal window.
 *
 * Instead of changing every print statement, we replace System.out with a
 * stream that adds a left margin at the start of every line. Everything the
 * program prints - banner, tables, snippets, even the algorithm trace - is
 * shifted together, so the layout never breaks.
 *
 * The terminal width is read once from the Windows "mode con" command. If that
 * fails we assume a comfortable default, and the farmer can always set it by
 * typing  width 120  in the chat.
 */
public class Centering {

    /** How wide our printed content actually is. */
    public static final int CONTENT_WIDTH = 70;

    private static final PrintStream ORIGINAL = System.out;

    private static int terminalWidth = detectWidth();
    private static boolean enabled = true;

    public static int terminalWidth() { return terminalWidth; }
    public static int margin() {
        if (!enabled) return 0;
        return Math.max(0, (terminalWidth - CONTENT_WIDTH) / 2);
    }

    /** Start centring, or re-apply after the width changed. */
    public static void apply() {
        System.setOut(new PrintStream(new MarginStream(ORIGINAL, margin()), true));
    }

    public static void setWidth(int columns) {
        terminalWidth = Math.max(CONTENT_WIDTH, columns);
        enabled = true;
        apply();
    }

    public static void off() {
        enabled = false;
        System.setOut(ORIGINAL);
    }

    public static boolean isOn() { return enabled; }

    /** Ask Windows how many columns the console has. */
    private static int detectWidth() {
        try {
            Process p = new ProcessBuilder("cmd", "/c", "mode", "con")
                    .redirectErrorStream(true).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.toLowerCase().contains("column")) {
                        String digits = line.replaceAll("[^0-9]", "");
                        if (!digits.isEmpty()) {
                            int w = Integer.parseInt(digits);
                            if (w >= 40 && w <= 400) return w;
                        }
                    }
                }
            }
            p.waitFor();
        } catch (Exception ignored) {
            // not Windows, or no console attached - fall through to the default
        }
        return 100;
    }

    /** Adds the left margin at the beginning of every line. */
    private static class MarginStream extends OutputStream {
        private final OutputStream out;
        private final byte[] pad;
        private boolean atLineStart = true;

        MarginStream(OutputStream out, int margin) {
            this.out = out;
            this.pad = new byte[margin];
            java.util.Arrays.fill(this.pad, (byte) ' ');
        }

        @Override public void write(int b) throws java.io.IOException {
            if (atLineStart && b != '\n' && b != '\r') {
                out.write(pad);              // indent this line
                atLineStart = false;
            }
            out.write(b);
            if (b == '\n') atLineStart = true;
        }

        @Override public void flush() throws java.io.IOException { out.flush(); }
    }
}
