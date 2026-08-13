package util;

/**
 * Prints a one line note every time one of the eight algorithms runs.
 * This is what makes the algorithm usage VISIBLE during the demo.
 * Turn it off from the chat with the command:  trace off
 *
 * The lines are printed in a dim grey so they sit behind the real answer
 * instead of competing with it.
 */
public class Trace {

    public static boolean enabled = true;

    /** Set to false by the "color off" command through Theme. */
    public static boolean colours = true;

    private static final String ESC = String.valueOf((char) 27) + "[";
    private static final String GREY = ESC + "90m";
    private static final String RESET = ESC + "0m";

    public static void log(String algorithm, String message) {
        if (!enabled) return;
        String line = "   . " + pad(algorithm) + " : " + message;
        System.out.println(colours ? GREY + line + RESET : line);
    }

    private static String pad(String s) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < 18) sb.append(' ');
        return sb.toString();
    }
}
