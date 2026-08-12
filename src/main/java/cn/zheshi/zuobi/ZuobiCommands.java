package cn.zheshi.zuobi;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.IOException;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

final class ZuobiCommands {
    private ZuobiCommands() {}

    static void register(CommandDispatcher<ServerCommandSource> dispatcher, ChunkMiningRules rules) {
        dispatcher.register(literal("zuobi")
                .executes(ctx -> show(ctx.getSource(), rules))
                .then(literal("list").executes(ctx -> show(ctx.getSource(), rules)))
                .then(literal(ChunkMiningRules.RULE_NAME)
                        .executes(ctx -> show(ctx.getSource(), rules))
                        .then(argument("value", BoolArgumentType.bool())
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(ctx -> {
                                    boolean value = BoolArgumentType.getBool(ctx, "value");
                                    rules.setTemporary(value);
                                    ctx.getSource().sendFeedback(() -> Text.translatable("commands.zuobi.changed", value), true);
                                    return 1;
                                })))
                .then(literal("setDefault").requires(source -> source.hasPermissionLevel(2))
                        .then(literal(ChunkMiningRules.RULE_NAME)
                                .then(argument("value", BoolArgumentType.bool()).executes(ctx -> {
                                    boolean value = BoolArgumentType.getBool(ctx, "value");
                                    try {
                                        rules.setDefault(ctx.getSource().getServer(), value);
                                        ctx.getSource().sendFeedback(() -> Text.translatable("commands.zuobi.default_set", value), true);
                                        return 1;
                                    } catch (IOException e) {
                                        ZheshiZuobiMod.LOGGER.error("Unable to write zuobi.conf", e);
                                        ctx.getSource().sendError(Text.literal(e.getMessage()));
                                        return 0;
                                    }
                                }))))
                .then(literal("removeDefault").requires(source -> source.hasPermissionLevel(2))
                        .then(literal(ChunkMiningRules.RULE_NAME).executes(ctx -> {
                            try {
                                rules.removeDefault(ctx.getSource().getServer());
                                ctx.getSource().sendFeedback(() -> Text.translatable("commands.zuobi.default_removed"), true);
                                return 1;
                            } catch (IOException e) {
                                ZheshiZuobiMod.LOGGER.error("Unable to remove zuobi.conf", e);
                                ctx.getSource().sendError(Text.literal(e.getMessage()));
                                return 0;
                            }
                        }))));
    }

    private static int show(ServerCommandSource source, ChunkMiningRules rules) {
        source.sendFeedback(() -> Text.translatable("commands.zuobi.rule", rules.creativeChunkMining()), false);
        source.sendFeedback(() -> Text.translatable("commands.zuobi.rule.description"), false);
        return 1;
    }
}

