package org.example.profiler.agent;

import org.example.profiler.analysis.LockStats;
import org.example.profiler.monitor.ThreadSnapshot;

import java.util.List;

public record ProfilerStats(
        int totalThreads,
        int totalLocks,
        int blockedThreads,
        List<ThreadSnapshot> threads,
        List<LockStats> locks
) {}