package org.example.profiler.agent;

import org.example.profiler.analysis.ContentionAnalyzer;
import org.example.profiler.monitor.ThreadSnapshot;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ProfilerSampler {

    private static final ThreadMXBean mxBean =
            ManagementFactory.getThreadMXBean();

    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private static volatile ProfilerStats latestStats;

    public static void start() {
        mxBean.setThreadContentionMonitoringEnabled(true);

        scheduler.scheduleAtFixedRate(
                ProfilerSampler::sample,
                0, 1, TimeUnit.SECONDS
        );
    }

    private static void sample() {
        ThreadInfo[] infos = mxBean.dumpAllThreads(true, true);
        long sampleTime = System.currentTimeMillis(); // capture current sample time

        List<ThreadSnapshot> snapshots = new ArrayList<>();

        for (ThreadInfo info : infos) {
            snapshots.add(ThreadSnapshot.from(info, sampleTime)); // pass sampleTime
        }

        latestStats = ContentionAnalyzer.analyze(snapshots);
    }


    public static ProfilerStats getLatestStats() {
        return latestStats;
    }
}