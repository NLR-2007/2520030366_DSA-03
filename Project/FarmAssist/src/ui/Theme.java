package ui;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * ==========================================================================
 * THE LOOK AND FEEL OF SOPHIE
 * ==========================================================================
 * Everything the console draws goes through this class, so the whole app
 * shares one grid, one palette and one set of glyphs.
 *
 *   WIDTH    every panel, rule and table is exactly this many columns
 *   palette  TRUECOLOR (24 bit), BASIC (old 16 colour) or NONE (plain text)
 *   unicode  rounded box drawing, or a plain ASCII fallback
 *
 * Both are detected automatically and can be changed from the chat with
 * "color basic", "color off" or "ascii on" if a terminal misbehaves.
 * ==========================================================================
 */
public final class Theme {

    private Theme() { }

    /** The single grid every element is drawn to, margin included. */
    public static final int WIDTH = 78;
    /** Every line on the page starts here, so the left edge is one clean run. */
    private static final String MARGIN = " ";
    /** Width of a box or rule once the margin is taken off. */
    private static final int BOX = WIDTH - 1;
    /** Usable columns inside a panel: border, space, content, space, border. */
    public static final int INNER = BOX - 4;

    public enum Palette { TRUECOLOR, BASIC, NONE }

    // built from (char) 27 because Java decodes a backslash-u escape before
    // the compiler even sees the line.
    private static final String ESC   = String.valueOf((char) 27) + "[";
    private static final String RESET = ESC + "0m";
    private static final Pattern ANSI = Pattern.compile(((char) 27) + "\\[[0-9;]*m");

    public static Palette palette = detectPalette();
    public static boolean unicode = detectUnicode();

    // ------------------------------------------------------------- glyphs
    // reassigned by setUnicode(), so the fallback can be toggled at runtime.

    public static String BX_TL, BX_TR, BX_BL, BX_BR, BX_H, BX_V, BX_LT, BX_RT;
    public static String BAR, BLOCK, SHADE;
    public static String I_CROP, I_DISEASE, I_BUG, I_FERT, I_DOC, I_LENS, I_LINK,
                         I_WAVE, I_OK, I_WARN, I_ARROW, I_DOT, I_CHEVRON, I_SPARK,
                         I_CAL, I_SCALE;
    public static String ELLIPSIS;

    static { setUnicode(unicode); }

    public static void setUnicode(boolean on) {
        unicode = on;
        util.Trace.unicode = on;
        BX_TL = on ? "╭" : "+";   BX_TR = on ? "╮" : "+";
        BX_BL = on ? "╰" : "+";   BX_BR = on ? "╯" : "+";
        BX_H  = on ? "─" : "-";   BX_V  = on ? "│" : "|";
        BX_LT = on ? "├" : "+";   BX_RT = on ? "┤" : "+";
        BAR   = on ? "▌" : "|";
        BLOCK = on ? "█" : "#";
        SHADE = on ? "░" : ".";
        I_CROP    = on ? "◆" : "*";    // filled diamond - crops
        I_DISEASE = on ? "▲" : "!";    // triangle       - disease
        I_BUG     = on ? "◈" : "%";    // pitted diamond - pests
        I_CAL     = on ? "▦" : "+";    // grid           - the crop planner
        I_SCALE   = on ? "⇅" : "=";    // up down arrow  - side by side compare
        I_FERT    = on ? "■" : "#";    // square         - fertilizer bags
        I_DOC     = on ? "▪" : "-";    // small square   - articles
        I_LENS    = on ? "◇" : "?";    // hollow diamond - search
        I_LINK    = on ? "⇄" : "<>";   // arrows         - matching
        I_WAVE    = on ? "≈" : "~";    // waves          - related reading
        I_OK      = on ? "✓" : "v";
        I_WARN    = on ? "▲" : "!";
        I_ARROW   = on ? "→" : "->";
        I_DOT     = on ? "·" : ".";
        I_CHEVRON = on ? "›" : ">";
        I_SPARK   = on ? "✦" : "*";
        ELLIPSIS  = on ? "…" : "...";
    }

    // -------------------------------------------------------------- colour

    public static void setPalette(Palette p) {
        palette = p;
        util.Trace.colours = (p != Palette.NONE);
    }

    /** Kept for the old "color on / color off" commands. */
    public static void setColours(boolean on) {
        setPalette(on ? detectPalette() : Palette.NONE);
    }

    private static String code(int r, int g, int b, int basic) {
        if (palette == Palette.TRUECOLOR) return ESC + "38;2;" + r + ";" + g + ";" + b + "m";
        if (palette == Palette.BASIC)     return ESC + basic + "m";
        return "";
    }

