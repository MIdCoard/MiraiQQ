package top.focess.qq.core.schedule;

import top.focess.qq.api.plugin.Plugin;
import top.focess.qq.api.schedule.Scheduler;
import top.focess.qq.api.schedule.Task;

import java.time.Duration;
import java.util.UUID;

public class FocessTask implements Task, ITask {

    private final Runnable runnable;
    private final Scheduler scheduler;
    private final String name;
    private Duration period;
    private boolean isRunning = false;
    private boolean isPeriod = false;
    private boolean isFinished = false;
    private ComparableTask nativeTask;

    FocessTask(Runnable runnable, Scheduler scheduler) {
        this.runnable = runnable;
        this.scheduler = scheduler;
        this.name = scheduler.getName() + "-" + UUID.randomUUID().toString().substring(0,8);
    }

    FocessTask(Runnable runnable, Duration period,Scheduler scheduler) {
        this(runnable,scheduler);
        this.isPeriod = true;
        this.period = period;
    }

    @Override
    public void setNativeTask(ComparableTask nativeTask) {
        this.nativeTask = nativeTask;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.nativeTask.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public Plugin getPlugin() {
        return this.scheduler.getPlugin();
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isPeriod() {
        return this.isPeriod;
    }

    @Override
    public boolean isFinished() {
        return !this.isPeriod && this.isFinished;
    }

    @Override
    public boolean isCancelled() {
        return this.nativeTask.isCancelled();
    }

    @Override
    public void run() {
        this.isRunning = false;
        this.runnable.run();
        this.isRunning = true;
        this.isFinished = true;
    }

    @Override
    public Duration getPeriod() {
        return this.period;
    }
}