package dev.behindthescenery.botumi.fabric;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.behindthescenery.botumi.DomeParticles;
import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.fabric.registry.BotumiFabricRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.RegistryPredicateArgumentType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public final class BotumiFabric implements ModInitializer {
    private static final DynamicCommandExceptionType STRUCTURE_INVALID_EXCEPTION = new DynamicCommandExceptionType((id) -> Text.stringifiedTranslatable("commands.locate.structure.invalid", id));

    @Override
    public void onInitialize() {
        Botumi.init();
        BotumiFabricRegistry.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("botumi")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("setStructure")
                                .then(CommandManager.argument("id", RegistryPredicateArgumentType.registryPredicate(RegistryKeys.STRUCTURE))
                                        .executes(c -> {
                                            String id = RegistryPredicateArgumentType.getPredicate(c, "id", RegistryKeys.STRUCTURE, STRUCTURE_INVALID_EXCEPTION).asString();
                                            Botumi.setTestStructureId(id);
                                            c.getSource().sendFeedback(() -> Text.literal("Структура установлена: " + Botumi.getTestStructureId()), true);
                                            return 1;
                                        })
                                )
                        )
                        .then(CommandManager.literal("setDomeOnClosestStructure")
                                .executes(c -> {
                                    try {
                                    ServerPlayerEntity player = c.getSource().getPlayerOrThrow();
                                    ServerWorld world = player.getServerWorld();
                                    BlockPos structurepos = Objects.requireNonNull(world.getChunkManager().getChunkGenerator().locateStructure(world, RegistryEntryList.of(RegistryEntry.of(world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(Botumi.getTestStructureId()))), player.getBlockPos(), 100, false)).getFirst();

                                    if (structurepos == null) {
                                        c.getSource().sendError(Text.literal("Структура не найдена в радиусе 100 блоков"));
                                        return 0;
                                    }
                                    double[] size = Botumi.getStructureSize(world, structurepos);
                                    if (size[0] == 0 && size[1] == 0 && size[2] == 0 && size[3] == 0 && size[4] == 0 && size[5] == 0) {
                                        c.getSource().sendError(Text.literal("Не удалось получить размер структуры. Убедитесь, что структура загружена и существует."));
                                        return 0;
                                    }
                                    double radius = Math.max(Math.max(size[0], size[1]), Math.max(size[2], size[3]));
                                    radius = Math.max(radius, 8);
                                    int spawned = DomeParticles.spawnDome(world, structurepos, radius, Math.max(6, 360.0 / (radius * 4.0)));
                                    c.getSource().sendFeedback(() -> Text.literal("Купол частиц создан над структурой. Частиц: " + spawned), false);
                                    return spawned;
                                    } catch (Exception e) {
                                        c.getSource().sendError(Text.literal("Ошибка при выполнении команды: " + e.getMessage()));
                                        System.out.println(e.getMessage());
                                        return 0;
                                    }
                                })

                        )
                        .then(CommandManager.literal("dome")
                                .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 128))
                                        .executes(c -> {
                                            int radius = IntegerArgumentType.getInteger(c, "radius");
                                            ServerPlayerEntity player = c.getSource().getPlayerOrThrow();
                                            ServerWorld world = player.getServerWorld();
                                            int spawned = DomeParticles.spawnDome(world, player.getBlockPos(), radius, Math.max(6, 360.0 / (radius * 4.0)));
                                            c.getSource().sendFeedback(() -> Text.literal("Купол частиц создан. Частиц: " + spawned), false);
                                            return spawned;
                                        })
                                )
                        )
        ));
    }
}
