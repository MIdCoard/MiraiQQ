package com.focess.core.bot;

import com.focess.Main;
import com.focess.api.bot.Bot;
import com.focess.api.bot.BotManager;
import com.focess.api.event.EventManager;
import com.focess.api.event.bot.BotLoginEvent;
import com.focess.api.event.bot.BotLogoutEvent;
import com.focess.api.event.bot.BotReloginEvent;
import com.focess.api.event.bot.FriendInputStatusEvent;
import com.focess.api.event.chat.FriendChatEvent;
import com.focess.api.event.chat.GroupChatEvent;
import com.focess.api.event.recall.FriendRecallEvent;
import com.focess.api.event.recall.GroupRecallEvent;
import com.focess.api.event.request.FriendRequestEvent;
import com.focess.api.event.request.GroupRequestEvent;
import com.focess.api.exceptions.BotLoginException;
import com.focess.api.exceptions.EventSubmitException;
import com.focess.api.util.IOHandler;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import kotlin.coroutines.Continuation;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.event.Listener;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.utils.BotConfiguration;
import net.mamoe.mirai.utils.LoginSolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.stream.FileImageOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

public class SimpleBotManager implements BotManager {

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(2);


    private static final List<Listener<?>> LISTENER_LIST = Lists.newArrayList();

    private static final Map<Long,Bot> BOTS = Maps.newHashMap();

    public SimpleBotManager() {
        if (Main.getBotManager() != null) {
            Main.getLogger().fatal("Run more that one BotManager. Force shutdown!");
            Main.exit();
        }
    }

    @Override
    public @NotNull Future<Bot> login(long id, String password) {
        return EXECUTOR.submit(() -> loginDirectly(id,password));
    }