    /**
     * Paint a string. Any reset already inside the text is followed by our own
     * colour again, so nesting like bold(leaf("a" + sun("b") + "c")) survives
     * instead of losing its colour halfway through the line.
     */
    private static String paint(String code, String s) {
        if (code.isEmpty() || palette == Palette.NONE) return s;
        String body = s.replace(RESET, RESET + code);
        // the text already ended in a reset, so the copy we just appended is
        // dead weight - drop it instead of leaving a stray code on the line
        if (body.endsWith(RESET + code)) body = body.substring(0, body.length() - code.length());
        return code + body + (body.endsWith(RESET) ? "" : RESET);
    }

    // the palette: a calm agritech set, with a 16 colour twin for old consoles
    public static String leaf(String s)  { return paint(code(126, 217, 137, 92), s); } // bright green
    public static String crop(String s)  { return paint(code( 96, 176, 116, 32), s); } // green
    public static String sun(String s)   { return paint(code(245, 197,  66, 93), s); } // amber
    public static String soil(String s)  { return paint(code(206, 145,  87, 33), s); } // clay
    public static String water(String s) { return paint(code( 94, 180, 224, 96), s); } // sky
    public static String alert(String s) { return paint(code(233, 110,  98, 91), s); } // red
    public static String iris(String s)  { return paint(code(166, 148, 232, 95), s); } // violet
    public static String chalk(String s) { return paint(code(226, 232, 228, 97), s); } // near white
    public static String stone(String s) { return paint(code(129, 140, 141, 90), s); } // muted
    public static String ash(String s)   { return paint(code( 84,  94,  96, 90), s); } // borders

    public static String bold(String s) { return paint(palette == Palette.NONE ? "" : ESC + "1m", s); }
    public static String dim(String s)  { return paint(palette == Palette.NONE ? "" : ESC + "2m", s); }

    // ------------------------------------------------ measuring and padding

    /** Text with every colour code removed. */
    public static String strip(String s) { return ANSI.matcher(s).replaceAll(""); }

