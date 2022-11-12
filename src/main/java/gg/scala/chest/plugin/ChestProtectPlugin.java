package gg.scala.chest.plugin;

import gg.scala.cache.uuid.ScalaStoreUuidCache;
import gg.scala.commons.ExtendedScalaPlugin;
import gg.scala.commons.annotations.container.ContainerEnable;
import gg.scala.commons.core.plugin.Plugin;
import gg.scala.commons.core.plugin.PluginAuthor;
import gg.scala.commons.core.plugin.PluginDependency;
import me.lucko.helper.Events;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @author GrowlyX
 * @since 11/5/2022
 */
@Plugin(
    name = "ScChestProtect",
    version = "1.0"
)
@PluginAuthor("GrowlyX")
@PluginDependency("scala-commons")
public class ChestProtectPlugin extends ExtendedScalaPlugin {

    protected final NamespacedKey ownerKey = new NamespacedKey(this, "chest-owner");

    @ContainerEnable
    public void containerEnable() {
        Events.subscribe(BlockExplodeEvent.class)
            .handler(event -> {
                if (event.getBlock().getState() instanceof Chest chest) {
                    final PersistentDataContainer container = chest
                        .getPersistentDataContainer();

                    final String owner = container.get(
                        this.ownerKey, PersistentDataType.STRING
                    );

                    if (owner != null) {
                        event.setCancelled(true);
                    }
                }
            })
            .bindWith(this);

        Events
            .subscribe(
                InventoryOpenEvent.class,
                EventPriority.LOWEST
            )
            .filter(event ->
                event.getInventory().getHolder() instanceof Chest
            )
            .handler(event -> {
                final Chest chest = (Chest) event
                    .getInventory().getHolder();

                if (chest != null) {
                    if (ensureChestOwnership(
                        chest, (Player) event.getPlayer(), "open"
                    )) {
                        event.setCancelled(true);
                    }
                }
            })
            .bindWith(this);

        Events
            .subscribe(BlockBreakEvent.class)
            .filter(event ->
                event.getBlock().getState() instanceof Chest
            )
            .handler(event -> {
                final Chest chest = (Chest) event.getBlock().getState();

                if (ensureChestOwnership(chest, event.getPlayer(), "break")) {
                    event.setCancelled(true);
                }
            })
            .bindWith(this);
    }

    private boolean ensureChestOwnership(
        @NotNull Chest chest, @NotNull Player player, @NotNull String action
    ) {
        final PersistentDataContainer container = chest
            .getPersistentDataContainer();

        final String owner = container.get(
            this.ownerKey, PersistentDataType.STRING
        );

        if (owner != null) {
            final UUID uuid = UUID.fromString(owner);

            if (!player.getUniqueId().equals(uuid)) {
                final String username = ScalaStoreUuidCache
                    .INSTANCE.username(uuid);

                player.sendMessage(
                    ChatColor.RED + "You cannot " + action + " this chest as " + username + " has locked it."
                );
                return true;
            }
        }

        return false;
    }
}