    @Override
    public @NotNull Bot loginDirectly(long id, String password) {
        BotConfiguration configuration = BotConfiguration.getDefault();
        configuration.setProtocol(BotConfiguration.MiraiProtocol.ANDROID_PAD);
        configuration.fileBasedDeviceInfo("devices/" + id + "/device.json");
        configuration.setCacheDir(new File("devices/" + id + "/cache"));
        configuration.setLoginSolver(new LoginSolver() {
            @Nullable
            @Override
            public Object onSolvePicCaptcha(@NotNull net.mamoe.mirai.Bot bot, byte[] bytes, @NotNull Continuation<? super String> continuation) {
                try {
                    FileImageOutputStream outputStream = new FileImageOutputStream(new File("captcha.jpg"));
                    outputStream.write(bytes);
                    outputStream.close();
                } catch (IOException e) {
                    Main.getLogger().thr("CAPTCHA Picture Load Exception",e);
                }
                Main.getLogger().info("Please input CAPTCHA for " + bot.getId() + ": ");
                return IOHandler.getConsoleIoHandler().input();
            }

            @Nullable
            @Override
            public Object onSolveSliderCaptcha(@NotNull net.mamoe.mirai.Bot bot, @NotNull String s, @NotNull Continuation<? super String> continuation) {
                Main.getLogger().info(s);
                IOHandler.getConsoleIoHandler().input();
                return null;
            }

            @Nullable
            @Override
            public Object onSolveUnsafeDeviceLoginVerify(@NotNull net.mamoe.mirai.Bot bot, @NotNull String s, @NotNull Continuation<? super String> continuation) {
                Main.getLogger().info(s);
                IOHandler.getConsoleIoHandler().input();
                return null;
            }
        });
        net.mamoe.mirai.Bot bot = BotFactory.INSTANCE.newBot(id, password, configuration);
        Bot b = new SimpleBot(id,password, bot);
        try {
            EventManager.submit(new BotLoginEvent(b));
        } catch (EventSubmitException e) {
            Main.getLogger().thr("Submit Bot Login Exception",e);
        }
        try {
            bot.login();
        } catch(Exception e) {
            throw new BotLoginException(id);
        }
        LISTENER_LIST.add(bot.getEventChannel().subscribeAlways(GroupMessageEvent.class, event -> {
            GroupChatEvent e = new GroupChatEvent(b,event.getSender(), event.getMessage(),event.getSource());
            try {
                EventManager.submit(e);
            } catch (EventSubmitException eventSubmitException) {
                Main.getLogger().thr("Submit Group Message Exception",eventSubmitException);
            }
        }));
        LISTENER_LIST.add(bot.getEventChannel().subscribeAlways(FriendMessageEvent.class, event -> {
            FriendChatEvent e = new FriendChatEvent(b,event.getFriend(), event.getMessage(),event.getSource());
            try {
                EventManager.submit(e);
            } catch (EventSubmitException eventSubmitException) {
                Main.getLogger().thr("Submit Friend Message Exception",eventSubmitException);
            }
        }));
        LISTENER_LIST.add(bot.getEventChannel().subscribeAlways(MessageRecallEvent.GroupRecall.class, event -> {
            GroupRecallEvent e = new GroupRecallEvent(b,event.getAuthor(),event.getMessageIds(),event.getOperator());
            try {
                EventManager.submit(e);
            } catch (EventSubmitException ex) {
                Main.getLogger().thr("Submit Group Recall Exception",ex);
            }
        }));
        LISTENER_LIST.add(bot.getEventChannel().subscribeAlways(MessageRecallEvent.FriendRecall.class, event -> {
            FriendRecallEvent e = new FriendRecallEvent(b,event.getAuthor(),event.getMessageIds());
            try {
                EventManager.submit(e);
            } catch (EventSubmitException ex) {
                Main.getLogger().thr("Submit Friend Recall Exception",ex);
            }
        }));
        LISTENER_LIST.add(bot.getEventChannel().subscribeAlways(NewFriendRequestEvent.class, event ->{
            FriendRequestEvent e = new FriendRequestEvent(b,event.getFromId(),event.getFromNick(),event.getFromGroup(),event.getMessage());
            try {
                EventManager.submit(e);
            } catch (EventSubmitException ex) {
                Main.getLogger().thr("Submit Friend Request Exception",ex);
            }
            if (e.getAccept() != null)
                if (e.getAccept())
                    event.accept();
                else event.reject(e.isBlackList());
        }));
        LISTENER_LIST.add(bot.getEventChannel().subscribeAlways(BotInvitedJoinGroupRequestEvent.class, event->{
            GroupRequestEvent e = new GroupRequestEvent(b,event.getGroupId(),event.getGroupName(),event.getInvitor());
            try {
                EventManager.submit(e);
            } catch (EventSubmitException ex) {
                Main.getLogger().thr("Submit Group Request Exception",ex);
            }
            if (e.getAccept() != null)
                if (e.getAccept())
                    event.accept();
                else event.ignore();
        }));
        LISTENER_LIST.add(bot.getEventChannel().subscribeAlways(FriendInputStatusChangedEvent.class,event->{
            FriendInputStatusEvent e = new FriendInputStatusEvent(b,event.getFriend(), event.getInputting());
            try {
                EventManager.submit(e);
            } catch (EventSubmitException ex) {
                Main.getLogger().thr("Submit Friend Input Status Exception",ex);
            }
        }));
        BOTS.put(id,b);
        return b;
    }

    @Override
    public boolean login(Bot bot) {
        if (!bot.isOnline()) {
            bot.getNativeBot().login();
            try {
                EventManager.submit(new BotLoginEvent(bot));
            } catch (EventSubmitException e) {
                Main.getLogger().thr("Submit Bot Login Exception",e);
            }
            return true;
        }
        return false;
    }


    @Override
    public boolean logout(@NotNull Bot bot) {
        if (!bot.isOnline())
            return false;
        bot.getNativeBot().close();
        try {
            EventManager.submit(new BotLogoutEvent(bot));
        } catch (EventSubmitException e) {
            Main.getLogger().thr("Submit Bot Logout Exception",e);
        }
        return true;
    }

    @Override
    public @Nullable Bot getBot(long username) {
        return BOTS.get(username);
    }

    @Override
    public boolean relogin(@NotNull Bot bot) {
        boolean ret = this.login(bot) & this.logout(bot);
        try {
            EventManager.submit(new BotReloginEvent(bot));
        } catch (EventSubmitException e) {
            Main.getLogger().thr("Submit Bot Relogin Exception",e);
        }
        return ret;
    }

    public static void disableAllBotsAndExit() {
        for (Listener<?> listener : LISTENER_LIST)
            listener.complete();
        for (Bot bot : BOTS.values())
            bot.logout();
    }


}
