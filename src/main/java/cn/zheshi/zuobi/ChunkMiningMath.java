package cn.zheshi.zuobi;

public final class ChunkMiningMath {
    private ChunkMiningMath() {}

    public static int clampLevel(int level) {
        return Math.max(1, Math.min(6, level));
    }

    public static int radiusForLevel(int level) {
        return clampLevel(level) - 1;
    }

    public static int diameterForLevel(int level) {
        return radiusForLevel(level) * 2 + 1;
    }

    public static int firstY(int bottomY, boolean preserveFloor, boolean floorOnly) {
        return preserveFloor && !floorOnly ? bottomY + 1 : bottomY;
    }
}
