package util;

import java.util.ArrayList;
import java.util.List;

/**
 * Prints a one line note every time one of the eight algorithms runs.
 * This is what makes the algorithm usage VISIBLE during the demo.
 * Turn it off from the chat with the command:  trace off
 *
 * The notes sit in a dim gutter so they stay behind the real answer instead
 * of competing with it. A long note folds under its own column and keeps the
 * gutter bar, so a run of traces reads as one unbroken line down the page:
 *
 *   | Aho-Corasick       crops=[rice] diseases=[leaf blight] fertilizers=[]
 *   |                    symptoms=[] pests=[]
 *   | Intent routing     question classified as DISEASE_DIAGNOSIS
 */
public class Trace {

    public static boolean enabled = true;

    /** Set to false by the "color off" command through Theme. */
    public static boolean colours = true;

    /** Set to false when the console cannot draw box characters. */
    public static boolean unicode = true;

    /** Kept in step with Theme.WIDTH so the trace folds on the same grid. */
    private static final int WIDTH = 78;
    private static final String INDENT = " ";          // same left margin as the speaker
    private static final int LABEL_W = 18;

    private static final String ESC    = String.valueOf((char) 27) + "[";
    private static final String RESET  = ESC + "0m";
    private static final String GUTTER = ESC + "38;2;96;106;108m";   // dim slate
    private static final String TEXT   = ESC + "38;2;129;140;141m";  // muted grey

    /**
     * Set by the ui layer so the trace can tell whether it is continuing a run
     * of its own lines or starting a fresh one under somebody else's block.
     * Left null when nothing has wired them up, and then nothing changes.
     */
    public static java.util.function.LongSupplier lineCounter;
    public static java.util.function.BooleanSupplier atBlankLine;

    private static long lastLine = Long.MIN_VALUE;

    public static void log(String algorithm, String message) {
        if (!enabled) return;

        // a fresh run of traces gets a blank line above it; lines that carry on
        // from the trace before stay packed together as one block
        if (lineCounter != null && atBlankLine != null
                && lineCounter.getAsLong() != lastLine && !atBlankLine.getAsBoolean()) {
            System.out.println();
        }

        String bar = unicode ? "│" : "|";
        int used = INDENT.length() + 2 + LABEL_W + 1;  // margin + bar + space + label
        List<String> lines = fold(message, Math.max(20, WIDTH - used));

        for (int i = 0; i < lines.size(); i++) {
            // the label belongs to the first line only; the rest hang under it
            String label = (i == 0) ? pad(algorithm) : " ".repeat(LABEL_W + 1);
            if (colours) {
                System.out.println(INDENT + GUTTER + bar + RESET + " "
                        + TEXT + label + lines.get(i) + RESET);
            } else {
                System.out.println(INDENT + bar + " " + label + lines.get(i));
            }
        }
        if (lineCounter != null) lastLine = lineCounter.getAsLong();
    }

    /** Break a long note into lines that fit the message column. */
    private static List<String> fold(String message, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : message.trim().split("\\s+")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > width) {
                out.add(line.toString());
                line.setLength(0);
            } else if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        out.add(line.toString());
        return out;
    }

    private static String pad(String s) {
        StringBuilder sb = new StringBuilder(s.length() > LABEL_W ? s.substring(0, LABEL_W) : s);
        while (sb.length() < LABEL_W + 1) sb.append(' ');
        return sb.toString();
    }
}
