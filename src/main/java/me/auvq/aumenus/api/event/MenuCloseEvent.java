package me.auvq.aumenus.api.event;

import lombok.Getter;
import me.auvq.aumenus.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public final class MenuCloseEvent extends PlayerEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final Menu menu;

    public MenuCloseEvent(@NotNull Player player, @NotNull Menu menu) {
        super(player);
        this.menu = menu;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
