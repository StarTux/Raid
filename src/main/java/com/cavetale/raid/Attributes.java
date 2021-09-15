package com.cavetale.raid;

import com.cavetale.fam.Fam;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import lombok.Getter;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public final class Attributes {
    protected static final String PREFIX = "raid:attr_";
    protected static final UUID HEALTH_UUID = UUID.fromString("c4c68c7b-2e40-4568-8888-0f6ea9e5438c");
    protected static final String HEALTH_NAME = PREFIX + "health";

    private Attributes() { }

    @Getter
    public static final class Bonus {
        protected int hearts;

        public boolean isEmpty() {
            return hearts == 0;
        }
    }

    public static void update(Player player, Set<UUID> presentPlayers, BiConsumer<Player, Bonus> callback) {
        World world = player.getWorld();
        Fam.relationshipsOf(player.getUniqueId(), friendships -> {
                if (!player.isOnline()) return;
                if (!world.equals(player.getWorld())) return;
                reset(player);
                Bonus bonus = new Bonus();
                for (var friendship : friendships) {
                    if (!presentPlayers.contains(friendship.uuid)) continue;
                    if (friendship.married) {
                        bonus.hearts += 3;
                    } else if (friendship.friend) {
                        bonus.hearts += 1;
                    }
                }
                if (bonus.hearts > 0) {
                    AttributeModifier modifier = new AttributeModifier(HEALTH_UUID, HEALTH_NAME,
                                                                       (double) (bonus.hearts * 2),
                                                                       Operation.ADD_NUMBER);
                    player.getAttribute(Attribute.GENERIC_MAX_HEALTH).addModifier(modifier);
                }
                if (callback != null) callback.accept(player, bonus);
            });
    }

    public static void reset(Player player) {
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance == null) continue;
            for (AttributeModifier attributeModifier : attributeInstance.getModifiers()) {
                if (!(attributeModifier.getName().startsWith(PREFIX))) continue;
                attributeInstance.removeModifier(attributeModifier);
            }
        }
    }
}
