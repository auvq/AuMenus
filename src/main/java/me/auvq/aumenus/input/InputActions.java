package me.auvq.aumenus.input;

import me.auvq.aumenus.action.Action;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class InputActions {

    private InputActions() {
    }

    static @NotNull String sanitize(@NotNull String input) {
        return MiniMessage.miniMessage().escapeTags(input).replace("\n", "").replace("\r", "");
    }

    static @NotNull List<Action> resolveInput(@NotNull List<Action> actions, @NotNull String inputText) {
        String sanitized = sanitize(inputText);
        return actions.stream()
                .map(action -> new Action(
                        action.getType(),
                        action.getValue().replace("{input}", sanitized),
                        action.getDelay(),
                        action.getChance()))
                .toList();
    }
}
