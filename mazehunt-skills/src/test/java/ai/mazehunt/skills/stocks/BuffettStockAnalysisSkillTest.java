package ai.mazehunt.skills.stocks;

import ai.mazehunt.api.skill.SkillResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BuffettStockAnalysisSkillTest {

    @Test
    void analysesStrongCompany() {
        BuffettStockAnalysisSkill s = new BuffettStockAnalysisSkill();
        Map<String, Object> args = Map.ofEntries(
                Map.entry("ticker", "AAA"),
                Map.entry("roeHistory", List.of(0.18, 0.22, 0.20, 0.19, 0.21)),
                Map.entry("grossMargin", 0.55),
                Map.entry("epsHistory", List.of(1.0, 1.1, 1.2, 1.3, 1.5, 1.7, 1.9, 2.0, 2.2, 2.5)),
                Map.entry("longTermDebt", 10_000_000_000.0),
                Map.entry("currentRatio", 2.1),
                Map.entry("freeCashFlow", 5_000_000_000.0),
                Map.entry("enterpriseValue", 60_000_000_000.0),
                Map.entry("sharePrice", 50.0),
                Map.entry("sharesOutstanding", 1_000_000_000.0),
                Map.entry("discountRate", 0.09),
                Map.entry("growthRate", 0.10),
                Map.entry("terminalGrowth", 0.025));
        SkillResult r = s.invoke("buffett_analyse", args, null);
        assertTrue(r.success());
        Map<?, ?> out = (Map<?, ?>) r.value();
        assertEquals("AAA", out.get("ticker"));
        assertNotNull(out.get("verdict"));
        assertNotNull(out.get("intrinsicPerShare"));
    }

    @Test
    void handlesMissingInputs() {
        BuffettStockAnalysisSkill s = new BuffettStockAnalysisSkill();
        SkillResult r = s.invoke("buffett_analyse", Map.of("ticker", "UNK"), null);
        assertTrue(r.success());
        Map<?, ?> out = (Map<?, ?>) r.value();
        List<?> unknowns = (List<?>) out.get("unknowns");
        assertFalse(unknowns.isEmpty(), "unknowns should be listed, not hallucinated");
    }
}
