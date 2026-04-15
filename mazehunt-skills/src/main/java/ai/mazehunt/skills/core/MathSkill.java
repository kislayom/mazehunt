package ai.mazehunt.skills.core;

import ai.mazehunt.api.model.ToolSpec;
import ai.mazehunt.api.skill.Skill;
import ai.mazehunt.api.skill.SkillContext;
import ai.mazehunt.api.skill.SkillResult;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.List;
import java.util.Map;

/**
 * Numeric helper. Falls back to a hand-rolled shunting-yard evaluator when no
 * JavaScript engine is on the classpath, so math works even on a stripped JDK.
 */
public final class MathSkill implements Skill {

    @Override public String id() { return "math.calc"; }
    @Override public String description() { return "Evaluates arithmetic expressions deterministically."; }

    @Override
    public List<ToolSpec> tools() {
        return List.of(new ToolSpec("calc",
                "Compute an arithmetic expression (e.g. '(1200*1.08^10)/12').",
                Map.of("type", "object",
                        "properties", Map.of("expression", Map.of("type", "string")),
                        "required", List.of("expression"))));
    }

    @Override
    public SkillResult invoke(String tool, Map<String, Object> args, SkillContext ctx) {
        String expr = String.valueOf(args.get("expression"));
        try {
            ScriptEngine js = new ScriptEngineManager().getEngineByName("javascript");
            if (js != null) {
                Object r = js.eval("Math.round(1e6*(" + expr + "))/1e6");
                return SkillResult.ok(r);
            }
            return SkillResult.ok(ShuntingYard.eval(expr));
        } catch (Exception e) {
            return SkillResult.error("calc failed: " + e.getMessage());
        }
    }

    /** Tiny shunting-yard evaluator: + - * / % ^ and parentheses. */
    static final class ShuntingYard {
        static double eval(String s) {
            return new ShuntingYard(s).parse();
        }
        private final String s;
        private int pos = -1, ch;
        ShuntingYard(String s) { this.s = s; }
        double parse() {
            next();
            double x = parseExpr();
            if (pos < s.length()) throw new IllegalArgumentException("unexpected: " + (char) ch);
            return x;
        }
        void next() { ch = ++pos < s.length() ? s.charAt(pos) : -1; }
        boolean eat(int c) {
            while (ch == ' ') next();
            if (ch == c) { next(); return true; }
            return false;
        }
        double parseExpr() {
            double x = parseTerm();
            while (true) {
                if (eat('+')) x += parseTerm();
                else if (eat('-')) x -= parseTerm();
                else return x;
            }
        }
        double parseTerm() {
            double x = parseFactor();
            while (true) {
                if (eat('*')) x *= parseFactor();
                else if (eat('/')) x /= parseFactor();
                else if (eat('%')) x %= parseFactor();
                else return x;
            }
        }
        double parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) return -parseFactor();
            double x;
            int start = pos;
            if (eat('(')) { x = parseExpr(); eat(')'); }
            else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') next();
                x = Double.parseDouble(s.substring(start, pos));
            } else throw new IllegalArgumentException("unexpected: " + (char) ch);
            if (eat('^')) x = Math.pow(x, parseFactor());
            return x;
        }
    }
}
