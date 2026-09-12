package app;

/**
 * Everything the console prints goes through here.
 *
 * Two things are worked out once, at start up, and every drawing method then
 * respects them:
 *
 *   colour   ANSI escapes are used only when the output really is a terminal.
 *            Piping the run into a file, or setting NO_COLOR, turns them off,
 *            so the transcript stays clean.
 *   glyphs   box drawing characters are used only when the console is running
 *            in UTF-8. On a code page 437 console the same drawings fall back
 *            to plain ASCII instead of coming out as mojibake.
 *
 * No layout maths ever counts an escape sequence: text is padded and wrapped
 * while it is still plain, and colour is put on afterwards.
 */
public final class Ui {

    /** Total drawing width. 78 fits an 80 column console with a margin. */
    public static final int WIDTH = 78;

    private static boolean colour  = detectColour();
    private static boolean unicode = detectUnicode();

    private Ui() { }

    // ------------------------------------------------------------------
    //  what the console can do
    // ------------------------------------------------------------------

    private static boolean detectColour() {
        if (System.getenv("NO_COLOR") != null) return false;
        if ("dumb".equals(System.getenv("TERM"))) return false;
        // null when the output is redirected, or the run came from an IDE that
        // gives no real console - escapes would only be noise there
        return System.console() != null;
    }

    private static boolean detectUnicode() {
        String enc = System.getProperty("stdout.encoding");
        if (enc == null) enc = System.getProperty("file.encoding", "");
        return enc.toLowerCase().contains("utf");
    }

    public static void colour(boolean on) { colour = on; }
    public static boolean colour()        { return colour; }
    public static void ascii(boolean on)  { unicode = !on; }

    /** Reads --no-color / --color / --ascii out of the command line. */
    public static void applyFlags(String[] args) {
        for (String a : args) {
            switch (a) {
                case "--no-color": case "--no-colour": colour = false;  break;
                case "--color":    case "--colour":    colour = true;   break;
                case "--ascii":                        unicode = false; break;
                default: /* the data folder, handled by Main */ break;
            }
        }
    }

    // ------------------------------------------------------------------
    //  colour
    // ------------------------------------------------------------------

    private static final String ESC   = ((char) 27) + "[";
    private static final String RESET = ESC + "0m";

    public static final String BOLD    = ESC + "1m";
    public static final String MUTED   = ESC + "90m";      // bright black
    public static final String ACCENT  = ESC + "96m";      // bright cyan
    public static final String CROP    = ESC + "92m";      // bright green
    public static final String SYMPTOM = ESC + "93m";      // bright yellow
    public static final String DISEASE = ESC + "91m";      // bright red
    public static final String HEAD    = ESC + "1;97m";    // bold white
    public static final String MARK    = ESC + "30;103m";  // black on yellow

    /** Wraps text in an escape, or hands it straight back when colour is off. */
    public static String paint(String text, String code) {
        return colour ? code + text + RESET : text;
    }

    public static String bold(String s)   { return paint(s, BOLD); }
    public static String muted(String s)  { return paint(s, MUTED); }
    public static String accent(String s) { return paint(s, ACCENT); }
    public static String head(String s)   { return paint(s, HEAD); }

    /** The colour that belongs to an Aho-Corasick match type. */
    public static String typeColour(String type) {
        switch (type) {
            case "CROP":    return CROP;
            case "SYMPTOM": return SYMPTOM;
            case "DISEASE": return DISEASE;
            default:        return ACCENT;
        }
    }

    // ------------------------------------------------------------------
    //  glyphs
    // ------------------------------------------------------------------

    /** Picks the box drawing character, or its ASCII stand in. */
    public static String g(String uni, String plain) {
        return unicode ? uni : plain;
    }

    public static String bullet() { return g("·", "-"); }
    public static String arrow()  { return g("▸", ">"); }
    public static String back()   { return g("◂", "<"); }
    public static String chev()   { return g("›", ">"); }
    public static String dot()    { return g("●", "*"); }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder(Math.max(0, s.length() * n));
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    // ------------------------------------------------------------------
    //  blocks
    // ------------------------------------------------------------------

    /** The title box drawn once at start up. */
    public static void banner(String title, String... lines) {
        String h  = g("─", "-");
        String v  = g("│", "|");
        String tl = g("╭", "+"), tr = g("╮", "+");
        String bl = g("╰", "+"), br = g("╯", "+");
        int inner = WIDTH - 2;

        System.out.println();
        System.out.println(paint(tl + repeat(h, inner) + tr, ACCENT));
        System.out.println(paint(v, ACCENT) + head(pad("  " + title, inner)) + paint(v, ACCENT));
        for (String line : lines) {
            System.out.println(paint(v, ACCENT) + muted(pad("  " + line, inner)) + paint(v, ACCENT));
        }
        System.out.println(paint(bl + repeat(h, inner) + br, ACCENT));
    }

