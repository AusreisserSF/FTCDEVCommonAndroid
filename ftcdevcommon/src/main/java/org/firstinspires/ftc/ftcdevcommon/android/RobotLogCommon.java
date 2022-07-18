package org.firstinspires.ftc.ftcdevcommon.android;

import android.annotation.SuppressLint;
import android.util.Log;

import org.firstinspires.ftc.ftcdevcommon.Pair;
import org.firstinspires.ftc.ftcdevcommon.Threading;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

// Logging class that enqueues log records and writes them out in
// the background to the standard Java logger.

// This class supports multiple loggers, of which only one may be
// active at any given time. To prevent the reuse of a logger that
// that may not have been closed cleanly (due to a panic stop in
// FTC Autonomous, for example), this class opens a new Java
// Filehandler and log file with a unique name each time initialize()
// is called. This feature supports the typical FTC testing pattern
// by which Autonomous and/or TeleOp are run multiple times without
// a cold start of the Robot Controller.

// So why do we need these features? In testing 2021-2022 Freight
// Frenzy we noticed that when we ran TeleOp after Autonomous, which
// is the normal case, we sometimes saw that Gamepad 2 would stop
// responding to button clicks after TeleOp had run for some time.
// Gamepad 1, which was used for the drive train, never locked up.
// We narrowed the problem down to logging for three reasons: 1) no
// hangups occurred when we ran TeleOp alone (so there was no
// possibility of an unclosed Autonomous log), 2) the code for
// Gamepad 1 did not include any logging calls and 3) when a hangup
// did occur in TeleOp we discovered that there were no lines in
// the log file for TeleOp. Since TeleOp would run for some time
// before locking up we knew the problem was not likely to be the
// locks or the log entry queue (which has a capacity of
// Integer.MAX_VALUE) but rather the actual call to the Java logger
// to write out a record.

// What's different about this version of the class? First, variables
// for each logger are stored separately in an instance of the LogData
// class - unlike the previous logger, which reused the same variables
// for the Java logger, log entry queue and locks, and the
// CompletableFuture for the LogWriter. Second, this version ensures
// that each call to initialize() causes the logger to write to a
// different file. And third, on any hint of trouble, this class
// disallows the enqueueing of log entries and the writing of records
// to the log file.

//## Ported from the IntelliJ project IntelliJTestbed on 1/30/2022.
public class RobotLogCommon {

    private static final String TAG = "FTCRobotLog";
    private static final Level DEFAULT_LEVEL = Level.FINE;

    public enum OpenStatus {
        // The logger was initialized with an id of NONE or there was
        // an error during initialization.
        LOGGING_DISABLED,
        NEW_LOGGER_CREATED,
    }

    public enum LogIdentifier {AUTO_LOG, TELEOP_LOG, TEST_LOG, APP_LOG, NONE}

    private static final EnumMap<LogIdentifier, String> logFileBaseNames = new EnumMap<LogIdentifier, String>(LogIdentifier.class) {{
        put(LogIdentifier.AUTO_LOG, "FTCAutoLog_");
        put(LogIdentifier.TELEOP_LOG, "FTCTeleOpLog_");
        put(LogIdentifier.TEST_LOG, "TestLog_");
        put(LogIdentifier.APP_LOG, "AppLog_");
    }};

    private static LogIdentifier currentLogIdentifier = LogIdentifier.NONE;
    private static LogData currentLogData;

