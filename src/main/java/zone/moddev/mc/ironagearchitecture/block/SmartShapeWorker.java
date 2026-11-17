package zone.moddev.mc.ironagearchitecture.block;

import java.util.concurrent.*;

/** Pure snapshot geometry only. World reads and commits stay on the server thread. */
public final class SmartShapeWorker {
    private static final ThreadPoolExecutor WORKER=new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,
        new ArrayBlockingQueue<Runnable>(8),task->{Thread t=new Thread(task,"Architecture shape worker");t.setDaemon(true);t.setPriority(Thread.NORM_PRIORITY-1);return t;},new ThreadPoolExecutor.AbortPolicy());
    static {WORKER.allowCoreThreadTimeOut(true);}
    private SmartShapeWorker() { }
    public static <T> Future<T> submit(Callable<T> job){try{return WORKER.submit(job);}catch(RejectedExecutionException busy){return null;}}
    public interface Steps<T> {boolean advance();T result();}
    /** Long roofs yield to queued walls instead of monopolising the geometry worker. */
    public static <T> Future<T> submitSteps(Callable<Steps<T>> factory) {
        CompletableFuture<T> future=new CompletableFuture<>();
        Runnable turn=new Runnable(){private Steps<T> job;public void run(){try{if(job==null)job=factory.call();for(;;){long deadline=System.nanoTime()+5000000L;do{if(future.isCancelled())return;if(job.advance()){future.complete(job.result());return;}}while(System.nanoTime()<deadline);if(WORKER.getQueue().offer(this))return;}}catch(Throwable failure){future.completeExceptionally(failure);}}};
        try{WORKER.execute(turn);return future;}catch(RejectedExecutionException busy){return null;}
    }
    public static <T> T completed(Future<T> job){try{return job.get();}catch(InterruptedException stopped){Thread.currentThread().interrupt();return null;}catch(CancellationException cancelled){return null;}catch(ExecutionException failed){throw new IllegalStateException("Smart geometry worker failed",failed.getCause());}}
}
