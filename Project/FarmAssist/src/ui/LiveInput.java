package ui;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

/**
 * The search box.
 *
 * Reads the farmer's question one key at a time and, after every key, asks
 * the trie for completions and draws them in a drop down under the prompt -
 * the way a video site fills its suggestion list while you are still typing.
 *
 *   letters      type, the list refreshes
 *   up / down    move the highlight through the list
 *   tab          copy the highlighted suggestion into the box
 *   enter        ask (the highlighted suggestion if one is chosen)
 *   escape       hide the list
 *   ctrl-c       leave
 *
 * Raw key reading needs the console switched out of line mode, which plain
 * Java cannot do, so JLine (lib/jline-*.jar) does that one job. When there is
 * no real console - input piped from a file, or an IDE run window - it quietly
 * falls back to ordinary line input and the chat works exactly as before.
 */
public class LiveInput implements AutoCloseable {

    private static final int MAX_SUGGESTIONS = 6;

    private final Function<String, List<String>> suggester;
    private Terminal term;             // null when we fell back to Scanner
    private Scanner fallback;

    public LiveInput(Function<String, List<String>> suggester) {
        this.suggester = suggester;
        try {
            if (System.console() == null) throw new IllegalStateException("no console");
            term = TerminalBuilder.builder()
                    .system(true).jni(true).jna(false).jansi(false).ffm(false)
                    .dumb(false).build();
            if ("dumb".equals(term.getType())) { term.close(); term = null; }
        } catch (Throwable e) {
            term = null;
        }
        if (term == null) fallback = new Scanner(System.in);
    }

    /** For tests: drive the reader from a terminal built on streams. */
    LiveInput(Function<String, List<String>> suggester, Terminal terminal) {
        this.suggester = suggester;
        this.term = terminal;
    }

    public boolean isLive() { return term != null; }

    /** Prints the prompt and returns one line, or null at end of input. */
    public String readLine() {
        if (term == null) {
            System.out.print(Theme.prompt());
            if (!fallback.hasNextLine()) return null;
            return fallback.nextLine().trim();
        }
        System.out.println(Theme.rule());
        System.out.print(Theme.promptLine());
        System.out.flush();
        return readLive();
    }

    // ------------------------------------------------------------ live loop

    private final StringBuilder buf = new StringBuilder();
    private List<String> shown = new ArrayList<>();
    private int selected = -1;
    private int linesBelow = 0;

    private String readLive() {
        buf.setLength(0);
        shown = new ArrayList<>();
        selected = -1;
        linesBelow = 0;

        Attributes saved = term.enterRawMode();
        try {
            NonBlockingReader in = term.reader();
            int pending = -1;                           // a key read early, not yet handled
            while (true) {
                int c = pending >= 0 ? pending : in.read();
                pending = -1;
                if (c < 0) return null;

                if (c == 27) {                          // escape, maybe an arrow
                    int n = in.read(60);
                    if (n == '[' || n == 'O') {
                        int k = in.read(60);
                        if (k == 'A') move(-1);
                        else if (k == 'B') move(+1);
                        draw();
                    } else {
                        shown = new ArrayList<>(); selected = -1; draw();
                        pending = n;                    // a real key followed the escape
                    }
                    continue;
                }
                if (key(c)) return finish();
            }
        } catch (Exception e) {
            return finish();
        } finally {
            term.setAttributes(saved);
        }
    }

    /** Handle one ordinary key. Returns true when the line is complete. */
    private boolean key(int c) {
        if (c == 3) {                                   // ctrl-c
            buf.setLength(0); buf.append("exit");
            return true;
        }
        if (c == '\r' || c == '\n') {
            if (selected >= 0) { buf.setLength(0); buf.append(shown.get(selected)); }
            return true;
        }
        if (c == '\t') {
            if (!shown.isEmpty()) {
                buf.setLength(0);
                buf.append(shown.get(Math.max(0, selected)));
            }
            refresh();
            return false;
        }
        if (c == 127 || c == 8) {                       // backspace
            if (buf.length() > 0) buf.setLength(buf.length() - 1);
            refresh();
            return false;
        }
        if (c >= 32) {
            buf.append((char) c);
            refresh();
        }
        return false;
    }

    private void move(int step) {
        if (shown.isEmpty()) return;
        selected += step;
        if (selected < -1) selected = shown.size() - 1;
        if (selected >= shown.size()) selected = -1;
    }

    /** Ask the trie again and redraw. */
    private void refresh() {
        selected = -1;
        shown = buf.length() == 0 ? new ArrayList<>()
                                  : suggester.apply(buf.toString());
        if (shown.size() > MAX_SUGGESTIONS) shown = shown.subList(0, MAX_SUGGESTIONS);
        draw();
    }

    /** Clear the drop down, leave the chosen text on the prompt line, end the line. */
    private String finish() {
        shown = new ArrayList<>();
        selected = -1;
        draw();
        PrintWriter w = term.writer();
        w.print("\n");
        w.flush();
        return buf.toString().trim();
    }

    // -------------------------------------------------------------- drawing

    private static final String CSI = String.valueOf((char) 27) + "[";

    /**
     * Repaint the prompt line and the drop down beneath it. The cursor always
     * rests on the prompt line between keys, so we go up over whatever we drew
     * last time, wipe to the end of the screen, and paint again.
     */
    private void draw() {
        PrintWriter w = term.writer();
        String pad = " ".repeat(Centering.margin());
        String typed = buf.toString();

        StringBuilder sb = new StringBuilder();
        if (linesBelow > 0) sb.append(CSI).append(linesBelow).append('A');
        sb.append('\r').append(CSI).append('J');

        String line = selected >= 0 ? shown.get(selected) : typed;
        sb.append(pad).append(Theme.promptLine()).append(Theme.chalk(line));

        int below = 0;
        for (int i = 0; i < shown.size(); i++) {
            sb.append('\n').append(pad).append(row(shown.get(i), typed, i == selected));
            below++;
        }
        if (!shown.isEmpty()) {
            sb.append('\n').append(pad).append("      ")
              .append(Theme.ash(Theme.I_ARROW + " tab fills   " + Theme.I_DOT + "   up / down picks   "
                                + Theme.I_DOT + "   enter asks"));
            below++;
        }
        linesBelow = below;

        if (below > 0) sb.append(CSI).append(below).append('A');
        int col = Centering.margin() + Theme.visible(Theme.promptLine()) + line.length();
        sb.append('\r');
        if (col > 0) sb.append(CSI).append(col).append('C');

        w.print(sb);
        w.flush();
    }

    /** One drop down row: the part already typed in white, the rest muted. */
    private static String row(String phrase, String typed, boolean picked) {
        String t = typed.trim().toLowerCase();
        String text;
        if (!t.isEmpty() && phrase.startsWith(t)) {
            text = Theme.chalk(phrase.substring(0, t.length())) + Theme.stone(phrase.substring(t.length()));
        } else {
            text = Theme.stone(phrase);
        }
        if (picked) text = Theme.sun(phrase);
        String mark = picked ? Theme.sun(Theme.I_CHEVRON) : Theme.ash(Theme.I_LENS);
        return "   " + mark + "  " + text;
    }

    @Override public void close() {
        try { if (term != null) term.close(); } catch (Exception ignored) { }
        if (fallback != null) fallback.close();
    }
}
