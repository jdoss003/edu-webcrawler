package com.bdj.eduwebcrawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueuedWorkerPool<T>
{
    private volatile boolean finished = false;
    private ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();
    private List<Thread> workers = new ArrayList<>();
    private ThreadGroup group;
    private Throwable threadError;

    public QueuedWorkerPool(String groupName, int numWorkers, Runnable job)
    {
        this.group = new ThreadGroup(groupName);
        for (int i = 0; i < numWorkers; ++i)
        {
            Thread t = new Thread(this.group, job);
            t.setUncaughtExceptionHandler((thread, e) -> this.threadError = e);
            this.workers.add(t);
        }
    }

    public void add(T item)
    {
        this.queue.add(item);
    }

    public Optional<T> getNextItem()
    {
        return Optional.ofNullable(this.queue.poll());
    }

    public boolean isEmpty()
    {
        return this.queue.isEmpty();
    }

    public boolean contains(T item)
    {
        return this.queue.contains(item);
    }

    public void start()
    {
        this.workers.forEach(Thread::start);
    }

    public boolean isFinished()
    {
        return this.finished;
    }

    public void finish()
    {
        this.finished = true;
        this.workers.forEach(QueuedWorkerPool::joinThread);
    }

    public void checkThreadStates()
    {
        this.workers.forEach(t ->
        {
            if (t.getState() == Thread.State.TERMINATED || threadError != null)
            {
                throw new IllegalStateException("Thread in group " + group.getName() + " caught an exception!", threadError);
            }
        });
    }

    // Not to be called unless we are crashing
    @SuppressWarnings("deprecation")
    public void stop()
    {
        this.group.stop();
    }

    private static void joinThread(Thread t)
    {
        try
        {
            t.join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
