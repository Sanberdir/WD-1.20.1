package ru.imaginaerum.wd.server.events;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import ru.imaginaerum.wd.common.entities.ModEntities;
import ru.imaginaerum.wd.common.entities.item_projectile_entities.AbstractHurtingProjectileMod;
import ru.imaginaerum.wd.common.entities.item_projectile_entities.StarBall;
import ru.imaginaerum.wd.common.items.ItemsWD;

@Mod.EventBusSubscriber
public class StrikeRobinStick {

    public static void execute(Entity entity, LevelAccessor world, double x, double y, double z) {
        // Получаем предмет из любой руки
        ItemStack mainHandItem = (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY);
        ItemStack offHandItem = (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY);

        // Проверяем, что хотя бы в одной руке есть Robin Stick
        if (isRobinStick(mainHandItem) || isRobinStick(offHandItem)) {
            // Уменьшаем прочность на 1 и восстанавливаем
            if ((mainHandItem.getDamageValue() < 69) || (offHandItem.getDamageValue() < 69)) {
                ItemStack _ist = (isRobinStick(mainHandItem)) ? mainHandItem : offHandItem;
                if (_ist.hurt(1, RandomSource.create(), null)) {
                    _ist.shrink(1);
                    _ist.setDamageValue(0);
                }


                world.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("wd:robin_stick")), SoundSource.NEUTRAL, 1, 1);

                Entity _shootFrom = entity;
                Level projectileLevel = _shootFrom.level();
                if (!projectileLevel.isClientSide()) {
                    // Создаем и спавним StarBall
                    Projectile _entityToSpawn = new Object() {
                        public Projectile getFireball(Level level, Entity shooter, double ax, double ay, double az) {
                            AbstractHurtingProjectileMod entityToSpawn = new StarBall(ModEntities.STAR_BALL.get(), level);
                            entityToSpawn.setOwner(shooter);
                            entityToSpawn.xPower = ax;
                            entityToSpawn.yPower = ay;
                            entityToSpawn.zPower = az;
                            return entityToSpawn;
                        }
                    }.getFireball(projectileLevel, entity, (entity.getLookAngle().x / 10), (entity.getLookAngle().y / 10), (entity.getLookAngle().z / 10));
                    _entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
                    _entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z, 1, 0);
                    projectileLevel.addFreshEntity(_entityToSpawn);
                }
            }
        }
    }

    // Проверка, является ли предмет ROBIN_STICK
    private static boolean isRobinStick(ItemStack itemStack) {
        return itemStack.getItem() == ItemsWD.ROBIN_STICK.get();
    }
}