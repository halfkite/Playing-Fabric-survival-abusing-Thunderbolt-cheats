package cn.zheshi.zuobi;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashSet;
import java.util.Set;

public final class ClearedFloorState extends PersistentState {
    private static final Type<ClearedFloorState> TYPE = new Type<>(ClearedFloorState::new, ClearedFloorState::fromNbt, DataFixTypes.LEVEL);
    private final Set<Long> pendingFloors = new HashSet<>();

    public static ClearedFloorState get(PersistentStateManager manager) {
        return manager.getOrCreate(TYPE, ZheshiZuobiMod.MOD_ID + "_cleared_floors");
    }

    private static ClearedFloorState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        ClearedFloorState state = new ClearedFloorState();
        for (long value : nbt.getLongArray("Chunks")) state.pendingFloors.add(value);
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.putLongArray("Chunks", pendingFloors.stream().mapToLong(Long::longValue).toArray());
        return nbt;
    }

    public boolean contains(ChunkPos pos) {
        return pendingFloors.contains(pos.toLong());
    }

    public void add(ChunkPos pos) {
        if (pendingFloors.add(pos.toLong())) markDirty();
    }

    public void remove(ChunkPos pos) {
        if (pendingFloors.remove(pos.toLong())) markDirty();
    }
}

