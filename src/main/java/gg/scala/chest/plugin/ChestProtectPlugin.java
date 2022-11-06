package gg.scala.chest.plugin;

import gg.scala.cache.uuid.ScalaStoreUuidCache;
import gg.scala.commons.ExtendedScalaPlugin;
import gg.scala.commons.annotations.container.ContainerEnable;
import gg.scala.commons.core.plugin.Plugin;
import gg.scala.commons.core.plugin.PluginAuthor;
import gg.scala.commons.core.plugin.PluginDependency;
import me.lucko.helper.Events;
import me.lucko.helper.metadata.MetadataKey;
import me.lucko.helper.metadata.MetadataMap;
import net.evilblock.cubed.util.CC;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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

    protected final NamespacedKey ownerKey = new NamespacedKey(this, "owner");

    @ContainerEnable
    public void containerEnable() {
        Events
            .subscribe(
                PlayerInteractEvent.class,
                EventPriority.LOWEST
            )
            .filter(event ->
                event.getClickedBlock() != null &&
                event.getClickedBlock().getState() instanceof Chest
            )
            .handler(event -> {
                final Chest chest = (Chest) event
                    .getClickedBlock().getState();

                if (ensureChestOwnership(chest, event.getPlayer(), "open")) {
                    event.setCancelled(true);
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
                    CC.RED + "You cannot " + action + " this chest as " + username + " has locked it."
                );
                return true;
            }
        }

        return false;
    }
}
