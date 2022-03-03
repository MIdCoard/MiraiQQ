package top.focess.qq.core.schedule;

import org.jetbrains.annotations.NotNull;
import top.focess.qq.FocessQQ;
import top.focess.qq.api.exceptions.TaskNotFinishedException;
import top.focess.qq.api.plugin.Plugin;
import top.focess.qq.api.schedule.Callback;
import top.focess.qq.api.schedule.Scheduler;
import top.focess.qq.api.schedule.Schedulers;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

public class FocessCallback<V> implements Callback<V>, ITask {

    private final static Scheduler DEFAULT_SCHEDULER = Schedulers.newFocessScheduler(FocessQQ.getMainPlugin());

    private final Callable<V> callback;
    private final Scheduler scheduler;
    private final String name;
    private boolean isRunning = false;
    private volatile boolean isFinished = false;
    private ComparableTask nativeTask;
    private V value;

    FocessCallback(Callable<V> callback, Scheduler scheduler) {
        this.callback = callback;
        this.scheduler = scheduler;
        this.name = scheduler.getName() + "-" + UUID.randomUUID().toString().substring(0,8);
    }

    @Override
    public V call() {
        if (this.isCancelled())
            throw new CancellationException();
        if (!this.isFinished)
            throw new TaskNotFinishedException(this);
        return value;
    }

    @Override
    public V get(long timeout, @NotNull TimeUnit unit) {
        return DEFAULT_SCHEDULER.submit(this::get,Duration.ofMillis(unit.toMillis(timeout))).get();
    }

    @Override
    public boolean isFinished() {
        return this.isFinished;
    }

    @Override
    public boolean isCancelled() {
        return this.nativeTask.isCancelled();
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
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
        return false;
    }

    @Override
    public boolean isSingleThread() {
        return this.scheduler instanceof ThreadPoolScheduler;
    }

    @Override
    public void run() {
        this.isRunning = true;
        try {
            value = this.callback.call();
        } catch (Exception e) {
            value = null;
        }
        this.isRunning = false;
        this.isFinished = true;
    }

    @Override
    public Duration getPeriod() {
        return null;
    }

    @Override
    public void setNativeTask(ComparableTask task) {
        this.nativeTask = task;
    }

    @Override
    public void clear() {
        this.isRunning = false;
        this.isFinished = false;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.nativeTask.cancel(mayInterruptIfRunning);
    }
}
