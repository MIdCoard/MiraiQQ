package com.focess.core.commands;

import com.focess.Main;
import com.focess.api.Plugin;
import com.focess.api.command.Command;
import com.focess.api.command.CommandResult;
import com.focess.api.command.CommandSender;
import com.focess.api.command.converter.PluginDataConverter;
import com.focess.api.event.EventManager;
import com.focess.api.event.plugin.PluginUnloadEvent;
import com.focess.api.exceptions.EventSubmitException;
import com.focess.api.util.IOHandler;

public class UnloadCommand extends Command {
    public UnloadCommand() {
        super("unload");
    }

    @Override
    public void init() {
        this.setExecutorPermission(CommandSender::isConsole);
        this.addExecutor(1, (sender, data, ioHandler) -> {
            Plugin plugin = data.getPlugin();
            ioHandler.output("Unload " + plugin.getName());
            if (!(plugin.getClass().getClassLoader() instanceof LoadCommand.PluginClassLoader))
                Main.getLogger().debug("Plugin " + plugin.getName() + " is not loaded from PluginClassLoader");
            LoadCommand.disablePlugin(plugin);
            PluginUnloadEvent pluginUnloadEvent = new PluginUnloadEvent(plugin);
            try {
                EventManager.submit(pluginUnloadEvent);
            } catch (EventSubmitException e) {
                Main.getLogger().thr("Submit Plugin Unload Exception",e);
            }
            return CommandResult.ALLOW;
        }).setDataConverters(PluginDataConverter.PLUGIN_DATA_CONVERTER);
    }

    @Override
    public void usage(CommandSender sender, IOHandler ioHandler) {
        ioHandler.output("Use: unload <plugin>");
    }


}
