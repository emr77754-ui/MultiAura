package com.example.killaura;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;

public class KillAuraMod implements ClientModInitializer {
    public static KillAuraModule MODULE = new KillAuraModule();
    private boolean shiftPressed = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MODULE.tick(client);
            if (client.getWindow() == null) return;
            boolean shift = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            if (shift && !shiftPressed && client.currentScreen == null) {
                client.setScreen(new KillAuraScreen());
            }
            shiftPressed = shift;
        });
    }
}
