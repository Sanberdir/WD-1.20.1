package ru.imaginaerum.wd.common.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.wd.client.gui.ars_melima.NetworkCookingXp;
import ru.imaginaerum.wd.client.gui.ars_melima.SyncCookingXpPacket;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.CookingXPManager;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe;
import vectorwing.farmersdelight.common.registry.ModRecipeTypes;

import java.util.List;

@Mixin(CuttingBoardBlockEntity.class)
public abstract class CuttingBoardBlockEntityMixin {
    // временное хранилище входного предмета
    private ItemStack wd$capturedInput = ItemStack.EMPTY;

    @Inject(
            method = "processStoredItemUsingTool(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Z",
            at = @At("HEAD"),
            remap = false
    )
    private void wd$captureInput(ItemStack toolStack, Player player, CallbackInfoReturnable<Boolean> cir) {
        CuttingBoardBlockEntity self = (CuttingBoardBlockEntity) (Object) this;
        // копируем, чтобы не держать ссылку на оригинал
        this.wd$capturedInput = self.getStoredItem().copy();
    }

    @Inject(
            method = "processStoredItemUsingTool(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Z",
            at = @At("RETURN"),
            remap = false
    )
    private void addCookingXp(ItemStack toolStack, Player player, CallbackInfoReturnable<Boolean> cir) {
        // если обработка не сработала — чистим и выходим
        if (!cir.getReturnValue()) {
            this.wd$capturedInput = ItemStack.EMPTY;
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            this.wd$capturedInput = ItemStack.EMPTY;
            return;
        }

        CuttingBoardBlockEntity self = (CuttingBoardBlockEntity) (Object) this;
        // Получаем уровень через публичный геттер (т.к. поле level protected)
        BlockEntity be = (BlockEntity) (Object) this;
        Level level = be.getLevel();

        if (level == null) {
            this.wd$capturedInput = ItemStack.EMPTY;
            return;
        }

        if (this.wd$capturedInput.isEmpty()) {
            this.wd$capturedInput = ItemStack.EMPTY;
            return;
        }

        // Создаём временный ItemStackHandler с одним слотом и кладём туда захваченный вход
        ItemStackHandler tempHandler = new ItemStackHandler(1);
        tempHandler.setStackInSlot(0, this.wd$capturedInput.copy());
        RecipeWrapper wrapper = new RecipeWrapper(tempHandler);

        // Ищем рецепты типа CUTTING для этого входа
        List<CuttingBoardRecipe> recipes = level.getRecipeManager()
                .getRecipesFor((net.minecraft.world.item.crafting.RecipeType) ModRecipeTypes.CUTTING.get(), wrapper, level);

        CuttingBoardRecipe found = recipes.stream()
                .filter(r -> r.getTool().test(toolStack))
                .findFirst()
                .orElse(null);

        if (found != null) {
            int fortune = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, toolStack);
            List<ItemStack> rolled = found.rollResults(level.getRandom(), fortune);

            boolean hasEdible = rolled.stream().anyMatch(s -> !s.isEmpty() && s.isEdible());

            if (hasEdible) {
                int xpAmount = 5; // можно вынести в конфиг
                CookingXPManager.addXp(serverPlayer, xpAmount);

                // синхронизация с клиентом
                NetworkCookingXp.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new SyncCookingXpPacket(CookingXPManager.getXp(serverPlayer), CookingXPManager.getLevel(serverPlayer))
                );
            }
        }

        // очистка временного поля
        this.wd$capturedInput = ItemStack.EMPTY;
    }
}
