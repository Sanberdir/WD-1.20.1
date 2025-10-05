package ru.imaginaerum.wd.common.events;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.wd.common.items.custom.ArsMelima;

@Mod.EventBusSubscriber(modid = "wd")
public class ArsMelimaXpHandler {

    // Когда XP выдаётся программно (giveExperience, команды и т.д.)
    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        int amt = event.getAmount();
        if (amt <= 0) return;
        Player player = event.getEntity(); // в твоих mappings поле player доступно
        distributeXpToBooks(player, amt);
    }

    // Когда игрок подбирает XP-орб
    @SubscribeEvent
    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        ExperienceOrb orb = event.getOrb();
        if (orb == null) return;

        int amt;
        try {
            amt = orb.getValue(); // в 1.20 обычно getValue() даёт XP орба
        } catch (NoSuchMethodError e) {
            // на случай разных mappings — попробуй другое имя
            amt = orb.getValue();
        }

        if (amt <= 0) return;
        Player player = event.getEntity();
        distributeXpToBooks(player, amt);
    }

    /**
     * Распределить amount XP по книгам игрока последовательно:
     * заполнить первую книгу до MAX, затем идти к следующей, пока XP не кончится.
     */
    private static void distributeXpToBooks(Player player, int amount) {
        if (player == null || amount <= 0) return;
        int remaining = amount;

        // 1) Основной инвентарь
        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) break;
            if (stack != null && !stack.isEmpty() && stack.getItem() instanceof ArsMelima) {
                int added = ArsMelima.addXpToStack(stack, remaining);
                remaining -= added;
            }
        }

        // 2) Левая рука/правая рука или offhand (если осталось)
        if (remaining > 0) {
            for (ItemStack stack : player.getInventory().offhand) {
                if (remaining <= 0) break;
                if (stack != null && !stack.isEmpty() && stack.getItem() instanceof ArsMelima) {
                    int added = ArsMelima.addXpToStack(stack, remaining);
                    remaining -= added;
                }
            }
        }

        // 3) (Опционально) можно смотреть в сумки/контейнеры — но обычно это лишнее.

        // Если нужно, можно логировать сколько осталось (например, для отладки)
        // if (remaining > 0) player.sendSystemMessage(Component.literal("XP overflow: " + remaining));
    }
}
