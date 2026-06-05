package com.fastplace;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class PlaceSession {

    private final ItemStack itemStack;
    private final Set<BlockPos> placedPositions = new HashSet<>();
    private boolean active = true;

    public PlaceSession(ItemStack itemStack) {
        // コピーを保持（スタック参照ではなく）
        this.itemStack = itemStack.copy();
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public boolean hasPlaced(BlockPos pos) {
        return placedPositions.contains(pos);
    }

    public void addPlaced(BlockPos pos) {
        placedPositions.add(pos);
    }

    public int getCount() {
        return placedPositions.size();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