    /** How many columns a string really occupies on screen. */
    public static int visible(String s) {
        int n = 0;
        boolean inEscape = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inEscape) { if (c == 'm') inEscape = false; continue; }
            if (c == 27)  { inEscape = true; continue; }
            n++;
        }
        return n;
    }

    /** Pad on the right. Safe on strings that are already coloured. */
    public static String padRight(String s, int width) {
        int gap = width - visible(s);
        return gap <= 0 ? s : s + " ".repeat(gap);
    }

    /** Pad on the left. Safe on strings that are already coloured. */
    public static String padLeft(String s, int width) {
        int gap = width - visible(s);
        return gap <= 0 ? s : " ".repeat(gap) + s;
    }

    /** Push a string towards the middle of the given width. */
    public static String centre(String s, int width) {
        int gap = width - visible(s);
        return gap <= 0 ? s : " ".repeat(gap / 2) + s;
    }

    /** Words that stay in capitals when a data file name is titled. */
    private static final java.util.Set<String> ACRONYMS = java.util.Set.of(
            "npk", "dap", "mop", "sop", "ssp", "tsp", "map", "ph");

    /**
     * The data files store names in lower case because that is what the string
     * matching works on. This dresses one up for the screen, so a table shows
     * "Farmyard Manure" and "NPK 10-26-26" rather than raw index text.
     */
    public static String title(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (sb.length() > 0) sb.append(' ');
            if (word.isEmpty()) continue;
            if (ACRONYMS.contains(word.toLowerCase())) {
                sb.append(word.toUpperCase());
            } else if (Character.isLetter(word.charAt(0))) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            } else {
                sb.append(word);
            }
        }
        return sb.toString();
    }

    /** Cut a plain string down to width, ending in an ellipsis. */
    public static String clip(String s, int width) {
        if (s == null) return "";
        if (s.length() <= width) return s;
        return s.substring(0, Math.max(0, width - ELLIPSIS.length())) + ELLIPSIS;
    }

    // ------------------------------------------------------------ structure

    /** Wipe the screen and park the cursor at the top left. */
    public static void clear() {
        if (palette != Palette.NONE) System.out.print(ESC + "2J" + ESC + "H");
    }

    /** A full width hair line, sitting on the page margin. */
    public static String rule() { return MARGIN + ash(BX_H.repeat(BOX)); }

    /** A dim rule of n columns. */
    public static String rule(int n) { return ash(BX_H.repeat(Math.max(0, n))); }

    /**
     * A heading that opens a block of prose:
     *
     *     | CROP PROFILE ----------------------------------------------
     */
    public static String section(String title) {
        String label = title.toUpperCase();
        return MARGIN + leaf(BAR) + " " + bold(chalk(label)) + " "
             + ash(BX_H.repeat(Math.max(3, BOX - 3 - label.length())));
    }

    /** Top edge of a panel, with the title sitting inside the border. */
    public static String panelTop(String icon, String title) {
        String label = (icon == null || icon.isEmpty() || !unicode)
                     ? title.toUpperCase()
                     : icon + "  " + title.toUpperCase();
        if (label.isEmpty()) return MARGIN + ash(BX_TL + BX_H.repeat(BOX - 2) + BX_TR);
        int fill = BOX - 5 - visible(label);
        return MARGIN + ash(BX_TL + BX_H + " ") + bold(sun(label)) + " "
             + ash(BX_H.repeat(Math.max(1, fill)) + BX_TR);
    }

    /** One content line of a panel, padded so the right edge always lines up. */
    public static String panelRow(String content) {
        return MARGIN + ash(BX_V) + " " + padRight(content, INNER) + " " + ash(BX_V);
    }

    /** A divider across the inside of a panel. */
    public static String panelSplit() {
        return MARGIN + ash(BX_LT + BX_H.repeat(BOX - 2) + BX_RT);
    }

    public static String panelBottom() {
        return MARGIN + ash(BX_BL + BX_H.repeat(BOX - 2) + BX_BR);
    }

    /** A small bar gauge, used for match scores. */
    public static String meter(int value, int max, int width) {
        if (max <= 0) max = 1;
        int on = Math.max(0, Math.min(width, (int) Math.round(value * (double) width / max)));
        return leaf(BLOCK.repeat(on)) + ash(SHADE.repeat(width - on));
    }

    // ------------------------------------------------------------- masthead

    private static final String[] LOGO = {
        " ████  ███  ████  █   █ █████ █████",
        "█     █   █ █   █ █   █   █   █    ",
        " ███  █   █ ████  █████   █   ████ ",
        "    █ █   █ █     █   █   █   █    ",
        "████   ███  █     █   █ █████ █████"
    };

    /** The start up masthead. */
    public static String banner() {
        StringBuilder sb = new StringBuilder();
        sb.append(panelTop("", "")).append('\n');
        sb.append(panelRow("")).append('\n');

        if (unicode) {
            // a soft gradient down the letters, deep green to bright leaf
            int[][] shade = {{ 74, 152,  95}, { 94, 176, 110}, {118, 202, 128},
                             {142, 219, 145}, {166, 232, 160}};
            for (int i = 0; i < LOGO.length; i++) {
                String tinted = paint(code(shade[i][0], shade[i][1], shade[i][2], 32), LOGO[i]);
                sb.append(panelRow(centre(tinted, INNER))).append('\n');
            }
        } else {
            sb.append(panelRow(centre(bold("S O P H I E"), INNER))).append('\n');
        }

        sb.append(panelRow("")).append('\n');
        String tag = crop("FarmAssist") + stone("  " + I_DOT + "  ")
                   + chalk("your digital agriculture officer");
        sb.append(panelRow(centre(tag, INNER))).append('\n');
        sb.append(panelBottom());
        return sb.toString();
    }

    /** What the assistant is called, everywhere on screen. */
    public static final String SPEAKER = "Sophie";

    /** Column the reply gutter bar sits in: " x " + name + two spaces. */
    private static final int GUTTER_COL = 3 + SPEAKER.length() + 2;

    /** The line Sophie speaks on. */
    public static String voice() {
        return " " + leaf(I_CROP) + " " + bold(leaf(SPEAKER)) + ash("  " + BX_V + " ");
    }

    /**
     * The gutter the rest of a reply hangs under. It carries the same bar as
     * voice(), so a wrapped answer reads as one block instead of drifting off
     * into open space.
     */
    public static String voiceIndent() {
        return " ".repeat(GUTTER_COL) + ash(BX_V) + " ";
    }

    /**
     * The line the farmer types on, opened by a rule so each exchange is
     * visibly its own turn rather than one long run of text.
     */
    public static String prompt() {
        return rule() + "\n " + bold(sun("you")) + ash("  " + I_CHEVRON + " ");
    }

    // ----------------------------------------------------------------- wrap

    /**
     * Wrap text to a width counting only the visible characters, so a line
     * holding colour codes still breaks in the right place.
     */
    public static String wrap(String text, int width, String indent) {
        StringBuilder out = new StringBuilder();
        int line = 0;
        boolean startOfLine = true;
        for (String word : text.trim().split("\\s+")) {
            int w = visible(word);
            if (!startOfLine && line + 1 + w > width) {
                out.append('\n').append(indent);
                line = 0;
                startOfLine = true;
            }
            if (!startOfLine) { out.append(' '); line++; }
            out.append(word);
            line += w;
            startOfLine = false;
        }
        return out.toString();
    }

    // ---------------------------------------------------------------- table

    /**
     * A boxed table on the shared grid. Cells are handed over as plain text
     * and coloured at print time, so the padding is never thrown off by escape
     * codes and over long cells can be clipped safely.
     */
    public static final class Table {

        private static final int GAP = 2;

        private final String icon, title;
        private final List<String> heads = new ArrayList<>();
        private final List<Integer> widths = new ArrayList<>();
        private final List<Boolean> rightAlign = new ArrayList<>();
        private final List<Function<String, String>> inks = new ArrayList<>();
        private final List<String[]> rows = new ArrayList<>();
        private final List<Boolean> strong = new ArrayList<>();

        private Table(String icon, String title) { this.icon = icon; this.title = title; }

        public static Table of(String icon, String title) { return new Table(icon, title); }

        /** A column. Width 0 marks the column that absorbs the leftover space. */
        public Table col(String head, int width, boolean right, Function<String, String> ink) {
            heads.add(head);
            widths.add(width);
            rightAlign.add(right);
            inks.add(ink == null ? s -> s : ink);
            return this;
        }

        public Table row(String... cells) {
            rows.add(cells);
            strong.add(false);
            return this;
        }

        /** A row printed in bold, for totals. */
        public Table total(String... cells) {
            rows.add(cells);
            strong.add(true);
            return this;
        }

        public boolean isEmpty() { return rows.isEmpty(); }

        public void print() {
            int fixed = 0, flexible = -1;
            for (int i = 0; i < widths.size(); i++) {
                if (widths.get(i) == 0) flexible = i; else fixed += widths.get(i);
            }
            int gaps = GAP * (widths.size() - 1);
            if (flexible >= 0) widths.set(flexible, Math.max(8, INNER - fixed - gaps));

            System.out.println(panelTop(icon, title));
            System.out.println(panelRow(headLine()));
            System.out.println(panelSplit());
            for (int r = 0; r < rows.size(); r++) System.out.println(panelRow(bodyLine(r)));
            System.out.println(panelBottom());
        }

        private String headLine() {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < heads.size(); c++) {
                if (c > 0) sb.append(" ".repeat(GAP));
                String cell = clip(heads.get(c).toUpperCase(), widths.get(c));
                sb.append(stone(rightAlign.get(c) ? padLeft(cell, widths.get(c))
                                                  : padRight(cell, widths.get(c))));
            }
            return sb.toString();
        }

        private String bodyLine(int r) {
            String[] cells = rows.get(r);
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < widths.size(); c++) {
                if (c > 0) sb.append(" ".repeat(GAP));
                String cell = clip(c < cells.length ? cells[c] : "", widths.get(c));
                cell = rightAlign.get(c) ? padLeft(cell, widths.get(c))
                                         : padRight(cell, widths.get(c));
                sb.append(inks.get(c).apply(cell));
            }
            return strong.get(r) ? bold(sb.toString()) : sb.toString();
        }
    }

    // ------------------------------------------------------------ detection

    private static Palette detectPalette() {
        if (System.getenv("NO_COLOR") != null) return Palette.NONE;
        String term = System.getenv("TERM");
        if ("dumb".equals(term)) return Palette.NONE;
        String colorTerm = System.getenv("COLORTERM");
        if (colorTerm != null && (colorTerm.contains("truecolor") || colorTerm.contains("24bit")))
            return Palette.TRUECOLOR;
        if (System.getenv("WT_SESSION") != null) return Palette.TRUECOLOR;  // Windows Terminal
        if (term != null && term.contains("256")) return Palette.TRUECOLOR;
        // Windows 10 and 11 consoles accept 24 bit codes and approximate them
        return Palette.TRUECOLOR;
    }

    /** Only draw box characters if the console can actually encode them. */
    private static boolean detectUnicode() {
        String probe = "" + (char) 0x2500 + (char) 0x2588 + (char) 0x25c6 + (char) 0x2192;
        String enc = System.getProperty("stdout.encoding");
        if (enc == null) enc = System.getProperty("sun.stdout.encoding");
        if (enc == null) enc = System.getProperty("native.encoding");
        if (enc == null) enc = System.getProperty("file.encoding");
        try {
            return enc != null && Charset.forName(enc).newEncoder().canEncode(probe);
        } catch (Exception ignored) {
            return false;
        }
    }
}
