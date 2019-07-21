package com.sx4.events;

import static com.rethinkdb.RethinkDB.r;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.rethinkdb.gen.ast.Get;
import com.sx4.core.Sx4Bot;
import com.sx4.utils.ModUtils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelCreateEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ModEvents extends ListenerAdapter {

	public void onGuildBan(GuildBanEvent event) {
		if (event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			event.getGuild().retrieveAuditLogs().type(ActionType.BAN).queueAfter(500, TimeUnit.MILLISECONDS, auditLogs -> {
				User moderator = null;
				String reason = null;
				for (AuditLogEntry auditLog : auditLogs) {
					if (auditLog.getTargetId().equals(event.getUser().getId())) {
						moderator = auditLog.getUser();
						reason = auditLog.getReason();
						break;
					}
				}
				
				if (moderator == null || !moderator.equals(event.getJDA().getSelfUser())) {
					ModUtils.createModLogAndOffence(event.getGuild(), Sx4Bot.getConnection(), moderator, event.getUser(), "Ban", reason);
				}
			});
		}
	}
	
	public void onGuildUnban(GuildUnbanEvent event) {
		if (event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			event.getGuild().retrieveAuditLogs().type(ActionType.UNBAN).queueAfter(500, TimeUnit.MILLISECONDS, auditLogs -> {
				User moderator = null;
				String reason = null;
				for (AuditLogEntry auditLog : auditLogs) {
					if (auditLog.getTargetId().equals(event.getUser().getId())) {
						moderator = auditLog.getUser();
						reason = auditLog.getReason();
						break;
					}
				}
				
				if (moderator == null || !moderator.equals(event.getJDA().getSelfUser())) {
					ModUtils.createModLog(event.getGuild(), Sx4Bot.getConnection(), moderator, event.getUser(), "Unban", reason);
				}
			});
		}
	}
	
	public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
		if (event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			event.getGuild().retrieveAuditLogs().type(ActionType.KICK).queueAfter(500, TimeUnit.MILLISECONDS, auditLogs -> {
				User moderator = null;
				String reason = null;
				for (AuditLogEntry auditLog : auditLogs) {
					if (auditLog.getTargetId().equals(event.getUser().getId()) && LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) - auditLog.getTimeCreated().toEpochSecond() <= 10) {
						moderator = auditLog.getUser();
						reason = auditLog.getReason();
						break;
					}
				}
				
				if (moderator != null && !moderator.equals(event.getJDA().getSelfUser())) {
					ModUtils.createModLogAndOffence(event.getGuild(), Sx4Bot.getConnection(), moderator, event.getUser(), "Kick", reason);
				}
			});
		}
	}
	
	@SuppressWarnings("unchecked")
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		Map<String, Object> data = r.table("mute").get(event.getGuild().getId()).run(Sx4Bot.getConnection());
		if (data != null) {
			long timestampNow = Clock.systemUTC().instant().getEpochSecond();
			
			List<Map<String, Object>> users = (List<Map<String, Object>>) data.get("users");
			for (Map<String, Object> userData : users) {
				if (userData.get("id").equals(event.getMember().getUser().getId())) {
					if (userData.get("amount") != null) {
						long timeLeft = ((long) userData.get("time") + (userData.get("amount") instanceof Double ? (long) (double) userData.get("amount") : (long) userData.get("amount"))) - timestampNow;
						if (timeLeft > 0) {
							Role mutedRole = MuteEvents.getMuteRole(event.getGuild());
							if (mutedRole != null) {
								event.getGuild().addRoleToMember(event.getMember(), mutedRole).queue();
							}
						} else {
							MuteEvents.removeUserMute(event.getGuild().getId(), event.getMember().getId());
						}
					}
				}
			}
		}
	}
	
	public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
		Get data = r.table("mute").get(event.getGuild().getId());
		Map<String, Object> dataRan = data.run(Sx4Bot.getConnection());
		if (dataRan == null) {
			return;
		}
		
		Role mutedRole = null;
		for (Role role : event.getGuild().getRoles()) {
			if (role.getName().equals("Muted - " + event.getJDA().getSelfUser().getName())) {
				mutedRole = role;
			}
		}
		
		if (mutedRole != null) {
			if (event.getRoles().get(0).equals(mutedRole)) {
				if (event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
					event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_ROLE_UPDATE).queueAfter(500, TimeUnit.MILLISECONDS, auditLogs -> {
						long timestampNow = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC);
						User moderator = null;
						String reason = null;
						for (AuditLogEntry auditLog : auditLogs) {
							if (auditLog.getTargetId().equals(event.getUser().getId()) && timestampNow - auditLog.getTimeCreated().toEpochSecond() <= 5) {
								moderator = auditLog.getUser();
								reason = auditLog.getReason();
								break;
							}
						}
						
						if (moderator == null || !moderator.equals(event.getJDA().getSelfUser())) {
							ModUtils.createModLogAndOffence(event.getGuild(), Sx4Bot.getConnection(), moderator, event.getUser(), "Mute (Infinite)", reason);
							data.update(row -> r.hashMap("users", row.g("users").append(r.hashMap("id", event.getUser().getId()).with("amount", null).with("time", timestampNow)))).runNoReply(Sx4Bot.getConnection());
						}
					});
				}
			}
		}
	}
	
	public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
		Get data = r.table("mute").get(event.getGuild().getId());
		Map<String, Object> dataRan = data.run(Sx4Bot.getConnection());
		if (dataRan == null) {
			return;
		}
		
		Role mutedRole = null;
		for (Role role : event.getGuild().getRoles()) {
			if (role.getName().equals("Muted - " + event.getJDA().getSelfUser().getName())) {
				mutedRole = role;
			}
		}
		
		if (mutedRole != null) {
			if (event.getRoles().get(0).equals(mutedRole)) {
				if (event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
					event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_ROLE_UPDATE).queueAfter(500, TimeUnit.MILLISECONDS, auditLogs -> {
						long timestampNow = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC);
						User moderator = null;
						String reason = null;
						for (AuditLogEntry auditLog : auditLogs) {
							if (auditLog.getTargetId().equals(event.getUser().getId()) && timestampNow - auditLog.getTimeCreated().toEpochSecond() <= 5) {
								moderator = auditLog.getUser();
								reason = auditLog.getReason();
								break;
							}
						}
						
						if (moderator != null && !moderator.equals(event.getJDA().getSelfUser())) {
							ModUtils.createModLog(event.getGuild(), Sx4Bot.getConnection(), moderator, event.getUser(), "Unmute", reason);
							data.update(row -> r.hashMap("users", row.g("users").filter(d -> d.g("id").ne(event.getUser().getId())))).runNoReply(Sx4Bot.getConnection());
							MuteEvents.cancelExecutor(event.getGuild().getId(), event.getMember().getUser().getId());
						}
					});
				}
			}
		}
	}
	
	public void onTextChannelCreate(TextChannelCreateEvent event) {
		Role mutedRole = MuteEvents.getMuteRole(event.getGuild());
		if (mutedRole != null) {
			event.getChannel().putPermissionOverride(mutedRole).setDeny(Permission.MESSAGE_WRITE).queue();
		}
	}
	
	public void onVoiceChannelCreate(VoiceChannelCreateEvent event) {
		Role mutedRole = MuteEvents.getMuteRole(event.getGuild());
		if (mutedRole != null) {
			event.getChannel().putPermissionOverride(mutedRole).setDeny(Permission.VOICE_SPEAK).queue();
		}
	}
	
}
