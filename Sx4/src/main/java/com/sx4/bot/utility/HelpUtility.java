package com.sx4.bot.utility;

import java.util.List;
import java.util.stream.Collectors;

import com.jockie.bot.core.command.ICommand;
import com.jockie.bot.core.option.IOption;
import com.sx4.bot.core.Sx4Command;
import com.sx4.bot.paged.PagedResult;
import com.sx4.bot.paged.PagedResult.SelectType;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

public class HelpUtility {

	public static Message getHelpMessage(ICommand initialCommand, boolean embed) {
		MessageBuilder builder = new MessageBuilder();
		
		Sx4Command command = (Sx4Command) initialCommand;
		String usage = command.getSubCommands().isEmpty() ? command.getUsage() : command.getUsage().trim().equals(command.getCommandTrigger()) ? command.getUsage() + " <sub command>" : command.getUsage() + " | <sub command>";
		
		StringBuilder options = new StringBuilder();
		for (int i = 0; i < command.getOptions().size(); i++) {
			IOption<?> option = command.getOptions().get(i);
			
			options.append("`" + option.getName() + (option.getType() == String.class ? "=<value>" : "") + "` - " + option.getDescription() + (i == command.getOptions().size() - 1 ? "" : "\n"));
		}
		
		if (embed) {
			EmbedBuilder embedBuilder = new EmbedBuilder();
			embedBuilder.setTitle(command.getCommandTrigger());
			embedBuilder.addField("Description", command.getDescription(), false);
			embedBuilder.addField("Usage", usage, false);
			
			if (!command.getOptions().isEmpty()) {
				embedBuilder.addField("Options", options.toString(), false);
			}
			
			if (command.getExamples().length != 0) {
				embedBuilder.addField("Examples", "`" + String.join("`\n`", command.getExamples()) + "`", false);
			}
			
			if (command.isDeveloperCommand()) {
				embedBuilder.addField("Required Permissions", "Developer", false);
			} else if (!command.getAuthorDiscordPermissions().isEmpty()) {
				embedBuilder.addField("Required Permissions", command.getAuthorDiscordPermissions().stream().map(Permission::getName).collect(Collectors.joining(", ")), false);
			}
			
			if (!command.getAliases().isEmpty()) {
				embedBuilder.addField("Aliases", String.join(", ", command.getAliases()), false);
			}
			
			if (command.getRedirects().length != 0) {
				embedBuilder.addField("Redirects", String.join(", ", command.getRedirects()), false);
			}
			
			if (!command.getSubCommands().isEmpty()) {
				embedBuilder.addField("Sub Commands", command.getSubCommands().stream().map(ICommand::getCommand).collect(Collectors.joining(", ")), false);
			}
	
			return builder.setEmbed(embedBuilder.build()).build();
		} else {
			String placeHolder = "%s:\n%s\n\n";
			
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(">>> **" + command.getCommandTrigger() + "**\n\n");
			stringBuilder.append(String.format(placeHolder, "Description", command.getDescription()));
			stringBuilder.append(String.format(placeHolder, "Usage", usage));
			
			if (!command.getOptions().isEmpty()) {
				stringBuilder.append(String.format(placeHolder, "Options", options.toString()));
			}
			
			if (command.getExamples().length != 0) {
				stringBuilder.append(String.format(placeHolder, "Examples", "`" + String.join("`\n`", command.getExamples()) + "`"));
			}
			
			if (command.isDeveloperCommand()) {
				stringBuilder.append(String.format(placeHolder, "Required Permissions", "Developer"));
			} else if (!command.getAuthorDiscordPermissions().isEmpty()) {
				stringBuilder.append(String.format(placeHolder, "Required Permissions", command.getAuthorDiscordPermissions().stream().map(Permission::getName).collect(Collectors.joining(", "))));
			}
			
			if (!command.getAliases().isEmpty()) {
				stringBuilder.append(String.format(placeHolder, "Aliases", String.join(", ", command.getAliases())));
			}
			
			if (command.getRedirects().length != 0) {
				stringBuilder.append(String.format(placeHolder, "Redirects", String.join(", ", command.getRedirects())));
			}
			
			if (!command.getSubCommands().isEmpty()) {
				stringBuilder.append(String.format(placeHolder, "Required Permissions", command.getSubCommands().stream().map(ICommand::getCommand).collect(Collectors.joining(", "))));
			}
			
			return builder.setContent(stringBuilder.toString()).build();
		}
	}
	
	public static PagedResult<Sx4Command> getCommandsPaged(List<Sx4Command> commands) {
		return new PagedResult<>(commands)
				.setAutoSelect(true)
				.setPerPage(15)
				.setDisplayFunction(command -> "`" + command.getCommandTrigger() + "` - " + command.getDescription())
				.setSelectablePredicate((content, command) -> command.getCommandTrigger().equals(content))
				.setSelect(SelectType.OBJECT)
				.setIndexed(false)
				.setAutoSelect(false);
	}
	
}