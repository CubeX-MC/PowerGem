package me.hushu.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.hushu.PowerGem;
import me.hushu.manager.ConfigManager;
import me.hushu.manager.GemManager;
import me.hushu.manager.LanguageManager;

public class PowerGemCommand implements CommandExecutor {
    private final PowerGem plugin;
    private final GemManager gemManager;
    private final ConfigManager configManager;
    private final LanguageManager languageManager;

    public PowerGemCommand(PowerGem plugin, GemManager gemManager, ConfigManager configManager, LanguageManager languageManager) {
        this.plugin = plugin;
        this.gemManager = gemManager;
        this.configManager = configManager;
        this.languageManager = languageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!cmd.getName().equalsIgnoreCase("powergem")) {
            return false;
        }

        if (args.length == 0) {
            languageManager.sendMessage(sender, "command.usage");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                gemManager.saveGems();
                configManager.reloadConfigs();
                languageManager.loadLanguage();
                plugin.loadPlugin();
                languageManager.sendMessage(sender, "command.reload_success");
                return true;
            case "powerplayer":
                Map<String, String> placeholders = new HashMap<>();
                Player pp = gemManager.getPowerPlayer();
                if (pp == null) {
                    languageManager.sendMessage(sender, "command.no_power_player");
                    return true;
                }
                placeholders.put("player", gemManager.getPowerPlayer().getName());
                languageManager.sendMessage(sender, "command.powerplayer_status", placeholders);
                return true;
            case "gems":
                gemManager.gemStatus(sender);
                return true;
            case "scatter":

                gemManager.scatterGems();
                languageManager.sendMessage(sender, "command.scatter_success");
                return true;
            case "place":
                return handlePlaceCommand(sender, args);
            case "revoke":
                return handleRevokeCommand(sender);
            case "help":
                sendHelp(sender);
                return true;
            default:
                languageManager.sendMessage(sender, "command.unknown_subcommand");
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        languageManager.sendMessage(sender, "command.help.header");
        languageManager.sendMessage(sender, "command.help.place");
        languageManager.sendMessage(sender, "command.help.revoke");
        languageManager.sendMessage(sender, "command.help.reload");
        languageManager.sendMessage(sender, "command.help.powerplayer");
        languageManager.sendMessage(sender, "command.help.gems");
        languageManager.sendMessage(sender, "command.help.scatter");
        languageManager.sendMessage(sender, "command.help.help");
        languageManager.sendMessage(sender, "command.help.footer");
    }

    private boolean handlePlaceCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            languageManager.sendMessage(sender, "command.place.usage");
            return true;
        }
        if (!(sender instanceof Player)) {
            languageManager.sendMessage(sender, "command.place.player_only");
            return true;
        }

        Player player = (Player) sender;
        World world = player.getWorld();
        // 如果玩家输入了 ~，则使用玩家当前坐标
        if (args[1].equals("~"))
            args[1] = String.valueOf(player.getLocation().getX());
        if (args[2].equals("~"))
            args[2] = String.valueOf(player.getLocation().getY());
        if (args[3].equals("~"))
            args[3] = String.valueOf(player.getLocation().getZ());
        double x, y, z;
        try {
            x = Double.parseDouble(args[1]);
            y = Double.parseDouble(args[2]);
            z = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            languageManager.sendMessage(sender, "command.place.invalid_coordinates");
            return true;
        }

        if (gemManager.getTotalGemCount() >= configManager.getRequiredCount()) {
            languageManager.sendMessage(sender, "command.place.gem_limit_reached");
            return false;
        }

        Location loc = new Location(world, x, y, z);
        if (!loc.getChunk().isLoaded()) {
            loc.getChunk().load();
        }

        // 放置宝石方块
        UUID newGemId = UUID.randomUUID();
        gemManager.placePowerGem(loc, newGemId);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("x", String.valueOf(x));
        placeholders.put("y", String.valueOf(y));
        placeholders.put("z", String.valueOf(z));
        languageManager.sendMessage(sender, "command.place.success", placeholders);
        return true;
    }

    private boolean handleRevokeCommand(CommandSender sender) {
        if (gemManager.getPowerPlayer() == null) {
            languageManager.sendMessage(sender, "command.revoke.no_power_player");
            return true;
        }
        gemManager.revokePermission(sender);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", gemManager.getPowerPlayer().getName());
        languageManager.sendMessage(sender, "command.revoke.success", placeholders);
        
        // 显示权限收回的标题
        for (Player player : Bukkit.getOnlinePlayers()) {
            languageManager.showTitle(player, "power_revoked", placeholders);
        }
        return true;
    }
}
