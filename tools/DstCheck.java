import java.time.*;
import java.util.Map;

/**
 * Verifies WatchFace.destOffset(): evaluates each airport's generated offset
 * expression with a small evaluator for the WFF expression grammar, and checks
 * it against java.time's zone rules for every hour from October 2026 to 2040. The
 * [YEAR] source is the wearer's local year, so each hour is checked with the
 * year as seen from UTC-10 and UTC+14 as well.
 *
 * Run from the project root:
 *   javac -d tools/out/cls tools/WatchFace.java tools/DstCheck.java && java -cp tools/out/cls DstCheck
 */
public class DstCheck {
    public static void main(String[] args) {
        int failures = 0;
        Instant from = Instant.parse("2026-10-01T00:00:00Z"), to = Instant.parse("2041-01-01T00:00:00Z");
        for (WatchFace.Airport a : WatchFace.AIRPORTS) {
            String expr = WatchFace.destOffset(a);
            ZoneId zone = ZoneId.of(a.zone());
            int bad = 0;
            for (Instant t = from; t.isBefore(to); t = t.plusSeconds(3600)) {
                long minutes = t.getEpochSecond() / 60;
                int expected = zone.getRules().getOffset(t).getTotalSeconds() / 60;
                for (int wearer : new int[] {0, -600, 840}) {
                    int year = t.atOffset(ZoneOffset.ofTotalSeconds(wearer * 60)).getYear();
                    double got = new Eval(expr, Map.of("MINUTES_SINCE_EPOCH", (double) minutes, "YEAR", (double) year)).run();
                    if (got != expected) {
                        if (bad++ < 3) System.out.printf("  %s %s (wearer year %d): got %s, expected %d%n", a.code(), t, year, got, expected);
                    }
                }
            }
            System.out.printf("%-4s %-30s %-5s %s%n", a.code(), a.zone(), WatchFace.dstRule(a), bad == 0 ? "OK" : bad + " MISMATCHES");
            failures += bad;
        }
        System.out.println(failures == 0 ? "ALL OFFSETS MATCH java.time" : "FAILURES: " + failures);
        if (failures > 0) System.exit(1);
    }

    /** Recursive-descent evaluator for the subset of WFF expressions the generator emits. */
    static class Eval {
        final String s; final Map<String, Double> vars; int i;
        Eval(String s, Map<String, Double> vars) { this.s = s; this.vars = vars; }

        double run() { double v = ternary(); ws(); if (i != s.length()) throw err("trailing input"); return v; }

        double ternary() {
            double c = or();
            if (eat("?")) { double a = ternary(); expect(":"); double b = ternary(); return c != 0 ? a : b; }
            return c;
        }
        double or() { double v = and(); while (eat("||")) { double r = and(); v = (v != 0 || r != 0) ? 1 : 0; } return v; }
        double and() { double v = cmp(); while (eat("&&")) { double r = cmp(); v = (v != 0 && r != 0) ? 1 : 0; } return v; }
        double cmp() {
            double v = add();
            for (String op : new String[] {">=", "<=", "==", "!=", ">", "<"}) {
                if (eat(op)) {
                    double r = add();
                    return switch (op) {
                        case ">=" -> v >= r ? 1 : 0; case "<=" -> v <= r ? 1 : 0;
                        case "==" -> v == r ? 1 : 0; case "!=" -> v != r ? 1 : 0;
                        case ">" -> v > r ? 1 : 0; default -> v < r ? 1 : 0;
                    };
                }
            }
            return v;
        }
        double add() {
            double v = mul();
            while (true) {
                if (eat("+")) v += mul();
                else if (peekMinus()) { i++; v -= mul(); }
                else return v;
            }
        }
        double mul() {
            double v = unary();
            while (true) {
                if (eat("*")) v *= unary();
                else if (eat("/")) { double r = unary(); v = r == 0 ? 0 : v / r; }
                else if (eat("%")) v %= unary();
                else return v;
            }
        }
        double unary() { ws(); if (s.charAt(i) == '-') { i++; return -unary(); } return primary(); }
        double primary() {
            ws();
            char c = s.charAt(i);
            if (c == '(') { i++; double v = ternary(); expect(")"); return v; }
            if (c == '[') { int j = s.indexOf(']', i); String name = s.substring(i + 1, j); i = j + 1;
                Double v = vars.get(name); if (v == null) throw err("unknown source " + name); return v; }
            if (Character.isDigit(c) || c == '.') { int j = i; while (j < s.length() && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) j++;
                double v = Double.parseDouble(s.substring(i, j)); i = j; return v; }
            if (Character.isLetter(c)) { int j = i; while (Character.isLetter(s.charAt(j))) j++; String fn = s.substring(i, j); i = j;
                expect("("); double x = ternary(); expect(")");
                return switch (fn) { case "floor" -> Math.floor(x); case "abs" -> Math.abs(x); default -> throw err("unknown function " + fn); }; }
            throw err("unexpected '" + c + "'");
        }
        boolean peekMinus() { ws(); return i < s.length() && s.charAt(i) == '-'; }
        void ws() { while (i < s.length() && s.charAt(i) == ' ') i++; }
        boolean eat(String t) { ws(); if (s.startsWith(t, i)) { i += t.length(); return true; } return false; }
        void expect(String t) { if (!eat(t)) throw err("expected " + t); }
        RuntimeException err(String m) { return new IllegalStateException(m + " at " + i + " in: " + s); }
    }
}
