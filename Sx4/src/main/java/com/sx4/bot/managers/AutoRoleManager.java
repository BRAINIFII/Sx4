package com.sx4.bot.managers;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import com.sx4.bot.core.Sx4Bot;
import com.sx4.bot.database.Database;
import com.sx4.bot.utility.ExceptionUtility;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class AutoRoleManager {
	
	private static final AutoRoleManager INSTANCE = new AutoRoleManager();
	
	public static AutoRoleManager get() {
		return AutoRoleManager.INSTANCE;
	}

	private final Map<Long, Map<Long, Map<ObjectId, ScheduledFuture<?>>>> executors;
	
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	
	public AutoRoleManager() {
		this.executors = new HashMap<>();
	}
	
	public ScheduledExecutorService getExecutor() {
		return this.executor;
	}
	
	public ScheduledFuture<?> getExecutor(long guildId, long userId, ObjectId id) {
		Map<Long, Map<ObjectId, ScheduledFuture<?>>> users = this.executors.get(guildId);
		if (users == null) {
			return null;
		}
		
		Map<ObjectId, ScheduledFuture<?>> tasks = users.get(userId);
		if (tasks == null) {
			return null;
		}
		
		return tasks.get(id);
	}
	
	public void putExecutor(long guildId, long userId, ObjectId id, ScheduledFuture<?> executor) {
		Map<Long, Map<ObjectId, ScheduledFuture<?>>> users = this.executors.get(guildId);
		Map<ObjectId, ScheduledFuture<?>> tasks;
		
		if (users == null) {
			tasks = new HashMap<>();
			tasks.put(id, executor);
			
			users = new HashMap<>();
			users.put(userId, tasks);
			
			this.executors.put(guildId, users);
			
			return;
		}
		
		tasks = users.get(userId);
		if (tasks == null) {
			tasks = new HashMap<>();
			tasks.put(id, executor);
			
			users.put(userId, tasks);
			
			return;
		}
		
		tasks.put(id, executor);
	}
	
	public void putMemberRoles(long guildId, long userId, ObjectId id, List<Long> roleIdsAdd, List<Long> roleIdsRemove, long seconds) {
		ScheduledFuture<?> executor = this.executor.schedule(() -> this.updateMemberRoles(guildId, userId, id, roleIdsAdd, roleIdsRemove), seconds, TimeUnit.SECONDS);
		
		this.putExecutor(guildId, userId, id, executor);
	}
	
	public void updateMemberRoles(long guildId, long userId, ObjectId id, List<Long> roleIdsAdd, List<Long> roleIdsRemove) {
		UpdateOneModel<Document> model = this.updateMemberRolesAndGet(guildId, userId, id, roleIdsAdd, roleIdsRemove);
		if (model != null) {
			Database.get().updateGuildById(model).whenComplete((result, exception) -> ExceptionUtility.sendErrorMessage(exception));
		}
	}
	
	public UpdateOneModel<Document> updateMemberRolesAndGet(long guildId, long userId, ObjectId id, List<Long> roleIdsAdd, List<Long> roleIdsRemove) {
		Guild guild = Sx4Bot.getShardManager().getGuildById(guildId);
		if (guild == null) {
			return null;
		}
		
		Member member = guild.getMemberById(userId);
		if (member == null) {
			return null;
		}
		
		List<Role> rolesAdd = roleIdsAdd.stream()
			.map(guild::getRoleById)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		
		List<Role> rolesRemove = roleIdsRemove.stream()
			.map(guild::getRoleById)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		
		guild.modifyMemberRoles(member, rolesAdd, rolesRemove).queue();
		
		UpdateOptions options = new UpdateOptions().arrayFilters(List.of(Filters.eq("id", userId)));
		return new UpdateOneModel<>(Filters.eq("_id", guildId), Updates.pull("autoRole.users.$[user].tasks", Filters.eq("id", id)), options);
	}
	
	public void ensureAutoRoles() {
		Database database = Database.get();
		
		List<WriteModel<Document>> bulkData = new ArrayList<>();
		database.getGuilds(Filters.elemMatch("autoRole.users", Filters.exists("id")), Projections.include("autoRole.users")).forEach(data -> {
			List<Document> users = data.getEmbedded(List.of("autoRole", "users"), Collections.emptyList());
			for (Document user : users) {
				long userId = user.get("id", 0L);
				
				List<Document> tasks = user.getList("tasks", Document.class);
				for (Document task : tasks) {
					ObjectId id = task.getObjectId("id");
					
					long executeAt = task.get("executeAt", 0L), timeNow = Clock.systemUTC().instant().getEpochSecond();
					if (executeAt - timeNow <= 0) {
						UpdateOneModel<Document> model = this.updateMemberRolesAndGet(data.get("_id", 0L), userId, id, data.getList("add", Long.class), data.getList("remove", Long.class));
						if (model != null) {
							bulkData.add(model);
						}
					} else {
						this.putMemberRoles(data.get("_id", 0L), userId, id, data.getList("add", Long.class), data.getList("remove", Long.class), executeAt - timeNow);
					}
				}
			}
		});
		
		if (!bulkData.isEmpty()) {
			database.bulkWriteGuilds(bulkData).whenComplete((result, exception) -> ExceptionUtility.sendErrorMessage(exception));
		}
	}
	
}