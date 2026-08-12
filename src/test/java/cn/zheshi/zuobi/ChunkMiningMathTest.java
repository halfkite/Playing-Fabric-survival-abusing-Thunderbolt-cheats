package cn.zheshi.zuobi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkMiningMathTest {
    @Test
    void levelsMapToOddChunkDiameters() {
        for (int level = 1; level <= 6; level++) {
            assertEquals(level - 1, ChunkMiningMath.radiusForLevel(level));
            assertEquals(level * 2 - 1, ChunkMiningMath.diameterForLevel(level));
        }
    }

    @Test
    void outOfRangeLevelsAreClamped() {
        assertEquals(1, ChunkMiningMath.clampLevel(-5));
        assertEquals(6, ChunkMiningMath.clampLevel(99));
    }

    @Test
    void dimensionFloorPolicyChoosesCorrectFirstY() {
        assertEquals(-63, ChunkMiningMath.firstY(-64, true, false));
        assertEquals(-64, ChunkMiningMath.firstY(-64, true, true));
        assertEquals(0, ChunkMiningMath.firstY(0, false, false));
    }
}
