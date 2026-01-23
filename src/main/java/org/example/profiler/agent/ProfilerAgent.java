package org.example.profiler.agent;

import lombok.Getter;
import org.example.profiler.analysis.ContentionAnalyzer;
import org.example.profiler.analysis.ThreadHistory;
import org.example.profiler.monitor.ThreadSnapshot;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProfilerAgent {

    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static final ConcurrentMap<Long, ThreadHistory> threadHistories = new ConcurrentHashMap<>();
    @Getter
    private static final ContentionAnalyzer analyzer = new ContentionAnalyzer();

    private static final int SNAPSHOT_INTERVAL_MS = 50;

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[ProfilerAgent] Starting Lock Contention Profiler Agent...");

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(ProfilerAgent::collectSnapshots, 0, SNAPSHOT_INTERVAL_MS, TimeUnit.MILLISECONDS);

        // Optional: shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running.set(false);
            scheduler.shutdown();
            System.out.println("[ProfilerAgent] Profiler Agent stopped.");
        }));
    }

    private static void collectSnapshots() {
        if (!running.get()) return;

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] threadIds = threadMXBean.getAllThreadIds();
        ThreadInfo[] infos = threadMXBean.getThreadInfo(threadIds, Integer.MAX_VALUE);

        List<ThreadSnapshot> snapshotBatch = new ArrayList<>();

        for (ThreadInfo info : infos) {
            if (info == null) continue;

            long tid = info.getThreadId();
            String tname = info.getThreadName();
            Thread.State state = info.getThreadState();
            StackTraceElement[] stackTrace = info.getStackTrace();

            ThreadSnapshot snapshot = new ThreadSnapshot(
                    tid,
                    tname,
                    state,
                    stackTrace,
                    Collections.emptyList(), // lockedMonitors
                    Collections.emptyList(), // lockedSynchronizers
                    null,                    // lockWaitingOn (can be set with instrumentation)
                    Instant.now().toEpochMilli()
            );

            snapshotBatch.add(snapshot);

            threadHistories.computeIfAbsent(tid, id -> new ThreadHistory(tid, info.getThreadName()))
                    .addSnapshot(snapshot);
        }

        // Optional: feed the batch to your analyzer immediately
        // e.g., List<ContentionRecord> records = analyzer.generateContentionRecords(snapshotBatch, 10);
    }

    public static Map<Long, ThreadHistory> getThreadHistories() {
        return Collections.unmodifiableMap(threadHistories);
    }

}
