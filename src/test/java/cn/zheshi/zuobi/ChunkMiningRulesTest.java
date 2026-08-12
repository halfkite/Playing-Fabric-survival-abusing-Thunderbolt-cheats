package cn.zheshi.zuobi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkMiningRulesTest {
    @Test
    void parsesStrictBooleanValues() {
        assertTrue(ChunkMiningRules.parseBoolean("TRUE"));
        assertFalse(ChunkMiningRules.parseBoolean(" false "));
        assertThrows(IllegalArgumentException.class, () -> ChunkMiningRules.parseBoolean("yes"));
    }
}

