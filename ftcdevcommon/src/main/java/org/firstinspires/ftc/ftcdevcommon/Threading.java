package org.firstinspires.ftc.ftcdevcommon;

import java.io.IOException;
import java.util.concurrent.*;

public class Threading {

    // See https://stackoverflow.com/questions/43764036/how-to-convert-the-code-to-use-completablefuture
    // Use answer with 6 upvotes from Holger.

    //##!! 12/14/2021 Added the newSingleThreadExecutor below after getting
    // inconsistent results when attempting to run the drive train and the
    // Freight Frenzy elevator in separate threads in Autonomous. The typical
    // pattern was that the first run after a cold start of the robot would
    // complete the threads serially; subsequent runs without an intervening
    // cold start would run the threads in parallel. The addition of the
    // newSingleThreadExecutor resulted in parallel threads every time.
    // For thread cancellation see --
    // https://docs.oracle.com/javase/tutorial/essential/concurrency/interrupt.html
    // and
    // https://stackoverflow.com/questions/47597798/how-to-kill-completablefuture-related-threads
    // and
    // https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html#shutdownNow()
    public static <R> CompletableFuture<R> launchAsync(Callable<R> pCallable) {
        ExecutorService singleExecutorService = Executors.newSingleThreadExecutor(); //##!! CRUCIAL
        CompletableFuture<R> cf = new CompletableFuture<>();
        cf.whenComplete((x,y) -> singleExecutorService.shutdownNow());
        CompletableFuture.runAsync(() -> {
            try {
                cf.complete(pCallable.call());
            } catch (Throwable ex) {
                cf.completeExceptionally(ex);
            }
        }, singleExecutorService); //##!! CRUCIAL
        return cf;
    }

    // The description of the problem in
    // https://stackoverflow.com/questions/10437890/what-is-the-best-way-to-handle-an-executionexception
    // actually contains the best, if awkward, approach.
    public static <R> R getFutureCompletion(CompletableFuture<R> pCompletable) throws InterruptedException, IOException, TimeoutException {
        return getFutureCompletion(pCompletable, 0);
    }

    public static <R> R getFutureCompletion(CompletableFuture<R> pCompletable, int pTimeoutMs) throws InterruptedException, IOException, TimeoutException {
        R retVal = null;
        try {
            if (pTimeoutMs != 0)
                retVal = pCompletable.get(pTimeoutMs, TimeUnit.MILLISECONDS);
            else
                retVal = pCompletable.get(); // allows catch clause for CompletionException below (no compiler error)
        } catch (Throwable t) {
            handleFutureException(t);
        }
        return retVal;
    }

    //  Exceptions caught during the execution of a CompletableFuture are wrapped
    //  in an ExecutionException (if CompletableFuture.get() was called - our
    //  standard usage) or a CompletionException (if CompletableFuture.join() was
    //  called). In either case we must unwrap the exception and re-throw it as the
    //  original type.
    private static void handleFutureException(Throwable t) throws InterruptedException, IOException, TimeoutException {

        if ((t instanceof ExecutionException) || (t instanceof CompletionException)) {
            t = t.getCause(); // unwrap original exception
        }

        if (t instanceof CancellationException) {
            // Benign in our environment - part of a normal shutdown
            return;
        }

        if (t instanceof TimeoutException) {
            throw (TimeoutException) t;
        }

        if (t instanceof InterruptedException) {
            throw (InterruptedException) t;
        }

        if (t instanceof IOException) {
            throw (IOException) t;
        }

        if (t instanceof Error) {
            throw (Error) t;
        }

        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }

        throw new RuntimeException(t);
    }
}
