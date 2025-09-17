package ru.imaginaerum.wd.common.mixin;

import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.wd.common.entities.goals_entity_mixin.ZombieEatPieGoal;

@Mixin(Zombie.class)
public abstract class ZombieMixin {
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void addRottenPieGoal(CallbackInfo ci) {
        Zombie self = (Zombie)(Object)this;
        self.goalSelector.addGoal(1, new ZombieEatPieGoal(self, 1.0D, 50));
    }
}