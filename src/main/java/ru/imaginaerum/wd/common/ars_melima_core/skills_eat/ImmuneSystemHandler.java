package ru.imaginaerum.wd.common.ars_melima_core.skills_eat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressNode;
import ru.imaginaerum.wd.client.gui.ars_melima.progress_tree.ProgressionLoader;
import ru.imaginaerum.wd.client.gui.ars_melima.screens.ui_manager.ProgressionUnlockManager;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Mod.EventBusSubscriber(modid = "wd", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ImmuneSystemHandler {

    // Счётчик полученного урона от негативных эффектов для каждого игрока
    private static final Map<ServerPlayer, Integer> DAMAGE_COUNTER = new HashMap<>();

    // Эффекты, от которых защищает навык
    private static final MobEffect[] PROTECTED_EFFECTS = {
            MobEffects.WITHER,      // Иссушение
            MobEffects.POISON,      // Отравление
            MobEffects.HUNGER,      // В принципе тоже негативный
            MobEffects.CONFUSION,   // Тошнота
            MobEffects.BLINDNESS,   // Слепота
            MobEffects.WEAKNESS,    // Слабость
            MobEffects.UNLUCK,      // Неудача
            // Обморожения нет в ванильном Minecraft, но если есть в модах:
            // Можно добавить кастомные эффекты
    };

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Проверяем, открыт ли навык immune_system
        if (!ProgressionUnlockManager.isUnlocked(player, "immune_system")) return;

        // Получаем уровень навыка
        ProgressNode immuneNode = findNode("immune_system");
        if (immuneNode == null) return;

        int level = Math.max(1, Math.min(5, immuneNode.getLevel())); // Ограничиваем 5 уровнями

        DamageSource source = event.getSource();
        float damage = event.getAmount();

        // Проверяем, является ли урон от негативного эффекта
        if (isDamageFromNegativeEffect(player, source)) {
            // Снижаем урон в зависимости от уровня
            float reducedDamage = reduceDamage(damage, level);
            event.setAmount(reducedDamage);

            // Увеличиваем счётчик урона
            incrementDamageCounter(player);

            // Проверяем, нужно ли восстановить здоровье (для уровня 5)
            if (level >= 5 && shouldHealPlayer(player)) {
                healPlayer(player);
                resetDamageCounter(player);
            }

            System.out.println("[ImmuneSystem] Reduced damage from " + damage + " to " + reducedDamage + " for level " + level);
        }
    }

    /**
     * Проверяет, является ли урон от негативного эффекта
     */
    private static boolean isDamageFromNegativeEffect(LivingEntity entity, DamageSource source) {
        // 1. Проверяем активные эффекты у сущности
        for (MobEffect effect : PROTECTED_EFFECTS) {
            if (entity.hasEffect(effect)) {
                return true;
            }
        }

        // 2. Новая проверка на магический урон в 1.20.1+
        // Получаем тип урона (DamageType) из источника
        ResourceKey<DamageType> damageTypeKey = source.typeHolder().unwrapKey().orElse(null);

        if (damageTypeKey != null) {
            // Проверяем, является ли урон иссушением (wither)
            if (damageTypeKey.location().getPath().equals("wither")) {
                return true;
            }

            // Проверяем, является ли урон магическим (magic)
            if (damageTypeKey.location().getPath().equals("magic")) {
                return true;
            }

            // Проверяем, является ли урон обморожением (freeze)
            if (damageTypeKey.location().getPath().equals("freeze")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Снижает урон в зависимости от уровня
     * Уровень 1: в 1.5 раза (делим на 1.5)
     * Уровень 2: в 2 раза (делим на 2)
     * Уровень 3: в 2.5 раза (делим на 2.5)
     * Уровень 4: в 3 раза (делим на 3)
     * Уровень 5: в 3 раза (делим на 3)
     */
    private static float reduceDamage(float originalDamage, int level) {
        float multiplier = getDamageMultiplier(level);
        return originalDamage / multiplier;
    }

    /**
     * Возвращает множитель снижения урона для уровня
     */
    private static float getDamageMultiplier(int level) {
        switch (level) {
            case 1: return 1.5f;
            case 2: return 2.0f;
            case 3: return 2.5f;
            case 4: return 3.0f;
            case 5: return 3.0f;
            default: return 1.0f;
        }
    }

    /**
     * Увеличивает счётчик полученного урона
     */
    private static void incrementDamageCounter(ServerPlayer player) {
        int current = DAMAGE_COUNTER.getOrDefault(player, 0);
        DAMAGE_COUNTER.put(player, current + 1);
    }

    /**
     * Проверяет, нужно ли восстановить здоровье (после 3 повреждений)
     */
    private static boolean shouldHealPlayer(ServerPlayer player) {
        return DAMAGE_COUNTER.getOrDefault(player, 0) >= 3;
    }

    /**
     * Восстанавливает 2 сердца (4 единицы здоровья) игроку
     */
    private static void healPlayer(ServerPlayer player) {
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float newHealth = Math.min(maxHealth, currentHealth + 4.0f); // 2 сердца = 4 здоровья

        player.setHealth(newHealth);
        player.heal(4.0f); // Альтернативный способ

        System.out.println("[ImmuneSystem] Healed player " + player.getName().getString() + " for 2 hearts");
    }

    /**
     * Сбрасывает счётчик урона
     */
    private static void resetDamageCounter(ServerPlayer player) {
        DAMAGE_COUNTER.remove(player);
    }

    /**
     * Очищаем счётчик при выходе игрока
     */
    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DAMAGE_COUNTER.remove(player);
        }
    }

    /**
     * Очищаем счётчик при смерти игрока
     */
    @SubscribeEvent
    public static void onPlayerDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DAMAGE_COUNTER.remove(player);
        }
    }

    private static ProgressNode findNode(String id) {
        List<ProgressNode> nodes = ProgressionLoader.loadNodes();
        for (ProgressNode n : nodes) {
            if (id.equals(n.getId())) return n;
        }
        return null;
    }
}