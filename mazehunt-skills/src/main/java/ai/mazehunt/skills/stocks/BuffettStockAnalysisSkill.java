package ai.mazehunt.skills.stocks;

import ai.mazehunt.api.model.ToolSpec;
import ai.mazehunt.api.skill.Skill;
import ai.mazehunt.api.skill.SkillContext;
import ai.mazehunt.api.skill.SkillResult;

import java.util.*;

/**
 * Value-investing evaluator inspired by Warren Buffett's stated heuristics.
 *
 * <p>The skill computes a bounded set of deterministic scores from user-supplied
 * fundamentals — it does <strong>not</strong> invent numbers. If an input is
 * missing, the corresponding dimension's result is marked {@code UNKNOWN}
 * rather than hallucinated. Numbers come from the caller (a finance API tool,
 * an SEC filing skill, or the user), never from the language model.
 *
 * <h3>Heuristics evaluated</h3>
 * <ul>
 *   <li><b>Economic moat</b> — ROE ≥ 15% sustained; gross margin ≥ 40%.</li>
 *   <li><b>Earnings stability</b> — 10-yr EPS CAGR &gt; 7% with no loss year.</li>
 *   <li><b>Manageable debt</b> — long-term debt ≤ 5× average annual earnings.</li>
 *   <li><b>Conservative balance sheet</b> — current ratio ≥ 1.5.</li>
 *   <li><b>Owner earnings yield</b> — FCF / EV (≥ 6% preferred).</li>
 *   <li><b>Margin of safety</b> — intrinsic value (DCF) vs. current price.</li>
 * </ul>
 *
 * <p>Output is a structured {@link Map} suitable for feeding to a REDUCE step or
 * rendering as a table.
 */
public final class BuffettStockAnalysisSkill implements Skill {

    @Override public String id() { return "stocks.buffett"; }
    @Override public String description() {
        return "Value-investing analysis (moat, debt, earnings stability, margin of safety).";
    }

    @Override
    public List<ToolSpec> tools() {
        return List.of(
                new ToolSpec("buffett_analyse",
                        "Run a Buffett-style checklist on a company's fundamentals.",
                        Map.of("type", "object",
                                "properties", Map.ofEntries(
                                        Map.entry("ticker",              Map.of("type", "string")),
                                        Map.entry("roeHistory",          Map.of("type", "array", "items", Map.of("type", "number"))),
                                        Map.entry("grossMargin",         Map.of("type", "number")),
                                        Map.entry("epsHistory",          Map.of("type", "array", "items", Map.of("type", "number"))),
                                        Map.entry("longTermDebt",        Map.of("type", "number")),
                                        Map.entry("currentRatio",        Map.of("type", "number")),
                                        Map.entry("freeCashFlow",        Map.of("type", "number")),
                                        Map.entry("enterpriseValue",     Map.of("type", "number")),
                                        Map.entry("sharePrice",          Map.of("type", "number")),
                                        Map.entry("sharesOutstanding",   Map.of("type", "number")),
                                        Map.entry("discountRate",        Map.of("type", "number")),
                                        Map.entry("growthRate",          Map.of("type", "number")),
                                        Map.entry("terminalGrowth",      Map.of("type", "number"))),
                                "required", List.of("ticker"))),
                new ToolSpec("dcf_intrinsic_value",
                        "Two-stage DCF; returns intrinsic value per share.",
                        Map.of("type", "object",
                                "properties", Map.ofEntries(
                                        Map.entry("freeCashFlow",      Map.of("type", "number")),
                                        Map.entry("growthRate",        Map.of("type", "number")),
                                        Map.entry("terminalGrowth",    Map.of("type", "number")),
                                        Map.entry("discountRate",      Map.of("type", "number")),
                                        Map.entry("sharesOutstanding", Map.of("type", "number")),
                                        Map.entry("years",             Map.of("type", "integer"))),
                                "required", List.of("freeCashFlow", "discountRate", "sharesOutstanding"))));
    }

    @Override
    public SkillResult invoke(String tool, Map<String, Object> args, SkillContext ctx) {
        return switch (tool) {
            case "buffett_analyse"     -> analyse(args);
            case "dcf_intrinsic_value" -> SkillResult.ok(Map.of(
                    "intrinsicPerShare", dcf(args),
                    "assumptions", args));
            default -> SkillResult.error("unknown tool: " + tool);
        };
    }

    private static SkillResult analyse(Map<String, Object> a) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ticker", a.get("ticker"));
        List<String> strengths = new ArrayList<>();
        List<String> concerns = new ArrayList<>();
        List<String> unknowns = new ArrayList<>();

        // 1. Economic moat — ROE history + gross margin
        double roeAvg = avg(toDoubles(a.get("roeHistory")));
        Double gm = asDouble(a.get("grossMargin"));
        Map<String, Object> moat = new LinkedHashMap<>();
        moat.put("roeAverage", Double.isNaN(roeAvg) ? null : roeAvg);
        moat.put("grossMargin", gm);
        if (!Double.isNaN(roeAvg)) {
            if (roeAvg >= 0.15) strengths.add("Sustained ROE ≥15% (avg=" + pct(roeAvg) + ")");
            else concerns.add("ROE below Buffett threshold (avg=" + pct(roeAvg) + ")");
        } else unknowns.add("roeHistory");
        if (gm != null) {
            if (gm >= 0.40) strengths.add("Strong gross margin (" + pct(gm) + ")");
            else concerns.add("Gross margin thin (" + pct(gm) + ")");
        } else unknowns.add("grossMargin");
        result.put("moat", moat);

