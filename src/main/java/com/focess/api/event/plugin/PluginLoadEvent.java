package com.focess.api.event.plugin;

import com.focess.api.plugin.Plugin;
import com.focess.api.event.ListenerHandler;

/**
 * Called when a plugin is loaded
 */
public class PluginLoadEvent extends PluginEvent{

    private static final ListenerHandler LISTENER_HANDLER = new ListenerHandler();


    /**
     * Constructs a PluginLoadEvent
     *
     * @param plugin the plugin
     */
    public PluginLoadEvent(Plugin plugin) {
        super(plugin);
    }

}