package gg.scala.chest.plugin;

import gg.scala.commons.acf.ConditionFailedException;
import gg.scala.commons.acf.annotation.CommandAlias;
import gg.scala.commons.annotations.commands.AutoRegister;
import gg.scala.commons.command.ScalaCommand;
import gg.scala.commons.issuer.ScalaPlayer;
import gg.scala.flavor.inject.Inject;
import net.evilblock.cubed.util.CC;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @author GrowlyX
 * @since 11/5/2022
 */
@AutoRegister
public class ChestProtectCommand extends ScalaCommand
{
    @Inject
    public ChestProtectPlugin plugin;

    @CommandAlias("chestprotect|protect|cp|chestprot")
    public void protect(@NotNull ScalaPlayer player)
    {
        final Block targetBlock = this.getTargetBlock(player, 3);

        if (!(targetBlock instanceof final Chest chest))
        {
            throw new ConditionFailedException(
                "You are not looking at a chest!"
            );
        }

        final String owner = chest
            .getPersistentDataContainer()
            .get(
                this.plugin.ownerKey,
                PersistentDataType.STRING
            );

        if (owner != null)
        {
            final UUID uuid = UUID.fromString(owner);

            if (!player.getUniqueId().equals(uuid)) {
                throw new ConditionFailedException(
                    "You cannot un-protect this chest as you do not own it!"
                );
            }

            chest.getPersistentDataContainer().remove(this.plugin.ownerKey);
            player.sendMessage(CC.RED + "You are no longer protecting this chest!");
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
    }

    @NotNull
    public final Block getTargetBlock(
        @NotNull ScalaPlayer player, int range
    ) {
        final BlockIterator blockIterator =
            new BlockIterator(
                player.bukkit(), range
            );

        Block lastBlock = blockIterator.next();

        while (blockIterator.hasNext()) {
            lastBlock = blockIterator.next();

            if (lastBlock.getType() == Material.AIR) {
                continue;
            }

            break;
        }

        return lastBlock;
    }
}
