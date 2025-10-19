package dev.behindthescenery.botumi.minigames;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import dev.behindthescenery.botumi.blocks.entity.MinigamesBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public final class MinigameActions {

    private MinigameActions() {
    }

    public static int execute(MinigamesBlockEntity be, PlayerEntity player) {
        if (!(be.getWorld() instanceof ServerWorld sw)) return 0;

        NbtCompound action = resolveAction(sw, be);
        if (action == null || action.isEmpty()) return 0;

        String type = action.getString("type");
        NbtCompound data = action.getCompound("data");

        return switch (type) {
            case "set_block" -> executeSetBlock(sw, be.getPos(), data);
            case "replace_blocks" -> executeReplaceBlocks(sw, be.getPos(), data);
            case "give_reward_item" -> executeGiveRewardItem(sw, player, data);
            case "give_reward_loot" -> executeGiveRewardLoot(sw, player, data);
            case "record_player" -> executeRecordPlayer(be, player);
            case "run_command" -> executeRunCommand(sw, be, player, data);
            case "run_function" -> executeRunFunction(sw, be, player, data);
            default -> 0;
        };
    }

    private static NbtCompound resolveAction(ServerWorld sw, MinigamesBlockEntity be) {
        NbtCompound action = be.getAction();
        if (action != null && !action.isEmpty()) return action;

        try {
            NbtCompound beNbt = be.createNbt(sw.getRegistryManager());
            if (beNbt == null) return action;
            NbtCompound comps = beNbt.getCompound("components");
            if (comps == null || comps.isEmpty()) return action;
            if (!comps.contains("minecraft:custom_data")) return action;
            NbtCompound custom = comps.getCompound("minecraft:custom_data");
            if (custom == null || custom.isEmpty()) return action;
            if (custom.contains("Action")) {
                return custom.getCompound("Action");
            }
        } catch (Exception ignored) {
        }
        return action;
    }

    private static int executeSetBlock(ServerWorld sw, BlockPos origin, NbtCompound data) {
        boolean relative = !data.contains("relative") || data.getBoolean("relative");
        BlockPos target = readPos(data, origin, relative);

        String blockSpec = data.contains("blockString") ? data.getString("blockString") : "";
        if (blockSpec.isEmpty() && data.contains("block") && data.contains("nbt")) {
            String id = data.getString("block");
            String nbt = data.getString("nbt");
            if (id != null && !id.isEmpty() && nbt != null && !nbt.isEmpty()) {
                blockSpec = id + (nbt.startsWith("[") || nbt.startsWith("{") ? nbt : "[" + nbt + "]");
            }
        }

        if (blockSpec != null && !blockSpec.isEmpty()) {
            ParsedBlock parsed = parseBlockLikeSetblock(sw, blockSpec);
            if (parsed != null) {
                sw.setBlockState(target, parsed.state(), Block.NOTIFY_ALL);
                if (parsed.beNbt() != null) {
                    applyBeNbt(sw, target, parsed.beNbt());
                }
                return 2;
            }
        }

        String blockId = data.getString("block");
        if (blockId == null || blockId.isEmpty()) return 0;

        Block block = Registries.BLOCK.get(Identifier.of(blockId));
        BlockState state = block.getDefaultState();
        if (data.contains("state")) {
            state = applyStateFromTag(state, data.getCompound("state"));
        }
        sw.setBlockState(target, state, Block.NOTIFY_ALL);
        maybeApplyBlockEntity(sw, target, data);
        return 2;
    }

    private static int executeReplaceBlocks(ServerWorld sw, BlockPos origin, NbtCompound data) {
        boolean relative = !data.contains("relative") || data.getBoolean("relative");
        BlockPos center = readPos(data, origin, relative);
        int radius = Math.max(0, data.getInt("radius"));

        boolean hasMatch = data.contains("matchBlock") && !data.getString("matchBlock").isEmpty();
        Block matchBlock = null;
        NbtCompound matchStateTag = null;
        if (hasMatch) {
            String matchId = data.getString("matchBlock");
            matchBlock = Registries.BLOCK.get(Identifier.of(matchId));
            matchStateTag = data.contains("matchState") ? data.getCompound("matchState") : null;
        }

        String setSpec = data.contains("setBlockString") ? data.getString("setBlockString") : "";
        if (setSpec.isEmpty() && data.contains("setBlock") && data.contains("setNbt")) {
            String id = data.getString("setBlock");
            String nbt = data.getString("setNbt");
            if (id != null && !id.isEmpty() && nbt != null && !nbt.isEmpty()) {
                setSpec = id + (nbt.startsWith("[") || nbt.startsWith("{") ? nbt : "[" + nbt + "]");
            }
        }

        ParsedBlock setParsed = null;
        if (setSpec != null && !setSpec.isEmpty()) {
            setParsed = parseBlockLikeSetblock(sw, setSpec);
            if (setParsed == null) return 0;
        }

        BlockState setState = null;
        if (setParsed == null) {
            String setId = data.getString("setBlock");
            if (setId == null || setId.isEmpty()) return 0;
            Block setBlock = Registries.BLOCK.get(Identifier.of(setId));
            setState = setBlock.getDefaultState();
            if (data.contains("setState")) {
                setState = applyStateFromTag(setState, data.getCompound("setState"));
            }
        }

        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (hasMatch) {
                        BlockState curr = sw.getBlockState(mutable);
                        if (!stateMatches(curr, matchBlock, matchStateTag)) continue;
                    }
                    if (setParsed != null) {
                        sw.setBlockState(mutable, setParsed.state(), Block.NOTIFY_ALL);
                        if (setParsed.beNbt() != null) applyBeNbt(sw, mutable, setParsed.beNbt());
                    } else {
                        sw.setBlockState(mutable, setState, Block.NOTIFY_ALL);
                    }
                }
            }
        }
        return 2;
    }

    private static int executeGiveRewardItem(ServerWorld sw, PlayerEntity player, NbtCompound data) {
        int count = Math.max(1, data.getInt("count"));

        String itemString = data.contains("itemString") ? data.getString("itemString") : "";
        if (itemString.isEmpty() && data.contains("item")) {
            String id = data.getString("item");
            String comps = data.contains("components") ? data.getString("components") : "";
            if (comps != null && !comps.isEmpty()) {
                if (!comps.startsWith("[")) comps = "[" + comps + "]";
                itemString = id + comps;
            }
        }

        ItemStack stack = null;
        if (itemString != null && !itemString.isEmpty()) {
            stack = parseLikeGive(sw.getRegistryManager(), itemString, count);
        }
        if (stack == null) {
            String itemId = data.getString("item");
            if (itemId == null || itemId.isEmpty()) return 0;
            Item item = Registries.ITEM.get(Identifier.of(itemId));
            stack = new ItemStack(item, count);
        }

        boolean inserted = player.getInventory().insertStack(stack.copy());
        if (!inserted) {
            player.dropItem(stack, false);
        }
        return 1;
    }

    private static ItemStack parseLikeGive(RegistryWrapper.WrapperLookup lookup, String spec, int count) {
        try {
            CommandRegistryAccess access = CommandRegistryAccess.of(lookup, FeatureFlags.VANILLA_FEATURES);
            ItemStackArgumentType type = ItemStackArgumentType.itemStack(access);
            ItemStackArgument parsed = type.parse(new StringReader(spec));
            return parsed.createStack(count, false);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ParsedBlock parseBlockLikeSetblock(ServerWorld sw, String spec) {
        try {
            CommandRegistryAccess access = CommandRegistryAccess.of(sw.getRegistryManager(), FeatureFlags.VANILLA_FEATURES);
            BlockStateArgumentType type = BlockStateArgumentType.blockState(access);
            BlockStateArgument arg = type.parse(new StringReader(spec));
            BlockState state = arg.getBlockState();
            NbtCompound beNbt = null;
            int brace = spec.indexOf('{');
            if (brace >= 0) {
                String nbtStr = spec.substring(brace);
                beNbt = StringNbtReader.parse(nbtStr);
            }
            return new ParsedBlock(state, beNbt);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void applyBeNbt(ServerWorld sw, BlockPos pos, NbtCompound nbt) {
        BlockEntity be = sw.getBlockEntity(pos);
        if (be == null || nbt == null || nbt.isEmpty()) return;
        be.read(nbt.copy(), sw.getRegistryManager());
        be.markDirty();
    }

    private static int executeGiveRewardLoot(ServerWorld sw, PlayerEntity player, NbtCompound data) {
        String lootId = data.getString("lootable");
        if (lootId == null || lootId.isEmpty()) return 0;

        Identifier id = Identifier.of(lootId);
        RegistryKey<LootTable> key = RegistryKey.of(RegistryKeys.LOOT_TABLE, id);

        LootTable table = sw.getServer().getReloadableRegistries().getLootTable(key);
        if (table == null) return 0;

        int rolls = Math.max(1, data.getInt("rolls"));

        LootContextParameterSet ctx = new LootContextParameterSet.Builder(sw)
                .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(player.getBlockPos()))
                .add(LootContextParameters.THIS_ENTITY, player)
                .build(LootContextTypes.CHEST);

        for (int i = 0; i < rolls; i++) {
            table.generateLoot(ctx).forEach(stack -> {
                ItemStack copy = stack.copy();
                if (!player.getInventory().insertStack(copy)) {
                    player.dropItem(copy, false);
                }
            });
        }
        return 1;
    }

    private static int executeRecordPlayer(MinigamesBlockEntity be, PlayerEntity player) {
        be.setCompletedBy(player.getUuid());
        return 3;
    }

    private static int executeRunCommand(ServerWorld sw, MinigamesBlockEntity be, PlayerEntity player, NbtCompound data) {
        String cmd = data.getString("command");
        if (cmd == null || cmd.isEmpty()) return 0;

        MinecraftServer server = sw.getServer();
        ServerCommandSource src = (player != null ? player.getCommandSource() : server.getCommandSource()).withWorld(sw);

        if (isTruthy(data, "atBlock")) {
            src = src.withPosition(Vec3d.ofCenter(be.getPos()));
        }
        if (data.contains("permissionLevel")) {
            int lvl = Math.max(0, Math.min(4, data.getInt("permissionLevel")));
            src = src.withLevel(lvl);
        }

        try {
            CommandManager mgr = server.getCommandManager();
            CommandDispatcher<ServerCommandSource> dispatcher = mgr.getDispatcher();
            String input = cmd.startsWith("/") ? cmd.substring(1) : cmd;
            ParseResults<ServerCommandSource> parsed = dispatcher.parse(input, src);
            mgr.execute(parsed, input);
            return 2;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int executeRunFunction(ServerWorld sw, MinigamesBlockEntity be, PlayerEntity player, NbtCompound data) {
        String fn = data.contains("function") ? data.getString("function") : data.getString("id");
        if (fn == null || fn.isEmpty()) return 0;

        NbtCompound cmdData = new NbtCompound();
        cmdData.putString("command", "function " + fn);
        if (data.contains("atBlock")) cmdData.putBoolean("atBlock", isTruthy(data, "atBlock"));
        if (data.contains("permissionLevel")) cmdData.putInt("permissionLevel", data.getInt("permissionLevel"));
        return executeRunCommand(sw, be, player, cmdData);
    }

    private static BlockPos readPos(NbtCompound data, BlockPos origin, boolean relative) {
        int x, y, z;
        int[] arr = data.getIntArray("pos");
        if (arr != null && arr.length >= 3) {
            x = arr[0];
            y = arr[1];
            z = arr[2];
        } else {
            x = data.getInt("x");
            y = data.getInt("y");
            z = data.getInt("z");
        }
        return relative ? origin.add(x, y, z) : new BlockPos(x, y, z);
    }

    private static BlockState applyStateFromTag(BlockState base, NbtCompound props) {
        BlockState st = base;
        for (String key : props.getKeys()) {
            Property<?> prop = getProperty(st, key);
            if (prop == null) continue;
            String valueStr = props.getString(key);
            st = applyParsed(st, prop, valueStr);
        }
        return st;
    }

    private static Property<?> getProperty(BlockState state, String name) {
        for (Property<?> p : state.getProperties()) {
            if (p.getName().equals(name)) return p;
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState applyParsed(BlockState state, Property prop, String valueStr) {
        Optional parsed = prop.parse(valueStr);
        if (parsed.isPresent()) {
            return state.with(prop, (Comparable) parsed.get());
        }
        return state;
    }

    private static boolean stateMatches(BlockState state, Block mustBe, NbtCompound requiredProps) {
        if (!state.isOf(mustBe)) return false;
        if (requiredProps == null || requiredProps.isEmpty()) return true;

        for (String name : requiredProps.getKeys()) {
            String want = requiredProps.getString(name);
            Property<?> p = getProperty(state, name);
            if (p == null) return false;
            Optional<?> parsed = p.parse(want);
            if (parsed.isEmpty()) return false;
            Object curr = state.get(p);
            if (!curr.equals(parsed.get())) return false;
        }
        return true;
    }

    private static void maybeApplyBlockEntity(ServerWorld sw, BlockPos pos, NbtCompound data) {
        if (!data.contains("setLootTable")) return;
        BlockEntity be = sw.getBlockEntity(pos);
        if (be instanceof LootableContainerBlockEntity lc) {
            Identifier lootId = Identifier.of(data.getString("setLootTable"));
            long seed = data.contains("lootSeed") ? data.getLong("lootSeed") : sw.getRandom().nextLong();
            RegistryKey<LootTable> key = RegistryKey.of(RegistryKeys.LOOT_TABLE, lootId);
            lc.setLootTable(key, seed);
            be.markDirty();
        }
    }

    private static boolean isTruthy(NbtCompound data, String key) {
        if (!data.contains(key)) return false;
        if (data.getBoolean(key)) return true;
        if (data.getInt(key) != 0) return true;
        String s = data.getString(key);
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    private record ParsedBlock(BlockState state, NbtCompound beNbt) {
    }
}
