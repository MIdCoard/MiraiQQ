package top.focess.qq.core.commands.special;

import org.checkerframework.checker.nullness.qual.NonNull;
import top.focess.qq.api.command.Command;
import top.focess.qq.api.command.CommandSender;
import top.focess.qq.api.command.SpecialArgumentHandler;

public class PreviousCommandArgumentHandler implements SpecialArgumentHandler {

    @Override
    public @NonNull String handle(CommandSender sender, Command command, String[] args, int i) {
        return sender.getSession().getOrDefault("@previous_command", "");
    }
}
