package com.sx4.bot.events.mod;

import com.sx4.bot.entities.mod.Action;
import com.sx4.bot.hooks.mod.ModAction;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class KickEvent extends ModActionEvent {

	public KickEvent(Member moderator, User target, String reason) {
		super(moderator, target, reason, new Action(ModAction.KICK));
	}
	
}
