package top.focess.qq.core.schedule;

import top.focess.qq.api.plugin.Plugin;
import top.focess.qq.api.schedule.Scheduler;
import top.focess.qq.api.schedule.Task;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class FocessTask implements Task, ITask {

    private final Runnable runnable;
    private final Scheduler scheduler;
    private final String name;
    private Duration period;
    protected boolean isRunning = false;
    private boolean isPeriod = false;
    protected boolean isFinished = false;
    private ComparableTask nativeTask;
    protected ExecutionException exception;

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
    public synchronized void clear() {
        this.isFinished = false;
        this.isRunning = false;
    }

    @Override
    public synchronized void startRun() {
        this.isRunning = true;
    }

    @Override
    public synchronized void endRun() {
        this.isRunning = false;
        this.isFinished = true;
        this.notifyAll();
    }

    @Override
    public void setException(ExecutionException e) {
        this.exception = e;
    }

    @Override
    public boolean isSingleThread() {
        return this.scheduler instanceof ThreadPoolScheduler;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.nativeTask.cancel(mayInterruptIfRunning);
    }

    @Override
    public synchronized boolean isRunning() {
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
    public synchronized boolean isFinished() {
        return !this.isPeriod && this.isFinished;
    }

    @Override
    public boolean isCancelled() {
        return this.nativeTask.isCancelled();
    }

    @Override
    public synchronized void join() throws InterruptedException, CancellationException {
        if (this.isFinished())
            return;
        if (this.isCancelled())
            throw new CancellationException();
        this.wait();
        if (this.isCancelled())
            throw new CancellationException();
    }

    @Override
    public void run() throws ExecutionException {
        this.runnable.run();
    }

    @Override
    public Duration getPeriod() {
        return this.period;
    }
}
