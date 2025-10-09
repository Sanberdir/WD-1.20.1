package ru.imaginaerum.wd.common.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.CookingXPManager;
import vectorwing.farmersdelight.common.block.entity.container.CookingPotMenu;
import vectorwing.farmersdelight.common.registry.ModRecipeTypes;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

@Mixin(CookingPotMenu.class)
public abstract class CookingPotMenuMixin {

    @Inject(method = "quickMoveStack", at = @At("TAIL"))
    private void wd$grantCookingXP(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = cir.getReturnValue();
        if (stack.isEmpty() || player.level().isClientSide) return;

        var level = player.level();
        var recipes = level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.COOKING.get());

        for (Recipe<?> recipe : recipes) {
            if (recipe instanceof CookingPotRecipe) {
                CookingPotRecipe cookingRecipe = (CookingPotRecipe) recipe;

                ItemStack result = cookingRecipe.getResultItem(level.registryAccess());
                if (stack.getItem() == result.getItem()) {
                    int xpPerItem = Math.max(5, Math.round(cookingRecipe.getExperience() * 10f));
                    int totalXp = xpPerItem * stack.getCount();
                    CookingXPManager.addXp(player, totalXp);
                    break;
                }
            }
        }
    }
}

