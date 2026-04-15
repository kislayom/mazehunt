package ai.mazehunt.core.util;

import java.util.List;
import java.util.Locale;

/**
 * Numeric coercion and summary helpers. Lives in core.util so any skill / tool
 * needing them can reuse rather than re-implement.
 */
public final class Numbers {

    private Numbers() {}

    /** {@code null}-safe {@link Number} → {@link Double}. Returns {@code null} for non-numbers. */
    public static Double asDouble(Object o) {
        return o instanceof Number n ? n.doubleValue() : null;
    }

    /** {@link #asDouble(Object)} with a fallback default. */
    public static double asDoubleOr(Object o, double fallback) {
        Double v = asDouble(o);
        return v == null ? fallback : v;
    }

    /** Coerce a {@link List} of {@link Number} (or JSON-decoded numbers) to a primitive array. */
    public static double[] toDoubles(Object raw) {
        if (!(raw instanceof List<?> list)) return new double[0];
        double[] v = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);
            if (!(o instanceof Number n)) return new double[0];
            v[i] = n.doubleValue();
        }
        return v;
    }

    public static double avg(double[] a) {
        if (a == null || a.length == 0) return Double.NaN;
        double s = 0;
        for (double d : a) s += d;
        return s / a.length;
    }

    /** Compound annual growth rate over an array whose first/last values bracket the period. */
    public static double cagr(double[] a) {
        if (a == null || a.length < 2 || a[0] <= 0 || a[a.length - 1] <= 0) return Double.NaN;
        return Math.pow(a[a.length - 1] / a[0], 1.0 / (a.length - 1)) - 1.0;
    }

    public static String pct(double d) { return String.format(Locale.ROOT, "%.1f%%", d * 100); }
    public static String round2(double d) { return String.format(Locale.ROOT, "%.2f", d); }
}
