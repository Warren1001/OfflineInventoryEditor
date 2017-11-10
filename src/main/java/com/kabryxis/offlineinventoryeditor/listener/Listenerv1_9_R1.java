package com.kabryxis.offlineinventoryeditor.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import com.kabryxis.offlineinventoryeditor.EditInventory;
import com.kabryxis.offlineinventoryeditor.OfflineInventoryEditor;

public class Listenerv1_9_R1 implements Listener {
	
	private final OfflineInventoryEditor plugin;
	
	public Listenerv1_9_R1(OfflineInventoryEditor plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
		Player player = event.getPlayer();
		EditInventory inv = plugin.getInventory(player.getUniqueId());
		if(inv != null) inv.swapPlaces(player.getInventory().getHeldItemSlot(), 4);
	}
	
}
