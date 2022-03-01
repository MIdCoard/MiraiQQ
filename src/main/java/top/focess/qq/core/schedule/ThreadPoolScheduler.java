package top.focess.qq.core.schedule;

import com.google.common.collect.Queues;
import top.focess.qq.Main;
import top.focess.qq.api.exceptions.SchedulerClosedException;
import top.focess.qq.api.plugin.Plugin;
import top.focess.qq.api.schedule.Callback;
import top.focess.qq.api.schedule.Scheduler;
import top.focess.qq.api.schedule.Task;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.Callable;

public class ThreadPoolScheduler implements Scheduler {
    private final Plugin plugin;

    private final Queue<ComparableTask> tasks = Queues.newPriorityBlockingQueue();

    private final ThreadPoolSchedulerThread[] threads;

    private boolean shouldStop = false;

    private int currentThread = 0;

    public ThreadPoolScheduler(Plugin plugin, int poolSize) {
        this.plugin = plugin;
        this.threads = new ThreadPoolSchedulerThread[poolSize];
        for (int i = 0; i < poolSize; i++) {
            threads[i] = new ThreadPoolSchedulerThread(this.getName() + "-" + i);
            threads[i].start();
        }
        new SchedulerThread(this.getName()).start();
    }

    @Override
    public Task run(Runnable runnable, Duration delay) {
        synchronized (tasks) {
            if (this.shouldStop)
                throw new SchedulerClosedException(this);
            FocessTask task = new FocessTask(runnable, this);
            tasks.add(new ComparableTask(System.currentTimeMillis() + delay.toMillis(), task));
            this.tasks.notify();
            return task;
        }
    }

    @Override
    public Task runTimer(Runnable runnable, Duration delay, Duration period) {
        synchronized (tasks) {
            if (this.shouldStop)
                throw new SchedulerClosedException(this);
            FocessTask task = new FocessTask(runnable, period, this);
            tasks.add(new ComparableTask(System.currentTimeMillis() + delay.toMillis(), task));
            this.tasks.notify();
            return task;
        }
    }

    @Override
    public <V> Callback<V> submit(Callable<V> callable, Duration delay) {
        synchronized (tasks) {
            if (this.shouldStop)
                throw new SchedulerClosedException(this);
            FocessCallback<V> callback = new FocessCallback<>(callable, this);
            tasks.add(new ComparableTask(System.currentTimeMillis() + delay.toMillis(), callback));
            this.tasks.notify();
            return callback;
        }
    }

    @Override
    public void cancelAll() {
        synchronized (tasks) {
            this.tasks.clear();
        }
    }

    @Override
    public String getName() {
        return this.getPlugin().getName() + "-ThreadPoolScheduler";
    }

    @Override
    public Plugin getPlugin() {
        return this.plugin;
    }

    @Override
    public void close() {
        this.shouldStop = true;
        cancelAll();
        for (ThreadPoolSchedulerThread thread : this.threads)
            thread.close();
    }

    private class SchedulerThread extends Thread {

        public SchedulerThread(String name) {
            super(name);
        }

        private ThreadPoolSchedulerThread getAvailableThread() {
            for (int i = 1;i <= threads.length;i++) {
                int next = (currentThread + i) % threads.length;
                if (threads[next].isAvailable()) {
                    currentThread = next;
                    return threads[next];
                }
            }
            return null;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    synchronized (tasks) {
                        if (shouldStop)
                            break;
                        if (tasks.isEmpty())
                            tasks.wait();
                        ComparableTask task = tasks.peek();
                        if (task != null)
                            synchronized (task) {
                                if (task.isCancelled()) {
                                    tasks.poll();
                                    continue;
                                }
                                if (task.getTime() <= System.currentTimeMillis()) {
                                    ThreadPoolSchedulerThread thread = getAvailableThread();
                                    if (thread == null)
                                        continue;
                                    tasks.poll();
                                    thread.startTask(task.getTask());
                                    if (task.getTask().isPeriod()) {
                                        tasks.add(new ComparableTask(System.currentTimeMillis() + task.getTask().getPeriod().toMillis(), task.getTask()));
                                    }
                                }
                            }
                    }
                } catch (Exception e) {
                    Main.getLogger().thrLang("exception-thread-pool-scheduler",e);
                }
            }
        }
    }
}