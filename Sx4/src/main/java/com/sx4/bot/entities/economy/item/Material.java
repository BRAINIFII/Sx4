package com.sx4.bot.entities.economy.item;

import com.sx4.bot.managers.EconomyManager;

public class Material extends Item {
	
	private final String emote;
	private final boolean hidden;

	public Material(String name, long price, String emote, boolean hidden) {
		super(name, price, ItemType.MATERIAL);
		
		this.emote = emote;
		this.hidden = hidden;
	}
	
	public String getEmote() {
		return this.emote;
	}
	
	public boolean isHidden() {
		return this.hidden;
	}
	
	public static Material getFromName(String name) {
		return EconomyManager.get().getItems(ItemType.MATERIAL).stream()
			.filter(item -> item.getName().equalsIgnoreCase(name))
			.map(Material.class::cast)
			.findFirst()
			.orElse(null);
	}
	
}