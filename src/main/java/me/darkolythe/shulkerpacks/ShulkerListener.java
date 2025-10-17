package me.darkolythe.shulkerpacks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.ArrayList;
import java.util.List;

public class ShulkerListener implements Listener {

    public ShulkerPacks main;
    public ShulkerListener(ShulkerPacks plugin) {
        this.main = plugin; //set it equal to an instance of main
    }

    /*
    Saves the shulker on inventory drag if its open
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            player.getScheduler().run(main, (task) -> {
                if (!saveShulker(player, event.getView().getTitle())) {
                    event.setCancelled(true);
                }
            }, null);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        List<Player> closeInventories = new ArrayList<>();
        for (Player p : ShulkerPacks.openshulkers.keySet()) {
            if (ShulkerPacks.openshulkers.get(p).equals(event.getItem())) {
                closeInventories.add(p);
            }
        }
        for (Player p : closeInventories) {
            if (event.getInitiator().getLocation() != null && event.getInitiator().getLocation().getWorld() == p.getWorld()) {
                if (event.getInitiator().getLocation().distance(p.getLocation()) < 6) {
                    p.closeInventory();
                }
            }
        }
    }

    /*
    Opens the shulker if its not in a weird inventory, then saves it
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
    	if (event.isCancelled()) {
    		return;
    	}
    	
        Player player = (Player) event.getWhoClicked();

        if (ShulkerPacks.openshulkers.containsKey(player)) {
            if (ShulkerPacks.openshulkers.get(player).getType() == Material.AIR) {
                event.setCancelled(true);
                player.closeInventory();
                return;
            }
        }

        if (checkIfOpen(event.getCurrentItem())) { //cancels the event if the player is trying to remove an open shulker
            if (event.getClick() != ClickType.RIGHT) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getWhoClicked() instanceof Player && event.getClickedInventory() != null) {
            if (event.getCurrentItem() != null && (ShulkerPacks.openshulkers.containsKey(player) && event.getCurrentItem().equals(ShulkerPacks.openshulkers.get(player)))) {
                event.setCancelled(true);
                return;
            }

            // prevent the player from opening it in a chest if they have no permission
            if (event.getClickedInventory() != null && (event.getClickedInventory().getType() == InventoryType.CHEST)) {
                if (!main.canopeninchests || (main.canopeninchests && !player.hasPermission("shulkerpacks.open_in_chests"))) {
                    return;
                }
            }

            // prevent the player from opening the shulkerbox in inventories without storage slots
            String typeStr = event.getClickedInventory().getType().toString();
            InventoryType type = event.getClickedInventory().getType();
            if (typeStr.equals("WORKBENCH") || typeStr.equals("ANVIL") || typeStr.equals("BEACON") || typeStr.equals("MERCHANT") || typeStr.equals("ENCHANTING") ||
                    typeStr.equals("GRINDSTONE") || typeStr.equals("CARTOGRAPHY") || typeStr.equals("LOOM") || typeStr.equals("STONECUTTER")) {
                return;
            }

            // prevent the player from opening it in the crafting slots of their inventory
            if (type == InventoryType.CRAFTING && event.getRawSlot() >= 1 && event.getRawSlot() <= 4) {
                return;
            }

            // prevent the player from opening it in the inventory if they have no permission
            if ((player.getInventory() == event.getClickedInventory())) {
                if (!main.canopenininventory || !player.hasPermission("shulkerpacks.open_in_inventory")) {
            	    return;
                }
            }

            // prevent the player from opening the shulkerbox in the result slot of an inventory (this can be done with dyes)
            if (event.getSlotType() == InventoryType.SlotType.RESULT) {
                return;
            }

            if(event.getClickedInventory() != null && event.getClickedInventory().getHolder() != null && event.getClickedInventory().getHolder().getClass().toString().endsWith(".CraftBarrel") && !main.canopeninbarrels) {
            	return;
            }

            if (!main.canopeninenderchest && type == InventoryType.ENDER_CHEST) {
                return;
            }

            for (String str: main.blacklist) {
                if (ChatColor.stripColor(player.getOpenInventory().getTitle()).contains(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', str)))) {
                    return;
                }
            }

            if (!main.shiftclicktoopen || event.isShiftClick()) {
                boolean isCancelled = event.isCancelled();
                event.setCancelled(true);
                if (event.isRightClick() && openInventoryIfShulker(event.getCurrentItem(), player)) {
                    main.fromhand.remove(player);
                    return;
                }
                event.setCancelled(isCancelled);
            }

            player.getScheduler().run(main, (task) -> {
                if (!saveShulker(player, event.getView().getTitle())) {
                    event.setCancelled(true);
                }
            }, null);
        }
    }

    // Deals with multiple people opening the same shulker
    private static boolean checkIfOpen(ItemStack shulker) {
        for (ItemStack i : ShulkerPacks.openshulkers.values()) {
            if (i.equals(shulker)) {
                return true;
            }
        }
        return false;
    }

    /*
    Saves the shulker if its open, then removes the current open shulker from the player data
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (saveShulker(player, player.getOpenInventory().getTitle())) {
                player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, main.volume, 1);
                if (main.openpreviousinv) {
                    openPreviousInventory(player);
                }
            }
            ShulkerPacks.openshulkers.remove(player);
        }
    }


    private void openPreviousInventory(Player player) {
        InventoryType type = main.opencontainer.get(player).getType();
        if (type != InventoryType.CRAFTING && type != InventoryType.SHULKER_BOX) {
            player.getScheduler().runDelayed(main, (task) -> {
                player.openInventory(main.opencontainer.get(player));
                main.opencontainer.remove(player);
            }, null, 1);
        }
    }


    /*
    Opens the shulker if the air was clicked with one
     */
    @EventHandler
    public void onClickAir(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (main.canopeninair && (event.getClickedBlock() == null || event.getClickedBlock().getType() == Material.AIR)) {
            if ((!main.shiftclicktoopen || player.isSneaking())) {
                if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                     if (main.canopeninair && player.hasPermission("shulkerpacks.open_in_air")) {
                         ItemStack item = event.getItem();
                         openInventoryIfShulker(item, event.getPlayer());
                         main.fromhand.put(player, true);
                     }
                }
            }
        }
    }

    @EventHandler
    public void onShulkerPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType().toString().contains("SHULKER_BOX")) {
            if (!main.canplaceshulker) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            main.setPvpTimer((Player) event.getDamager());
            main.setPvpTimer((Player) event.getEntity());
        }
    }

    @EventHandler
    public void onPlayerShoot(ProjectileHitEvent event) {
        if (event.getHitEntity() instanceof Player && event.getEntity().getShooter() instanceof Player) {
            main.setPvpTimer((Player) event.getEntity().getShooter());
            main.setPvpTimer((Player) event.getHitEntity());
        }
    }

    /*
    Saves the shulker data in the itemmeta
     */
    public boolean saveShulker(Player player, String title) {
        try {
            if (ShulkerPacks.openshulkers.containsKey(player)) {
                if (title.equals(main.defaultname) || (ShulkerPacks.openshulkers.get(player).hasItemMeta() &&
                        ShulkerPacks.openshulkers.get(player).getItemMeta().hasDisplayName() &&
                        (ShulkerPacks.openshulkers.get(player).getItemMeta().getDisplayName().equals(title)))) {
                    ItemStack item = ShulkerPacks.openshulkers.get(player);
                    if (item != null) {
                        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
                        ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
                        shulker.getInventory().setContents(main.openinventories.get(player).getContents());
                        meta.setBlockState(shulker);
                        item.setItemMeta(meta);
                        ShulkerPacks.openshulkers.put(player, item);
                        updateAllInventories(ShulkerPacks.openshulkers.get(player));
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            ShulkerPacks.openshulkers.remove(player);
            player.closeInventory();
            return false;
        }
        return false;
    }

    private void updateAllInventories(ItemStack item) {
        for (Player p : ShulkerPacks.openshulkers.keySet()) {
            if (ShulkerPacks.openshulkers.get(p).equals(item)) {
                BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
                ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
                p.getOpenInventory().getTopInventory().setContents(shulker.getInventory().getContents());
                // updateInventory() is deprecated and no longer needed in modern versions
            }
        }
    }

    /*
    Opens the shulker inventory with the contents of the shulker
     */
    public boolean openInventoryIfShulker(ItemStack item, Player player) {
        if (player.hasPermission("shulkerpacks.use")) {
            if (item != null) {
                if (item.getAmount() == 1 && item.getType().toString().contains("SHULKER")) {

                    if (main.getPvpTimer(player)) {
                        player.sendMessage(main.prefix + ChatColor.RED + "You cannot open shulkerboxes in combat!");
                        return false;
                    }

                    if (item.getItemMeta() instanceof BlockStateMeta) {
                        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
                        if (meta != null && meta.getBlockState() instanceof ShulkerBox) {
                            ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
                            Inventory inv;
                            if (meta.hasDisplayName()) {
                                inv = Bukkit.createInventory(new ShulkerHolder(), InventoryType.SHULKER_BOX, meta.getDisplayName());
                            } else {
                                inv = Bukkit.createInventory(new ShulkerHolder(), InventoryType.SHULKER_BOX, main.defaultname);
                            }
                            inv.setContents(shulker.getInventory().getContents());

                            main.opencontainer.put(player, player.getOpenInventory().getTopInventory());

                            player.getScheduler().runDelayed(main, (task) -> {
                                player.openInventory(inv);
                                player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, main.volume, 1);
                                ShulkerPacks.openshulkers.put(player, item);
                                main.openinventories.put(player, player.getOpenInventory().getTopInventory());
                                // Start validation task for this player
                                startValidationTask(player);
                            }, null, 1);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    void checkIfValid() {
        // No longer needed - validation is now done per-player when they open a shulker
        // See startValidationTask() method
    }

    void startValidationTask(Player player) {
        // Start a per-player repeating task for validation
        player.getScheduler().runAtFixedRate(main, (task) -> {
            // Check if player still has the shulker open
            if (!ShulkerPacks.openshulkers.containsKey(player)) {
                task.cancel();
                return;
            }

            // Check if the shulker item still exists
            if (ShulkerPacks.openshulkers.get(player).getType() == Material.AIR) {
                player.closeInventory();
                task.cancel();
                return;
            }

            // Check distance from container if applicable
            if (main.opencontainer.containsKey(player)) {
                if (main.opencontainer.get(player).getLocation() != null) {
                    if (main.opencontainer.get(player).getLocation().getWorld() == player.getWorld()) {
                        if (main.opencontainer.get(player).getLocation().distance(player.getLocation()) > 6) {
                            player.closeInventory();
                            task.cancel();
                        }
                    }
                }
            }
        }, null, 1L, 1L);
    }
}
