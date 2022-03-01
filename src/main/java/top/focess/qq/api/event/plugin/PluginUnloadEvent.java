package top.focess.qq.api.event.plugin;

import top.focess.qq.api.plugin.Plugin;
import top.focess.qq.api.event.ListenerHandler;


/**
 * Called when a plugin is unloaded
 */
public class PluginUnloadEvent extends PluginEvent{

    private static final ListenerHandler LISTENER_HANDLER = new ListenerHandler();


    /**
     * Constructs a PluginUnloadEvent
     *
     * @param plugin the plugin
     */
    public PluginUnloadEvent(Plugin plugin) {
        super(plugin);
    }

}