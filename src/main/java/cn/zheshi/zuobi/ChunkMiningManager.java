package cn.zheshi.zuobi;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Comparator;

public final class ChunkMiningManager {
    private static final int BLOCK_BUDGET_PER_TICK = 32_768;
    private static final long TIME_BUDGET_NANOS = 5_000_000L;
    private static final int TICKET_LEVEL = 31;
    private static final ChunkTicketType<ChunkPos> TICKET = ChunkTicketType.create(
            ZheshiZuobiMod.MOD_ID + ":chunk_mining", Comparator.comparingLong(ChunkPos::toLong));
    private static final RegistryKey<Enchantment> ENCHANTMENT_KEY = RegistryKey.of(
            RegistryKeys.ENCHANTMENT, Identifier.of(ZheshiZuobiMod.MOD_ID, "chunk_mining"));

    private final ChunkMiningRules rules;
    private final ArrayDeque<ChunkMiningJob> queue = new ArrayDeque<>();
    private final Map<UUID, ChunkMiningJob> byPlayer = new HashMap<>();

    public ChunkMiningManager(ChunkMiningRules rules) {
        this.rules = rules;
    }

    public boolean tryStart(ServerWorld world, ServerPlayerEntity player, BlockPos brokenPos) {
        return tryStart(world, player, brokenPos, false);
    }

    public boolean tryStartFloor(ServerWorld world, ServerPlayerEntity player, BlockPos attackedPos) {
        if (attackedPos.getY() != world.getBottomY()
                || !ClearedFloorState.get(world.getPersistentStateManager()).contains(new ChunkPos(attackedPos))) {
            return false;
        }
        return tryStart(world, player, attackedPos, true);
    }

    private boolean tryStart(ServerWorld world, ServerPlayerEntity player, BlockPos brokenPos, boolean requireFloorOnly) {
        ItemStack tool = player.getMainHandStack();
        if (!player.isSneaking() || !(tool.getItem() instanceof PickaxeItem)) return false;
        var entry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(ENCHANTMENT_KEY).orElse(null);
        if (entry == null) return false;
        int level = EnchantmentHelper.getLevel(entry, tool);
        if (level <= 0) return false;

        if (player.isCreative() && !rules.creativeChunkMining()) {
            player.sendMessage(Text.translatable("message.zheshi_zuobi.creative_disabled"), true);
            return false;
        }
        ChunkMiningJob existing = byPlayer.get(player.getUuid());
        if (existing != null) {
            player.sendMessage(Text.translatable("message.zheshi_zuobi.busy", existing.percent()), true);
            return true;
        }

        ChunkPos center = new ChunkPos(brokenPos);
        int radius = ChunkMiningMath.radiusForLevel(level);
        List<ChunkPos> loaded = snapshotLoaded(world, center, radius);
        boolean floorOnly = brokenPos.getY() == world.getBottomY()
                && ClearedFloorState.get(world.getPersistentStateManager()).contains(center);
        if (requireFloorOnly && !floorOnly) return false;
        if (floorOnly) {
            ClearedFloorState floors = ClearedFloorState.get(world.getPersistentStateManager());
            loaded.removeIf(pos -> !floors.contains(pos));
        }
        if (loaded.isEmpty()) return false;

        ChunkMiningJob job = new ChunkMiningJob(player.getUuid(), world, loaded, floorOnly);
        loaded.forEach(pos -> world.getChunkManager().addTicket(TICKET, pos, TICKET_LEVEL, pos));
        byPlayer.put(player.getUuid(), job);
        queue.add(job);

        if (!player.isCreative()) {
            tool.damage(1, world, player, item -> player.sendEquipmentBreakStatus(item, EquipmentSlot.MAINHAND));
        }
        int diameter = ChunkMiningMath.diameterForLevel(level);
        player.sendMessage(Text.translatable("message.zheshi_zuobi.started", diameter, diameter, loaded.size()), false);
        return true;
    }

    private static List<ChunkPos> snapshotLoaded(ServerWorld world, ChunkPos center, int radius) {
        List<ChunkPos> result = new ArrayList<>();
        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                if (world.getChunkManager().isChunkLoaded(x, z)) result.add(new ChunkPos(x, z));
            }
        }
        return result;
    }

    public void tick() {
        if (queue.isEmpty()) return;
        int budget = BLOCK_BUDGET_PER_TICK;
        long deadline = System.nanoTime() + TIME_BUDGET_NANOS;
        int jobsThisTick = queue.size();
        while (budget > 0 && jobsThisTick-- > 0 && !queue.isEmpty() && System.nanoTime() < deadline) {
            ChunkMiningJob job = queue.removeFirst();
            int fairShare = Math.max(1, budget / (jobsThisTick + 1));
            try {
                budget -= job.process(fairShare, deadline);
                if (job.shouldReportProgress()) job.reportProgress();
                if (job.isComplete()) {
                    finish(job);
                } else {
                    queue.addLast(job);
                }
            } catch (RuntimeException e) {
                ZheshiZuobiMod.LOGGER.error("Chunk Mining job for {} failed", job.owner(), e);
                releaseTickets(job);
                byPlayer.remove(job.owner());
            }
        }
    }

    private void finish(ChunkMiningJob job) {
        job.finish();
        releaseTickets(job);
        byPlayer.remove(job.owner());
    }

    public void shutdown() {
        for (ChunkMiningJob job : queue) releaseTickets(job);
        queue.clear();
        byPlayer.clear();
    }

    private static void releaseTickets(ChunkMiningJob job) {
        for (ChunkPos pos : job.chunks()) {
            job.world().getChunkManager().removeTicket(TICKET, pos, TICKET_LEVEL, pos);
        }
    }
}