    // Wanted to use a MemoryHandler to log to a buffer but found out here --
    // https://chromium.googlesource.com/android_tools/+/refs/heads/master/sdk/sources/android-25/java/util/logging/MemoryHandler.java
    // that push() is synchronous with the linked FileHandler. So we'll use a BlockingQueue instead.
    public static synchronized OpenStatus initialize(LogIdentifier pIdentifier, String pLogDirPath) {

        Log.d(TAG, "Request to initialize logger " + pIdentifier);

        // We'll always create a new logger and a new log file so we don't have
        // to worry about the state of the current logger.
        currentLogIdentifier = LogIdentifier.NONE;
        currentLogData = null;

        // This is the same as not calling initialize() at all but may be useful
        // if you want to make logging configurable.
        if (pIdentifier == LogIdentifier.NONE) {
            Log.d(TAG, "For log id NONE no logger will be initialized");
            return OpenStatus.LOGGING_DISABLED;
        }

        // Now we can initialize the requested logger.
        Log.d(TAG, "Initializing the requested logger");
        OpenStatus openStatus;
        try {
            // Log file initialization is based on --
            //https://www.logicbig.com/tutorials/core-java-tutorial/logging/customizing-default-format.html
            // Get a timestamp and use it to make each logger unique. Use the same
            // timestamp in the log file name below.
            String dateTimeNow = TimeStamp.getDateTimeStamp(new Date());
            Logger logger = Logger.getLogger(pIdentifier.toString() + dateTimeNow);

            // System.setProperty("java.util.logging.config.file",
            // "Files/logging.properties");
            // OR
            // FileInputStream logProperties = new
            // FileInputStream("Files/logging.properties");
            // LogManager.getLogManager().readConfiguration(logProperties);

            // 11/27/2019 When I use either of the above methods to set the log file size
            // limit, the file count, and the append flag - only the append flag is
            // honored. A single log file is created that grows indefinitely. But if I
            // use the next line, everything works.
            String fullLogFilePath = pLogDirPath + logFileBaseNames.get(pIdentifier) + dateTimeNow + ".txt";
            FileHandler fileHandler = new FileHandler(fullLogFilePath, 1000000, 5, true);
            logger.setUseParentHandlers(false);
            fileHandler.setFormatter(new SimpleFormatter() {
                // original private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s
                // %n";
                // outputs [2019-08-29 13:46:11.496]
                private static final String format = "[%1$tF %1$tT.%1$tL] [%2$-7s] %3$s %n";

                // outputs August 29, 2019 1:37:10.810 PM
                // private static final String format = "[%1$tb %1$td, %1$tY
                // %1$tl:%1$tM:%1$tS.%1$tL %1$Tp] [%2$-7s] %3$s %n";

                @SuppressLint("DefaultLocale")
                @Override
                public synchronized String format(final LogRecord lr) {
                    return String.format(format, new Date(lr.getMillis()), lr.getLevel().getLocalizedName(),
                            lr.getMessage());
                }
            });

            logger.addHandler(fileHandler);
            logger.setLevel(DEFAULT_LEVEL);

            // Start a CompletableFuture for writing out the log.
            currentLogIdentifier = pIdentifier;
            currentLogData = new LogData(logger, fileHandler);

            // Controlled startup of the LogWriter thread.
            CountDownLatch logWriterLatch = new CountDownLatch(1);
            CompletableFuture<Void> logWriterFuture = Threading.launchAsync(new LogWriter(logWriterLatch));
            logWriterLatch.await(); // wait for the LogWriter to start
            currentLogData.logWriterFuture = logWriterFuture;

            openStatus = OpenStatus.NEW_LOGGER_CREATED;
            Log.d(TAG, "Requested logger up and running on file " + fullLogFilePath);
        } catch (Throwable throwable) {
            currentLogIdentifier = LogIdentifier.NONE;
            currentLogData = null;
            openStatus = OpenStatus.LOGGING_DISABLED;
            Log.d(TAG, "Error in logger initialization; logging is disabled");
        }

        return openStatus;
    }

    public static synchronized void setMostDetailedLogLevel(final Level pLogLevel) {
        if (currentLogIdentifier == LogIdentifier.NONE) {
            Log.d(TAG, "Attempt to set log level when logging is disabled");
            return;
        }

        currentLogData.logger.setLevel(pLogLevel);
        currentLogData.logLevel = pLogLevel;
    }

