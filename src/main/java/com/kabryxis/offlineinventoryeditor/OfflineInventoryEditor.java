package com.kabryxis.offlineinventoryeditor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.kabryxis.kabutils.cache.Cache;
import com.kabryxis.kabutils.spigot.version.Version;
import com.kabryxis.offlineinventoryeditor.listener.GeneralListener;
import com.kabryxis.offlineinventoryeditor.listener.Listenerv1_9_R1;

public class OfflineInventoryEditor extends JavaPlugin {
	
	private Map<Player, EditInventory> viewing;
	private Map<UUID, EditInventory> inventories;
	private boolean shouldAccomodateOffHand;
	
	@Override
	public void onEnable() {
		this.viewing = new HashMap<>();
		this.inventories = new HashMap<>();
		this.shouldAccomodateOffHand = Version.VERSION >= Version.v1_9_R1;
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new GeneralListener(this), this);
		if(shouldAccomodateOffHand()) pm.registerEvents(new Listenerv1_9_R1(this), this);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase("offlineinventoryeditor")) {
			if(!(sender instanceof Player)) return true;
			Player player = (Player)sender;
			if(args.length == 1) {
				OfflinePlayer owner = getServer().getOfflinePlayer(args[0]);
				EditInventory inv = inventories.computeIfAbsent(owner.getUniqueId(), uuid -> Cache.get(EditInventory.class));
				if(owner.isOnline()) inv.create(owner.getPlayer());
				else inv.create(owner);
				viewing.put(player, inv);
				inv.open(player);
				return true;
			}
		}
		return false;
	}
	
	public EditInventory getViewing(Player player) {
		return viewing.get(player);
	}
	
	public EditInventory removeViewing(Player player) {
		return viewing.remove(player);
	}
	
	public EditInventory getInventory(UUID owner) {
		return inventories.get(owner);
	}
	
	public EditInventory removeInventory(UUID owner) {
		return inventories.remove(owner);
	}
	
	public boolean hasInventory(UUID owner) {
		return inventories.containsKey(owner);
	}
	
	public boolean shouldAccomodateOffHand() {
		return shouldAccomodateOffHand;
	}
	
	public int getParallelSlot(int slot, boolean isPlayerInventory) {
		if(slot > 45) throw new ArrayIndexOutOfBoundsException("Slot was greater than 45: " + slot);
		if(isPlayerInventory) {
			switch(slot) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
				break;
			case 5:
			case 6:
			case 7:
			case 8:
				return slot - 5;
			case 45:
				return 4;
			default:
				return slot;
			}
		}
		else {
			switch(slot) {
			case 0:
			case 1:
			case 2:
			case 3:
				return 39 - slot;
			case 4:
				if(shouldAccomodateOffHand) return 45;
			case 5:
			case 6:
			case 7:
				break;
			case 8:
				return slot;
			default:
				return slot > 35 ? slot - 36 : slot;
			}
		}
		return -1;
	}
	
}
