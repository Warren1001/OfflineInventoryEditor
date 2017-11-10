package com.kabryxis.offlineinventoryeditor.listener;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.PlayerInventory;

import com.kabryxis.offlineinventoryeditor.EditInventory;
import com.kabryxis.offlineinventoryeditor.OfflineInventoryEditor;

public class GeneralListener implements Listener {
	
	private final OfflineInventoryEditor plugin;
	
	public GeneralListener(OfflineInventoryEditor plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		//System.out.println("rawSlot: " + event.getRawSlot());
		int slot = event.getRawSlot();
		if(slot < 0 || slot > 45) return;
		Player player = (Player)event.getWhoClicked();
		InventoryView view = player.getOpenInventory();
		Inventory clickedInventory = event.getClickedInventory(), topInventory = view.getTopInventory(), bottomInventory = view.getBottomInventory();
		if(clickedInventory instanceof PlayerInventory || clickedInventory.equals(bottomInventory)) {
			EditInventory inv = plugin.getInventory(player.getUniqueId());
			if(inv != null) {
				ClickType click = event.getClick();
				if(click == ClickType.SHIFT_LEFT && clickedInventory.equals(bottomInventory) && topInventory.getSize() == 45) {
					EditInventory viewing = plugin.getViewing(player);
					if(viewing != null && viewing.isEqual(topInventory)) {
						event.setCancelled(true);
						return;
					}
				}
				handleClick(event, inv, true);
			}
		}
		else {
			EditInventory inv = plugin.getViewing(player);
			if(inv != null && inv.isEqual(clickedInventory) && inv.isOnline()) handleClick(event, inv, false);
		}
	}
	
	private void handleClick(InventoryClickEvent event, EditInventory inv, boolean player) {
		int slot = plugin.getParallelSlot(event.getRawSlot(), player);
		if(slot == -1) {
			event.setCancelled(true);
			return;
		}
		switch(event.getClick()) {
		case CONTROL_DROP:
			inv.removeFromSlot(slot, player);
			break;
		case DROP:
			inv.subtractFromSlot(slot, player);
			break;
		case NUMBER_KEY:
			inv.swapPlaces(slot, event.getHotbarButton(), player);
			break;
		case RIGHT:
			// TODO
			break;
		case LEFT:
			inv.setSlot(slot, event.getCursor(), player);
			break;
		case SHIFT_LEFT:
			inv.removeFromSlot(slot, player);
			break;
		default:
			break;
		}
	}
	
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		Player player = (Player)event.getPlayer();
		EditInventory inventory = plugin.removeViewing(player);
		if(inventory != null) {
			inventory.close(player);
			if(!inventory.isBeingViewed()) plugin.removeInventory(inventory.getOwner()).cache();
		}
	}
	
	@EventHandler
	public void onPlayerLogin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		if(plugin.hasInventory(uuid)) plugin.getInventory(uuid).loggedIn(player);
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		if(plugin.hasInventory(uuid)) plugin.getInventory(uuid).loggedOut();
	}
	
}
