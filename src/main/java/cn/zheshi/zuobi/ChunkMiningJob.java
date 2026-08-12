package cn.zheshi.zuobi;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

final class ChunkMiningJob {
    private final UUID owner;
    private final ServerWorld world;
    private final List<ChunkPos> chunks;
    private final boolean floorOnly;
    private final int minY;
    private final int maxYExclusive;
    private final long totalPositions;
    private int chunkIndex;
    private int localX;
    private int localZ;
    private int y;
    private long visited;
    private int lastReportedPercent = -1;

    ChunkMiningJob(UUID owner, ServerWorld world, List<ChunkPos> chunks, boolean floorOnly) {
        this.owner = owner;
        this.world = world;
        this.chunks = List.copyOf(chunks);
        this.floorOnly = floorOnly;
        boolean preserveFloor = world.getRegistryKey() == World.OVERWORLD || world.getRegistryKey() == World.NETHER;
        this.minY = ChunkMiningMath.firstY(world.getBottomY(), preserveFloor, floorOnly);
        this.maxYExclusive = floorOnly ? minY + 1 : world.getTopY();
        this.y = minY;
        this.totalPositions = (long) chunks.size() * 16 * 16 * (maxYExclusive - minY);
    }

    UUID owner() { return owner; }
    ServerWorld world() { return world; }
    List<ChunkPos> chunks() { return chunks; }
    int chunkCount() { return chunks.size(); }
    long visited() { return visited; }
    long totalPositions() { return totalPositions; }
    int percent() { return totalPositions == 0 ? 100 : (int) Math.min(100, visited * 100 / totalPositions); }
    boolean shouldReportProgress() {
        int percent = percent();
        if (percent >= lastReportedPercent + 5 && percent < 100) {
            lastReportedPercent = percent;
            return true;
        }
        return false;
    }

    int process(int allowance, long deadlineNanos) {
        int processed = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        while (chunkIndex < chunks.size() && processed < allowance && System.nanoTime() < deadlineNanos) {
            ChunkPos chunk = chunks.get(chunkIndex);
            pos.set(chunk.getStartX() + localX, y, chunk.getStartZ() + localZ);
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS);
            processed++;
            visited++;
            advanceCursor();
        }
        return processed;
    }

    private void advanceCursor() {
        y++;
        if (y < maxYExclusive) return;
        y = minY;
        localX++;
        if (localX < 16) return;
        localX = 0;
        localZ++;
        if (localZ < 16) return;
        localZ = 0;
        recordCompletedChunk(chunks.get(chunkIndex));
        chunkIndex++;
    }

    private void recordCompletedChunk(ChunkPos chunk) {
        ClearedFloorState floors = ClearedFloorState.get(world.getPersistentStateManager());
        if (floorOnly) {
            floors.remove(chunk);
        } else if (world.getRegistryKey() == World.OVERWORLD || world.getRegistryKey() == World.NETHER) {
            floors.add(chunk);
        }
    }

    boolean isComplete() { return chunkIndex >= chunks.size(); }

    void reportProgress() {
        var player = world.getServer().getPlayerManager().getPlayer(owner);
        if (player != null) player.sendMessage(Text.translatable("message.zheshi_zuobi.progress", percent()), true);
    }

    void finish() {
        var player = world.getServer().getPlayerManager().getPlayer(owner);
        if (player != null) player.sendMessage(Text.translatable("message.zheshi_zuobi.finished", chunks.size()), false);
    }
}
