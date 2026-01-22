package org.example.profiler.analysis;

import org.example.profiler.monitor.LockType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public record ContentionRecord(
        String lockId,
        String lockName,
        LockType lockType,

        long owningThreadId,
        String owningThreadName,

        List<Long> blockedThreadIds,
        List<String> blockedThreadNames,

        int blockCount,
        int uniqueWaiterCount,

        long totalBlockedTime,
        long maxSingleBlock,

        Map<Long, Integer> ownershipFrequency,
        AtomicInteger maxOwnership,
        long lastTimestamp,

        Map<StackTraceKey, Integer> blockingStacks
) {

    public ContentionRecord {
        if (ownershipFrequency != null && !ownershipFrequency.isEmpty()) {
            maxOwnership.set(ownershipFrequency.values().stream()
                    .max(Integer::compare)
                    .orElse(0));
        } else {
            maxOwnership.set(0);
        }
    }

    public boolean hasContention() {
        return blockCount > 0;
    }

    public double severityScore() {
        if (blockCount == 0) return 0.0;

        double avgBlock = (double) totalBlockedTime / blockCount;
        double ownerInstability = ownershipFrequency.size();

        return totalBlockedTime
                * Math.log(blockCount + 1)
                * Math.log(ownerInstability + 1)
                / (maxSingleBlock + 1);
    }

    public boolean isConvoy() {
        return maxOwnership.get() > blockCount * 0.6
                && uniqueWaiterCount > 3;
    }

    @Override
    public String toString() {
        return String.format(
                "Lock[%s:%s] owner=%d blockedThreads=%d totalBlockedTime=%d maxBlock=%d convoy=%b",
                lockType,
                lockName,
                owningThreadId,
                uniqueWaiterCount,
                totalBlockedTime,
                maxSingleBlock,
                isConvoy()
        );
    }
}
