package ru.imaginaerum.wd.common.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.imaginaerum.wd.common.blocks.custom.DragolitGrid;
import ru.imaginaerum.wd.common.blocks.custom.DragoliteCage;

public class DragoliteCageBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    private String soulFirst = ""; // Первая душа
    private String soulSecond = ""; // Вторая душа

    private String soulsTwo = "";

    public DragoliteCageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRAGOLITE_CAGE_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("soul_first", soulFirst); // Сохраняем значение первой души
        tag.putString("soul_second", soulSecond); // Сохраняем значение второй души
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("soul_first")) {
            soulFirst = tag.getString("soul_first"); // Извлекаем значение первой души из тега
        }
        if (tag.contains("soul_second")) {
            soulSecond = tag.getString("soul_second"); // Извлекаем значение второй души из тега
        }
    }

    public String getSoulsTwo() {
        if (soulFirst.equals(soulSecond)) {
            soulsTwo = soulFirst; // Устанавливаем значение soulsTwo равным первой душе
        } else {
            soulsTwo = ""; // Если души не равны, очищаем значение
        }
        return soulsTwo; // Возвращаем значение soulsTwo
    }
    public void setSoulFirst(String soulFirst) {
        this.soulFirst = soulFirst;
        setChanged(); // Уведомляем Minecraft о том, что данные изменились
    }
    public String getSoulFirst() {
        return soulFirst; // Возвращаем значение первой души
    }
    public void setSoulSecond(String soulSecond) {
        this.soulSecond = soulSecond;
        setChanged(); // Уведомляем Minecraft о том, что данные изменились
    }
    public String getSoulSecond() {
        return soulSecond; // Возвращаем значение второй души
    }





    @Override
    protected Component getDefaultName() {
        return Component.literal("Dragolite Cage");
    }


    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return null;
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, @Nullable Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public ItemStack getItem(int i) {
        return null;
    }

    @Override
    public ItemStack removeItem(int i, int i1) {
        return null;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return null;
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {
    }
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}