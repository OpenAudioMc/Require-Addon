package com.craftmend.openaudiomc.addons.requieoa;

import com.craftmend.openaudiomc.api.interfaces.AudioApi;
import com.craftmend.openaudiomc.generic.networking.client.objects.player.ClientConnection;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public final class RequireOpenAudioAddon extends JavaPlugin implements Listener {

    private Set<Class<?>> supportedTypes = new HashSet<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        supportedTypes.add(PlayerEvent.class);
        supportedTypes.add(EntityEvent.class);

        int loaded = 0;
        for (String events : getConfig().getStringList("events")) {
            try {
                if (registerEvent((Class<? extends Event>) Class.forName(events))) {
                    loaded++;
                }
            } catch (ClassNotFoundException e) {
                getLogger().log(Level.SEVERE, "Couldn't register " + events + " because I couldn't find the file");
            }
        }
        getLogger().log(Level.INFO, "Hooked into " + loaded + " events");
    }

    private boolean shouldBeCanceled(Player player) {
        if (!AudioApi.getInstance().getClient(player.getUniqueId()).isConnected()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("message-when-canceled")));
            return true;
        }
        return false;
    }

    private boolean registerEvent(Class<? extends Event> event) {
        if (!supportedTypes.contains(event.getSuperclass())) {
            getLogger().log(Level.SEVERE, "Couldn't register " + event.getSimpleName() + " because it isn't a supported event.");
            return false;
        }

        // get the handler
        try {
            HandlerList list = (HandlerList) event.getDeclaredMethod("getHandlerList").invoke(null);
            list.register(new RegisteredListener(this, new EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) throws EventException {
                    if (event instanceof Cancellable) {
                        if (event instanceof PlayerEvent) {
                            if (shouldBeCanceled(((PlayerEvent) event).getPlayer())) {
                                ((Cancellable) event).setCancelled(true);
                            }
                        } else if (event instanceof EntityEvent) {
                            if (((EntityEvent) event).getEntity() instanceof Player) {
                                if (shouldBeCanceled((Player) ((EntityEvent) event).getEntity())) {
                                    ((Cancellable) event).setCancelled(true);
                                }
                            }
                        }
                    }
                }
            }, EventPriority.NORMAL, this, false));
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            getLogger().log(Level.SEVERE, "Couldn't register " + event.getSimpleName() + " because it doesn't have a valid handler list mehtod.");
            return false;
        }
        return true;
    }
}