        // 2. Earnings stability
        double[] eps = toDoubles(a.get("epsHistory"));
        if (eps.length >= 3) {
            boolean anyNeg = false;
            for (double v : eps) if (v < 0) { anyNeg = true; break; }
            double cagr = cagr(eps);
            if (anyNeg) concerns.add("Loss-making year in EPS history");
            if (cagr > 0.07) strengths.add("EPS CAGR " + pct(cagr));
            else if (!Double.isNaN(cagr)) concerns.add("Slow EPS growth (" + pct(cagr) + ")");
            result.put("epsCAGR", cagr);
            result.put("epsLossYears", anyNeg);
        } else unknowns.add("epsHistory");

        // 3. Debt
        Double ltDebt = asDouble(a.get("longTermDebt"));
        if (ltDebt != null && eps.length > 0) {
            Double shares = asDouble(a.get("sharesOutstanding"));
            double avgEarnings = avg(eps) * (shares == null ? 1 : shares);
            if (avgEarnings > 0) {
                double ratio = ltDebt / avgEarnings;
                result.put("debtToAvgEarnings", ratio);
                if (ratio <= 5) strengths.add("LT-debt ≤ 5× avg earnings (" + round(ratio) + "×)");
                else concerns.add("LT-debt > 5× avg earnings (" + round(ratio) + "×)");
            }
        } else if (ltDebt == null) unknowns.add("longTermDebt");

        // 4. Liquidity
        Double cr = asDouble(a.get("currentRatio"));
        if (cr != null) {
            result.put("currentRatio", cr);
            if (cr >= 1.5) strengths.add("Current ratio ≥ 1.5 (" + round(cr) + ")");
            else concerns.add("Current ratio below 1.5 (" + round(cr) + ")");
        } else unknowns.add("currentRatio");

        // 5. Owner-earnings yield
        Double fcf = asDouble(a.get("freeCashFlow"));
        Double ev = asDouble(a.get("enterpriseValue"));
        if (fcf != null && ev != null && ev > 0) {
            double yield = fcf / ev;
            result.put("ownerEarningsYield", yield);
            if (yield >= 0.06) strengths.add("FCF/EV ≥ 6% (" + pct(yield) + ")");
            else concerns.add("Low FCF/EV yield (" + pct(yield) + ")");
        } else {
            if (fcf == null) unknowns.add("freeCashFlow");
            if (ev == null) unknowns.add("enterpriseValue");
        }

        // 6. Margin of safety (DCF)
        double intrinsic = dcf(a);
        Double price = asDouble(a.get("sharePrice"));
        if (!Double.isNaN(intrinsic) && price != null) {
            double mos = (intrinsic - price) / intrinsic;
            result.put("intrinsicPerShare", intrinsic);
            result.put("marginOfSafety", mos);
            if (mos >= 0.30) strengths.add("Margin of safety ≥ 30% (" + pct(mos) + ")");
            else if (mos >= 0) concerns.add("Margin of safety thin (" + pct(mos) + ")");
            else concerns.add("Price exceeds intrinsic value (" + pct(mos) + ")");
        } else if (Double.isNaN(intrinsic)) unknowns.add("DCF inputs");

        int score = strengths.size() - concerns.size();
        String verdict = score >= 3 ? "STRONG_BUY_CANDIDATE"
                : score >= 1 ? "WATCHLIST"
                : score <= -2 ? "AVOID" : "NEUTRAL";

        result.put("strengths", strengths);
        result.put("concerns",  concerns);
        result.put("unknowns",  unknowns);
        result.put("verdict",   verdict);
        return SkillResult.ok(result,
                unknowns.isEmpty() ? List.of() : List.of("caller-supplied fundamentals"));
    }

    /**
     * Two-stage DCF: {@code years} years of explicit growth, then a perpetuity
     * at {@code terminalGrowth}. Defaults are intentionally conservative.
     */
    private static double dcf(Map<String, Object> a) {
        Double fcf = asDouble(a.get("freeCashFlow"));
        Double shares = asDouble(a.get("sharesOutstanding"));
        Double disc = asDouble(a.get("discountRate"));
        if (fcf == null || shares == null || disc == null || shares == 0) return Double.NaN;
        double g  = asDoubleOr(a.get("growthRate"),     0.08);
        double tg = asDoubleOr(a.get("terminalGrowth"), 0.025);
        int years = a.get("years") instanceof Number n ? n.intValue() : 10;
        if (disc <= tg) return Double.NaN;

        double npv = 0;
        double cf = fcf;
        for (int y = 1; y <= years; y++) {
            cf *= (1 + g);
            npv += cf / Math.pow(1 + disc, y);
        }
        double terminal = (cf * (1 + tg)) / (disc - tg);
        npv += terminal / Math.pow(1 + disc, years);
        return npv / shares;
    }

    // ---- numeric helpers ----

    private static double[] toDoubles(Object raw) {
        if (raw instanceof List<?> list) {
            double[] v = new double[list.size()];
            for (int i = 0; i < list.size(); i++) v[i] = ((Number) list.get(i)).doubleValue();
            return v;
        }
        return new double[0];
    }
    private static Double asDouble(Object o) { return o instanceof Number n ? n.doubleValue() : null; }
    private static double asDoubleOr(Object o, double d) { Double v = asDouble(o); return v == null ? d : v; }
    private static double avg(double[] a) { if (a.length == 0) return Double.NaN; double s = 0; for (double d : a) s += d; return s / a.length; }
    private static double cagr(double[] a) {
        if (a.length < 2 || a[0] <= 0 || a[a.length - 1] <= 0) return Double.NaN;
        return Math.pow(a[a.length - 1] / a[0], 1.0 / (a.length - 1)) - 1.0;
    }
    private static String pct(double d) { return String.format(Locale.ROOT, "%.1f%%", d * 100); }
    private static String round(double d) { return String.format(Locale.ROOT, "%.2f", d); }
}