    /** A titled section: a left edge, the title, then a rule out to the width. */
    public static void section(String title) {
        section(title, "");
    }

    /** The same, with a quiet note after the title saying how the list was made. */
    public static void section(String title, String note) {
        String edge = g("▌", "|");
        String h    = g("─", "-");
        String tail = note.isEmpty() ? "" : "  " + note;

        int used = 4 + title.length() + tail.length() + 1;
        String rule = used < WIDTH ? " " + repeat(h, WIDTH - used) : "";

        System.out.println();
        System.out.println("  " + paint(edge, ACCENT) + " " + head(title)
                + muted(tail) + muted(rule));
    }

    /** A thin divider between the cards inside a section. */
    public static void divider() {
        System.out.println("  " + muted(repeat(g("┈", "-"), WIDTH - 4)));
    }

    /** An aligned  label   value  line inside a card. */
    public static void field(String label, String value, int indent) {
        System.out.println(spaces(indent) + muted(pad(label, 10))
                + wrap(value, indent + 10));
    }

    /** The numbered heading of a result card, with its score bar on the right. */
    public static void card(int number, String title, int score, int max) {
        String shown = shorten(title, WIDTH - 26);
        String left  = "  " + paint(number + ".", ACCENT) + " " + head(shown);
        int plainLen = 5 + shown.length();
        int gap = WIDTH - plainLen - 16;
        System.out.println(left + spaces(Math.max(2, gap))
                + scoreBar(score, max) + " " + bold(String.valueOf(score)));
    }

    /** Ten cells, filled in proportion to the best score in the list. */
    public static String scoreBar(int score, int max) {
        int cells = 10;
        if (max <= 0) max = 1;
        int on = (int) Math.round((double) score / max * cells);
        if (on < 1) on = 1;
        if (on > cells) on = cells;
        return paint(repeat(g("█", "#"), on), ACCENT)
             + muted(repeat(g("░", "."), cells - on));
    }

    // ------------------------------------------------------------------
    //  text
    // ------------------------------------------------------------------

    /**
     * Folds text to the drawing width. The indent is the column the text
     * starts in, so the fold point allows for it and every continuation line
     * lines up underneath the first.
     */
    public static String wrap(String text, int indent) {
        int room = WIDTH - indent;
        if (room < 20) room = 20;

        StringBuilder out = new StringBuilder();
        int used = 0;
        for (String word : text.split("\\s+")) {
            int len = visibleLength(word);      // a word may carry colour of its own
            if (used > 0 && used + 1 + len > room) {
                out.append('\n').append(spaces(indent));
                used = 0;
            } else if (used > 0) {
                out.append(' ');
                used++;
            }
            out.append(word);
            used += len;
        }
        return out.toString();
    }

    /**
     * The width a string really takes on screen. An escape sequence is several
     * characters long and prints as none of them, so it is skipped. Every
     * padding and folding decision is made with this, not String.length().
     */
    public static int visibleLength(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == 27) {
                // an escape runs up to its first letter, e.g. ESC [ 9 6 m
                while (i < s.length() && !Character.isLetter(s.charAt(i))) i++;
                continue;
            }
            n++;
        }
        return n;
    }

    /** Paints every occurrence of term, keeping the original casing. */
    public static String highlight(String text, String term, String code) {
        if (!colour || term == null || term.isEmpty()) return text;

        String hay = text.toLowerCase();
        String needle = term.toLowerCase();
        StringBuilder out = new StringBuilder();
        int from = 0, at;
        while ((at = hay.indexOf(needle, from)) >= 0) {
            out.append(text, from, at)
               .append(code).append(text, at, at + needle.length()).append(RESET);
            from = at + needle.length();
        }
        return out.append(text.substring(from)).toString();
    }

    public static String pad(String s, int width) {
        int len = visibleLength(s);
        return len >= width ? s : s + spaces(width - len);
    }

    public static String spaces(int n) {
        return n <= 0 ? "" : repeat(" ", n);
    }

    public static String shorten(String s, int max) {
        if (max < 4 || s.length() <= max) return s;
        return s.substring(0, max - 1).trim() + g("…", "...");
    }

    public static void clear() {
        if (colour) System.out.print(ESC + "2J" + ESC + "H");
        else System.out.println("\n\n");
    }
}
