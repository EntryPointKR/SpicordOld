package kr.entree.spicord.config;

import io.vavr.control.Option;
import io.vavr.control.Try;
import kr.entree.spicord.bukkit.util.ConfigurationSections;
import kr.entree.spicord.discord.Member;
import kr.entree.spicord.discord.Message;
import lombok.Data;
import lombok.val;
import net.dv8tion.jda.api.Permission;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

/**
 * Created by JunHyung Lim on 2020-04-02
 */
@Data
public class DiscordCommand {
    private final Collection<String> literals;
    private final Set<String> channelIds;
    private final String messageId;
    private final Set<Permission> permissions;
    private final Set<String> roles;

    public static DiscordCommand parse(ConfigurationSection section, SpicordConfig topConfig) {
        val command = section.get("label");
        val channel = ConfigurationSections.getStringCollection(section, "channel").getOrElse(emptyList());
        val message = section.getString("message", "");
        val permissions = getPermissions(section, "permission").getOrElse(emptySet());
        val roles = ConfigurationSections.getStringCollection(section, "roles").getOrElse(emptyList());
        val literals = new ArrayList<String>();
        if (command instanceof Collection) {
            val collection = ((Collection<?>) command);
            val strings = collection.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
            literals.addAll(strings);
        } else if (command != null) {
            literals.add(command.toString());
        }
        return DiscordCommand.of(literals, topConfig.remapChannel(channel), message, permissions, roles);
    }

    public static DiscordCommand of(Collection<String> literals, Collection<String> channelIds,
                                    String messageId, Collection<Permission> permissions, Collection<String> roles) {
        return new DiscordCommand(literals, new HashSet<>(channelIds), messageId, new HashSet<>(permissions), new HashSet<>(roles));
    }

    public static Option<Set<Permission>> getPermissions(ConfigurationSection section, String key) {
        return ConfigurationSections.getStringCollection(section, key)
                .map(c -> c.stream()
                        .map(name -> Try.of(() -> Permission.valueOf(name.toUpperCase())).toOption().getOrNull())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()));
    }

    public boolean isValidChannel(String channelId) {
        return channelIds.isEmpty() || channelIds.contains(channelId);
    }

    public boolean match(String literal) {
        return !literals.isEmpty() && literals.stream().anyMatch(literal::startsWith);
    }

    public boolean checkPermission(Member member) {
        if (permissions.isEmpty() || member.isOwner()) return true;
        val memberPerms = member.getPermissions();
        return permissions.stream().allMatch(permission -> memberPerms.contains(permission.getOffset()));
    }

    public boolean checkRole(Member member) {
        if (roles.isEmpty()) return true;
        val memberRoles = member.getRoles();
        return roles.stream().allMatch(role -> memberRoles.get(role) != null);
    }

    public boolean accessible(@Nullable Member member) {
        if (member == null) return false;
        return checkPermission(member) && checkRole(member);
    }

    public boolean match(Message message) {
        return isValidChannel(message.getChannelId())
                && match(message.getContents())
                && message.getMember().exists(this::accessible);
    }
}