    public static synchronized Level getMostDetailedLogLevel() {
        if (currentLogIdentifier == LogIdentifier.NONE)
            return Level.OFF;
        return currentLogData.logger.getLevel();
    }

    public static void e(String pTAG, String pLogMessage) {
        enqueueLogEntry(Level.SEVERE, pTAG + " " + pLogMessage);
    }

    public static void c(String pTAG, String pLogMessage) {
        enqueueLogEntry(Level.CONFIG, pTAG + " " + pLogMessage);
    }

    public static void i(String pTAG, String pLogMessage) {
        enqueueLogEntry(Level.INFO, pTAG + " " + pLogMessage);
    }

    public static void d(String pTAG, String pLogMessage) {
        enqueueLogEntry(Level.FINE, pTAG + " " + pLogMessage);
    }

    public static void v(String pTAG, String pLogMessage) {
        enqueueLogEntry(Level.FINER, pTAG + " " + pLogMessage);
    }

    public static void vv(String pTAG, String pLogMessage) {
        enqueueLogEntry(Level.FINEST, pTAG + " " + pLogMessage);
    }

    // Even though the BlockingQueue is thread-safe there's a warning about drainTo:
    // "A failure encountered while attempting to add elements to collection c
    // [during the drainTo] may result in elements being in neither, either or both
    // collections when the associated exception is thrown."
    private static synchronized void enqueueLogEntry(Level pLevel, String pLogString) {
        if (currentLogIdentifier == LogIdentifier.NONE || currentLogData.logLevel == Level.OFF)
            return; // nothing to do

        // Don't enqueue if the log entry is more detailed than the current logging level.
        if (!currentLogData.logger.isLoggable(pLevel))
            return;

        // Enqueue the log entry for writing out to the log file.
        currentLogData.logWriterLock.lock();
        try {
            currentLogData.logEntryQueue.add(Pair.create(pLevel, pLogString));
            currentLogData.logWriterNotification = true;
            currentLogData.closeLogWriter = false;
            currentLogData.logWriterCondition.signal();
        } finally {
            currentLogData.logWriterLock.unlock();
        }
    }

    // Close the logger and the associated LogWriter thread.
    public static synchronized void closeLog() {
        if (currentLogIdentifier == LogIdentifier.NONE)
            return;

        // If the LogWriter is hanging onto the lock for some reason or it has already
        // exited because of an InterruptedException, make sure it can't be reused.
        if (!currentLogData.logWriterLock.tryLock() || currentLogData.logWriterFuture.isDone()) {
            currentLogIdentifier = LogIdentifier.NONE;
            currentLogData = null;
            return;
        }

        // Signal the LogWriter to exit.
        try {
            currentLogData.logWriterNotification = true;
            currentLogData.closeLogWriter = true;
            currentLogData.logWriterCondition.signal();
        } finally {
            currentLogData.logWriterLock.unlock();
        }

        // Check if the LogWriter has shut down cleanly.
        try {
            // Use a timeout value so that we never get hung up here.
            Threading.getFutureCompletion(currentLogData.logWriterFuture, 100);

            // After a clean shutdown we can remove all traces of the logger.
            Log.d(TAG, "LogWriter thread completed succssfully for " + currentLogIdentifier);
        } catch (Throwable t) {
            // If the CompletableFuture throws any exception at all there's really
            // nothing we can do about the underlying thread: from the Java documentation
            // for CompletableFuture --
            // "Since (unlike FutureTask) this class has no direct control over
            // the computation that causes it to be completed, cancellation is
            // treated as just another form of exceptional completion."
            Log.d(TAG, "Exception during shutdown of logger " + currentLogIdentifier);
            Log.d(TAG, "Error " + t);
        } finally {
            // Noticed that when testing Auto within TeleOp the FTC runtime
            // sends us down this path and that the underlying Java logger leaves
            // behind a .lck file. This is because the handler is not properly
            // closed. So do it here.
            /*
02-08 15:55:08.995   997  1379 D FTCRobotLog: Closing the log with 0 entries on the queue
02-08 15:55:08.995   997  1378 D FTCRobotLog: Exception during shutdown of logger TELEOP_LOG
02-08 15:55:08.995   997  1378 D FTCRobotLog: Error java.lang.InterruptedException
             */
            currentLogData.fileHandler.close();
            currentLogIdentifier = LogIdentifier.NONE;
            currentLogData = null;
        }
    }

