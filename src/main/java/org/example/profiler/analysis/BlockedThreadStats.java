package org.example.profiler.analysis;

import org.example.profiler.monitor.ThreadSnapshot;

public record BlockedThreadStats(
        long threadId,
        String threadName,
        long totalBlockedTime,
        int blockCount,
        long maxSingleBlock,
        ThreadSnapshot lastSnapshot
) {}
