package cn.zheshi.zuobi;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.util.ActionResult;

public final class ZheshiZuobiMod implements ModInitializer {
    public static final String MOD_ID = "zheshi_zuobi";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final ChunkMiningRules RULES = new ChunkMiningRules();
    private static final ChunkMiningManager MINING = new ChunkMiningManager(RULES);

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ZuobiCommands.register(dispatcher, RULES));
        ServerLifecycleEvents.SERVER_STARTED.register(RULES::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> MINING.shutdown());
        ServerTickEvents.END_SERVER_TICK.register(server -> MINING.tick());
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) return true;
            return !MINING.tryStart(serverWorld, serverPlayer, pos);
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }
            return MINING.tryStartFloor(serverWorld, serverPlayer, pos) ? ActionResult.SUCCESS : ActionResult.PASS;
        });
        LOGGER.info("这是作弊 initialized");
    }
}
