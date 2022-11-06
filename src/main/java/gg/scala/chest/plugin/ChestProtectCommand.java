package gg.scala.chest.plugin;

import gg.scala.commons.acf.ConditionFailedException;
import gg.scala.commons.acf.annotation.CommandAlias;
import gg.scala.commons.annotations.commands.AutoRegister;
import gg.scala.commons.command.ScalaCommand;
import gg.scala.commons.issuer.ScalaPlayer;
import gg.scala.flavor.inject.Inject;
import me.lucko.helper.Events;
import me.lucko.helper.Schedulers;
import me.lucko.helper.terminable.composite.CompositeTerminable;
import net.evilblock.cubed.util.CC;
import org.bukkit.block.Chest;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author GrowlyX
 * @since 11/5/2022
 */
@AutoRegister
public class ChestProtectCommand extends ScalaCommand {

    @Inject
    public ChestProtectPlugin plugin;

    private final Set<UUID> prompting = new HashSet<>();

    @CommandAlias("chestprotect|protect|cp|chestprot|unprotect|unprot|up")
    public void protect(@NotNull ScalaPlayer player) {
        if (prompting.contains(player.getUniqueId())) {
            throw new ConditionFailedException(
                CC.RED + "You're already in a protection prompt. Right click a chest to protect/unprotect it!"
            );
        }

        final CompositeTerminable terminable = CompositeTerminable.create();
        terminable.with(() -> prompting.remove(player.getUniqueId()));

        prompting.add(player.getUniqueId());

        player.sendMessage(
            CC.GREEN + "Right click a chest to protect/unprotect it... You have 10 seconds!"
        );

        Schedulers
            .async()
            .runLater(
                terminable::closeAndReportException,
                10L, TimeUnit.SECONDS
            )
            .bindWith(terminable);

        Events
            .subscribe(
                PlayerQuitEvent.class
            )
            .handler(event -> {
                terminable.closeAndReportException();
            })
            .bindWith(terminable);

        Events
            .subscribe(
                PlayerInteractEvent.class,
                EventPriority.HIGHEST
            )
            .filter(event ->
                event.getClickedBlock() != null
            )
            .handler(event -> {
                if (!(event.getClickedBlock() instanceof final Chest chest)) {
                    player.sendMessage(CC.RED + "You did not click a chest block!");
                    terminable.closeAndReportException();
                    return;
                }

                final String owner = chest
                    .getPersistentDataContainer()
                    .get(
                        this.plugin.ownerKey,
                        PersistentDataType.STRING
                    );

                if (owner != null) {
                    final UUID uuid = UUID.fromString(owner);

                    if (!player.getUniqueId().equals(uuid)) {
                        player.sendMessage(CC.RED + "You cannot un-protect this chest as you do not own it!");
                        terminable.closeAndReportException();
                        return;
                    }

                    chest.getPersistentDataContainer().remove(this.plugin.ownerKey);
                    player.sendMessage(CC.RED + "You are no longer protecting this chest!");

                    terminable.closeAndReportException();
                    return;
                }

                chest
                    .getPersistentDataContainer()
                    .set(
                        this.plugin.ownerKey,
                        PersistentDataType.STRING,
                        player.getUniqueId().toString()
                    );

                player.sendMessage(
                    CC.GREEN + "You are now protecting this chest!"
                );

                terminable.closeAndReportException();
            })
            .bindWith(terminable);
    }
}
