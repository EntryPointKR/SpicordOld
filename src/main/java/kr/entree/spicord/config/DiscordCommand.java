package kr.entree.spicord.config;

import kr.entree.spicord.bukkit.structure.Message;
import kr.entree.spicord.bukkit.util.ConfigurationSections;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by JunHyung Lim on 2020-04-02
 */
@Data
@RequiredArgsConstructor
@ExtensionMethod(ConfigurationSections.class)
public class DiscordCommand {
    private final Collection<String> commands;
    private final Set<String> channelIds;
    private final String messageId;

    public static DiscordCommand parse(ConfigurationSection section, SpicordConfig topConfig) {
        val command = section.get("command");
        val channel = ConfigurationSections.getStringCollection(section, "channel");
        val message = section.getString("message", "");
        val commands = new ArrayList<String>();
        if (command instanceof Collection) {
            val collection = ((Collection<?>) command);
            val strings = collection.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
            commands.addAll(strings);
        } else if (command != null) {
            commands.add(command.toString());
        }
        return new DiscordCommand(commands, new HashSet<>(topConfig.remapChannel(channel)), message);
    }

    public boolean isValidChannel(String channelId) {
        return channelIds.isEmpty() || channelIds.contains(channelId);
    }

    public boolean check(Message message) {
        val contents = message.getContents();
        return !commands.isEmpty()
                && isValidChannel(message.getChannelId())
                && commands.stream().anyMatch(contents::startsWith);
    }
}