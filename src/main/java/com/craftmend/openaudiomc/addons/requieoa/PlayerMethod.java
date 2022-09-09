package com.craftmend.openaudiomc.addons.requieoa;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PlayerMethod {

    private boolean availible = false;
    private Method method = null;

    public PlayerMethod(Class clazz) {
        for (Method clazzMethod : clazz.getMethods()) {
            if (clazzMethod.getReturnType() == Player.class) {
                availible = true;
                method = clazzMethod;
                method.setAccessible(true);
                break;
            }
        }
    }

    public boolean isAvailible() {
        return availible;
    }

    public Player invokeGetter(Event event) {
        if (!isAvailible()) return null;
        try {
            return (Player) method.invoke(event);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
