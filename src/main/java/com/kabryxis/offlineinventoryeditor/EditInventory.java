package com.kabryxis.offlineinventoryeditor;

import java.io.File;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.kabryxis.kabutils.cache.Cache;
import com.kabryxis.kabutils.spigot.inventory.Inventories;
import com.kabryxis.kabutils.spigot.inventory.itemstack.ItemBuilder;
import com.kabryxis.kabutils.spigot.inventory.itemstack.Items;
import com.kabryxis.kabutils.spigot.version.Version;
import com.kabryxis.kabutils.spigot.version.wrapper.WrapperCache;
import com.kabryxis.kabutils.spigot.version.wrapper.item.itemstack.WrappedItemStack;
import com.kabryxis.kabutils.spigot.version.wrapper.nbt.compound.WrappedNBTTagCompound;
import com.kabryxis.kabutils.spigot.version.wrapper.nbt.list.WrappedNBTTagList;

public class EditInventory {
	
	private static final ItemStack EMPTY = new ItemStack(Material.AIR);
	private static final ItemStack[] DEFAULT_CONTENTS = new ItemStack[45];
	private static final boolean V9 = Version.VERSION >= Version.v1_9_R1;
	private static final int FILL_START = V9 ? 5 : 4;
	
	static {
		ItemBuilder builder = new ItemBuilder();
		ItemStack filler = builder.material(Material.BARRIER).name(ChatColor.BLACK.toString()).enchant(Enchantment.DURABILITY, 1).flag(ItemFlag.HIDE_ENCHANTS).build();
		for(int i = FILL_START; i <= 7; i++) {
			DEFAULT_CONTENTS[i] = filler;
		}
		DEFAULT_CONTENTS[8] = builder.material(Material.WOOL).data((byte)14).name(ChatColor.RED + "Refresh").enchant(Enchantment.DURABILITY, 1).flag(ItemFlag.HIDE_ENCHANTS).build();
	}
	
	private final WrappedNBTTagCompound<?> tag = WrapperCache.get(WrappedNBTTagCompound.class);
	private final WrappedNBTTagList<?> list = WrapperCache.get(WrappedNBTTagList.class);
	private final WrappedItemStack<?> itemStack = WrapperCache.get(WrappedItemStack.class);
	
	private UUID owner;
	private Inventory inv;
	private boolean online;
	
	private PlayerInventory ownerInv;
	
	private File ownerData;
	
	public void create(OfflinePlayer player) {
		this.owner = player.getUniqueId();
		this.inv = Inventories.createInventory(player.getName(), 5, DEFAULT_CONTENTS);
		this.online = false;
		this.ownerData = new File("world" + File.separator + "playerdata", owner.toString() + ".dat");
		tag.loadPlayerData(ownerData);
		tag.getList(list, "Inventory", 10);
		for(int i = 0; i < list.size(); i++) {
			tag.setHandle(list.handleGet(i));
			int slot = tag.getByte("Slot") & 0xFF;
			if(slot >= 0 && slot <= 8) slot += 36;
			else if(slot >= 100 && slot <= 103) slot = 103 - slot;
			else if(slot == 150) slot = 4;
			itemStack.createStack(tag);
			inv.setItem(slot, itemStack.getBukkitItemStack());
		}
		itemStack.clear();
		tag.clear();
		list.clear();
	}
	
	public void create(Player player) {
		this.owner = player.getUniqueId();
		this.inv = Inventories.createInventory(player.getName(), 5, DEFAULT_CONTENTS);
		this.online = true;
		this.ownerInv = player.getInventory();
		refresh();
	}
	
	public UUID getOwner() {
		return owner;
	}
	
	public void refresh() {
		inv.clear();
		ItemStack[] items = V9 ? ownerInv.getStorageContents() : ownerInv.getContents();
		for(int i = 0; i < items.length; i++) {
			ItemStack item = items[i];
			if(Items.exists(item)) inv.setItem(i < 9 ? i + 36 : i, item);
		}
		items = ownerInv.getArmorContents();
		for(int i = 0; i < items.length; i++) {
			ItemStack item = items[i];
			if(Items.exists(item)) inv.setItem(3 - i, item);
		}
		if(V9) inv.setItem(4, ownerInv.getItemInOffHand());
	}
	
	public void loggedIn(Player player) {
		if(online) throw new IllegalStateException("Player EditInventory was already in online mode when logging in.");
		this.online = true;
		this.ownerData = null;
		this.ownerInv = player.getInventory();
		ItemStack[] contents = inv.getContents();
		for(int i = 0; i < 45; i++) {
			if(outOfBounds(i)) continue;
			int slot = i;
			if(slot >= 0 && slot <= 3) slot = 3 - slot + 36;
			else if(slot >= 36 && slot <= 44) slot -= 36;
			ownerInv.setItem(slot, contents[i]);
		}
		if(V9) ownerInv.setItemInOffHand(inv.getItem(4));
	}
	
	public void loggedOut() {
		if(!online) throw new IllegalStateException("Player EditInventory was already in offline mode when logging out.");
		this.online = false;
		this.ownerInv = null;
		this.ownerData = new File("world" + File.separator + "playerdata", owner.toString() + ".dat");
	}
	
	public void open(Player player) {
		player.openInventory(inv);
	}
	
	public void close(Player player) {
		if(!isBeingViewed() && !online) {
			list.newInstance();
			ItemStack[] items = inv.getContents();
			for(int i = 0; i < items.length; i++) {
				if(outOfBounds(i)) continue;
				ItemStack item = items[i];
				if(Items.exists(item)) {
					tag.newInstance();
					itemStack.setBukkitItemStack(item);
					itemStack.save(tag);
					if(i < 4) i = 3 - i + 100;
					else if(i == 4) i = 150;
					else if(i >= 36 && i <= 44) i -= 36;
					tag.setByte("Slot", (byte)i);
					list.add(tag);
				}
			}
			tag.loadPlayerData(ownerData);
			tag.set("Inventory", list);
			tag.savePlayerData(ownerData);
			tag.clear();
			itemStack.clear();
			list.clear();
		}
	}
	
	private boolean outOfBounds(int index) {
		return index >= FILL_START && index <= 8;
	}
	
	public boolean isBeingViewed() {
		return inv.getViewers().size() != 0;
	}
	
	public void setSlot(int slot, ItemStack item, boolean player) {
		if(player) ownerInv.setItem(slot, item);
		else inv.setItem(slot, item);
	}
	
	public void removeFromSlot(int slot, boolean player) {
		setSlot(slot, EMPTY, player);
	}
	
	public void subtractFromSlot(int slot, boolean player) {
		ItemStack item = player ? ownerInv.getItem(slot) : inv.getItem(slot);
		if(Items.exists(item)) {
			int amt = item.getAmount();
			if(amt == 1) removeFromSlot(slot, player);
			else {
				item.setAmount(amt - 1);
				setSlot(slot, item, player);
			}
		}
	}
	
	public void swapPlaces(int slot1, int slot2, boolean player) {
		ItemStack buffer = player ? ownerInv.getItem(slot1) : inv.getItem(slot1);
		setSlot(slot1, player ? ownerInv.getItem(slot2) : inv.getItem(slot2), player);
		setSlot(slot2, buffer, player);
	}
	
	public void cache() {
		owner = null;
		inv = null;
		online = false;
		ownerInv = null;
		ownerData = null;
		Cache.cache(this);
	}
	
	public boolean isEqual(Inventory inv) {
		return this.inv.getTitle().equals(inv.getTitle());
	}
	
	public boolean isOnline() {
		return online;
	}
	
}
