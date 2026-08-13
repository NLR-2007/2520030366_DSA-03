package ui;

/**
 * The farming look and feel of the console.
 *
 * Colours are ANSI escape codes. They work in Windows Terminal, PowerShell and
 * modern cmd. If a terminal shows strange characters like [32m, just type
 * "color off" in the chat and everything falls back to plain text.
 */
public class Theme {

    public static boolean colours = true;

    /** Turn colour on or off everywhere, including the algorithm trace. */
    public static void setColours(boolean on) {
        colours = on;
        util.Trace.colours = on;
    }

    // built with (char) 27 instead of a unicode escape, because Java turns
    // a \ u escape into a real character before the code is even compiled.
    private static final String ESC = String.valueOf((char) 27) + "[";

    private static final String RESET  = ESC + "0m";
    private static final String BOLD   = ESC + "1m";
    private static final String DIM    = ESC + "2m";

    private static final String LEAF   = ESC + "92m";   // bright green - crops
    private static final String CROP   = ESC + "32m";   // green        - plants
    private static final String SUN    = ESC + "93m";   // bright yellow - sun, harvest
    private static final String SOIL   = ESC + "33m";   // dark yellow   - soil, bags
    private static final String WATER  = ESC + "96m";   // cyan          - water, info
    private static final String ALERT  = ESC + "91m";   // red           - disease
    private static final String STONE  = ESC + "90m";   // grey          - quiet text

    // ------------------------------------------------------------ colouring

    private static String paint(String code, String text) {
        return colours ? code + text + RESET : text;
    }

    public static String leaf(String s)  { return paint(LEAF, s); }
    public static String crop(String s)  { return paint(CROP, s); }
    public static String sun(String s)   { return paint(SUN, s); }
    public static String soil(String s)  { return paint(SOIL, s); }
    public static String water(String s) { return paint(WATER, s); }
    public static String alert(String s) { return paint(ALERT, s); }
    public static String stone(String s) { return paint(STONE, s); }
    public static String bold(String s)  { return paint(BOLD, s); }
    public static String dim(String s)   { return paint(DIM, s); }

    // ---------------------------------------------------------------- icons

    public static final String SPROUT  = "\\|/";
    public static final String WARNING = "/!\\";
    public static final String SACK    = "[#]";
    public static final String LINK    = "<->";
    public static final String LENS    = "(?)";
    public static final String SCROLL  = "[=]";
    public static final String DROP    = " ~ ";

    // ---------------------------------------------------------------- parts

    /** A row of young plants growing out of the soil. */
    public static String field() {
        StringBuilder plants = new StringBuilder();
        StringBuilder ground = new StringBuilder();
        for (int i = 0; i < 13; i++) plants.append(" \\|/ ");
        for (int i = 0; i < 22; i++) ground.append("^~~");
        return "  " + leaf(plants.substring(0, 64)) + "\n"
             + "  " + soil(ground.substring(0, 64));
    }

    public static String fence() {
        return soil("==================================================================");
    }

    public static String furrow() {
        return crop("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
    }

    public static String banner() {
        return "\n" + fence() + "\n"
             + "     " + sun("\\  |  /") + "\n"
             + "   " + sun("--   O   --") + "     " + bold(leaf("F A R M A S S I S T")) + "\n"
             + "     " + sun("/  |  \\") + "       " + crop("your digital agriculture officer") + "\n"
             + "\n"
             + field() + "\n"
             + fence();
    }

    /** A titled section header, e.g.   \|/  CROP PROFILE  ------------------ */
    public static String header(String icon, String title) {
        StringBuilder dashes = new StringBuilder();
        int width = 58 - title.length();
        for (int i = 0; i < Math.max(3, width); i++) dashes.append('-');
        return "  " + leaf(icon) + " " + bold(sun(title)) + " " + stone(dashes.toString());
    }

    /** The line the bot speaks on. */
    public static String voice() {
        return leaf(SPROUT) + " " + bold(leaf("FarmAssist")) + stone(" > ");
    }

    /** The line the farmer types on. */
    public static String prompt() {
        return "\n" + sun("Farmer") + stone(" > ");
    }

    /**
     * Pad BEFORE colouring. printf counts the invisible escape characters as
     * part of the width, so "%-14s" on a coloured string collapses the column.
     */
    public static String padRight(String s, int width) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }

    public static String padLeft(String s, int width) {
        StringBuilder sb = new StringBuilder();
        for (int i = s.length(); i < width; i++) sb.append(' ');
        return sb + s;
    }

    public static String rule(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append('-');
        return stone(sb.toString());
    }
}
