package com.craftmend.openaudiomc.addons.requieoa;

import com.craftmend.openaudiomc.api.interfaces.AudioApi;
import com.craftmend.openaudiomc.api.interfaces.Client;
import com.craftmend.openaudiomc.generic.client.objects.ClientConnection;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public final class RequireOpenAudioAddon extends JavaPlugin implements Listener {

    private Map<Class, PlayerMethod> playerMethodCache = new HashMap<>();
    private Set<Class<?>> supportedTypes = new HashSet<>();
    private boolean requireVc = false;

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

        requireVc = getConfig().getBoolean("require-voice-chat");
    }

    private boolean shouldBeCanceled(Player player) {
        Client client = AudioApi.getInstance().getClient(player.getUniqueId());
        if (client == null) {
            // wait for oa to init
            return true;
        }

        if (requireVc) {
            ClientConnection cc = (ClientConnection) client;
            if (cc.getDataCache() == null) {
                // still loading
                return true;
            }

            if (!cc.getRtcSessionManager().isReady()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("message-when-canceled")));
                return true;
            }
        } else {
            if (!client.isConnected()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("message-when-canceled")));
                return true;
            }
        }


        return false;
    }

    private boolean registerEvent(Class<? extends Event> event) {
        if (!supportedTypes.contains(event.getSuperclass())) {
            // check if we can handle it with reflection regardless
            PlayerMethod playerMethod = new PlayerMethod(event);

            if (playerMethod.isAvailible()) {
                playerMethodCache.put(event, playerMethod);
            } else {
                getLogger().log(Level.SEVERE, "Couldn't register " + event.getSimpleName() + " because it isn't a supported event.");
                return false;
            }
        }

        // get the handler
        try {
            HandlerList list = (HandlerList) event.getDeclaredMethod("getHandlerList").invoke(null);
            list.register(new RegisteredListener(this, (listener, event1) -> {

                if (event1 instanceof PlayerCommandPreprocessEvent) {
                    if (((PlayerCommandPreprocessEvent) event1).getMessage().equals("/audio")) {
                        // ignore
                        return;
                    }
                }

                if (event1 instanceof Cancellable) {
                    if (event1 instanceof PlayerEvent) {
                        if (shouldBeCanceled(((PlayerEvent) event1).getPlayer())) {
                            ((Cancellable) event1).setCancelled(true);
                        }
                    } else if (event1 instanceof EntityEvent) {
                        if (((EntityEvent) event1).getEntity() instanceof Player) {
                            if (shouldBeCanceled((Player) ((EntityEvent) event1).getEntity())) {
                                ((Cancellable) event1).setCancelled(true);
                            }
                        }
                    } else {
                        // use fallback using the method cache
                        PlayerMethod playerMethod = playerMethodCache.get(event1.getClass());
                        if (playerMethod == null) return;

                        Player player = playerMethod.invokeGetter(event1);
                        if (shouldBeCanceled(player)) {
                            ((Cancellable) event1).setCancelled(true);
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