    // Separate thread that writes log entries to the Java log file.
    private static class LogWriter implements Callable<Void> {
        private final CountDownLatch countDownLatch;

        public LogWriter(CountDownLatch pCountDownLatch) {
            countDownLatch = pCountDownLatch;
        }

        public Void call() {
            ArrayList<Pair<Level, String>> drain = new ArrayList<>();

            // Use a countdown latch to signal that the CompletableFuture is started.
            countDownLatch.countDown();
            while (true) {
                try {
                    Objects.requireNonNull(currentLogData.logWriterLock).lock();
                    while (!currentLogData.logWriterNotification)
                        Objects.requireNonNull(currentLogData.logWriterCondition).await();

                    currentLogData.logWriterNotification = false;

                    // If there is a request to close the LogWriter, write a maximum
                    // number of 10 entries to the log *inside* the lock.
                    if (currentLogData.closeLogWriter) {
                        Objects.requireNonNull(currentLogData.logEntryQueue).drainTo(drain); // must be protected by a lock
                        int drainCount = drain.size();
                        int drainIndex = 0;

                        Log.d(TAG, "Closing the log with " + drainCount + " entries on the queue");
                        Objects.requireNonNull(currentLogData.logger).log(Level.INFO, "Closing the log with " + drainCount + " entries on the queue");
                        if (drainCount > 10) {
                            Log.d(TAG, "Writing out the last 10 entries on the queue");
                            currentLogData.logger.log(Level.INFO, "Writing out the last 10 entries on the queue");
                            drainIndex = drainCount - 10;
                            drainCount = 10;
                        }

                        for (int i = 0; i < drainCount; i++, drainIndex++) {
                            currentLogData.logger.log(drain.get(drainIndex).first, drain.get(drainIndex).second);
                        }
                        break; // LogWriter will exit
                    }

                    // This is the normal path.
                    // Drain one or more entries from the queue to a local collection
                    // and then write them out *after* the lock is released.
                    Objects.requireNonNull(currentLogData.logEntryQueue).drainTo(drain); // must be protected by a lock
                } catch (InterruptedException iex) {  // await() can throw InterruptedException
                    break; // LogWriter will exit
                } finally {
                    Objects.requireNonNull(currentLogData.logWriterLock).unlock();
                }

                // We're *outside* the lock so more queue entries or a close request
                // may come in. But we won't see them until the following writes to
                // the log file have completed.
                for (Pair<Level, String> oneLogEntry : drain) {
                    Objects.requireNonNull(currentLogData.logger).log(oneLogEntry.first, oneLogEntry.second);
                }
                drain.clear();
            }

            return null;
        }
    }

    private static class LogData {
        public final Logger logger;
        public final FileHandler fileHandler;
        public Level logLevel = DEFAULT_LEVEL;
        public final LinkedBlockingQueue<Pair<Level, String>> logEntryQueue = new LinkedBlockingQueue<>();

        public CompletableFuture<Void> logWriterFuture;
        public final Lock logWriterLock = new ReentrantLock();
        public final Condition logWriterCondition = logWriterLock.newCondition();
        public boolean logWriterNotification = false; // protected by logQueueLock
        public boolean closeLogWriter = false; // protected by logQueueLock

        // Construct LogData for an active logger.
        public LogData(Logger pLogger, FileHandler pFileHandler) {
            logger = pLogger;
            fileHandler = pFileHandler;
        }
    }

}
