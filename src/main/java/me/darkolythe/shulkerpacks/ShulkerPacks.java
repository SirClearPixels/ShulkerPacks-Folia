package me.darkolythe.shulkerpacks;

import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class ShulkerPacks extends JavaPlugin {

    ShulkerListener shulkerlistener;

    private static ShulkerPacks plugin;

    String prefix = ChatColor.WHITE.toString() + ChatColor.BOLD.toString() + "[" + ChatColor.BLUE.toString() + "ShulkerPacks" + ChatColor.WHITE.toString() + ChatColor.BOLD.toString() + "] ";

    static Map<Player, ItemStack> openshulkers = new ConcurrentHashMap<>();
    Map<Player, Boolean> fromhand = new ConcurrentHashMap<>();
    Map<Player, Inventory> openinventories = new ConcurrentHashMap<>();
    Map<Player, Inventory> opencontainer = new ConcurrentHashMap<>();
    private Map<Player, Long> pvp_timer = new ConcurrentHashMap<>();
    boolean canopeninchests = true;
    boolean openpreviousinv = false;
    List<String> blacklist = new ArrayList<>();
    String defaultname = ChatColor.BLUE + "Shulker Pack";
    boolean pvp_timer_enabled = false;
    boolean shiftclicktoopen = false;
    boolean canopeninenderchest, canopeninbarrels, canplaceshulker, canopenininventory, canopeninair;
    float volume;

    /*
    Sets up the plugin
     */
    @Override
    public void onEnable() {
        plugin = this;
        shulkerlistener = new ShulkerListener(this);

        getServer().getPluginManager().registerEvents(shulkerlistener, this);

        getCommand("shulkerpacks").setExecutor(new CommandReload());

        ConfigHandler.loadConfig(this);

        // bStatsID for ShulkerPacks - get from https://bstats.org/
        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this, 3243);

        shulkerlistener.checkIfValid();

        getLogger().log(Level.INFO, (prefix + ChatColor.GREEN + "ShulkerPacks has been enabled!"));
    }

    /*
    Closes all open shulker inventories and logs disable message
     */
    @Override
    public void onDisable() {
        // Close all open shulker inventories synchronously
        // Cannot schedule tasks during shutdown, so we close inventories directly
        // This is safe because the server is shutting down anyway
        List<Player> playersToClose = new ArrayList<>(this.openinventories.keySet());
        for (Player player : playersToClose) {
            try {
                player.closeInventory();
            } catch (Exception e) {
                // Ignore errors during shutdown
            }
        }
        getLogger().log(Level.INFO, (prefix + ChatColor.RED + "ShulkerPacks has been disabled!"));
    }


    public static ShulkerPacks getInstance() {
        return plugin;
    }


    public boolean getPvpTimer(Player player) {
        if (pvp_timer.containsKey(player)) {
            return System.currentTimeMillis() - pvp_timer.get(player) < 7000;
        }
        return false;
    }

    public void setPvpTimer(Player player) {
        if (pvp_timer_enabled) {
            pvp_timer.put(player, System.currentTimeMillis());
        }
    }
}
