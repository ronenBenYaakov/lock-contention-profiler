package org.example.profiler.monitor;

import org.example.profiler.analysis.StackTraceKey;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HotLockRecord extends LockRecord {

    private final double normalizedBlockedRatio;
    private final List<StackTraceKey> topHotStacks;

    /**
     * Constructs a HotLockRecord from aggregated lock metrics.
     *
     * @param lockId             unique lock identifier
     * @param lockName           human-readable lock name
     * @param lockType           type of the lock
     * @param mainOwnerId        thread ID that owns the lock most frequently
     * @param mainOwnerName      thread name of the main owner
     * @param blockedThreadIds   list of threads that were blocked on this lock
     * @param blockedThreadNames list of thread names blocked
     * @param blockCount         total number of block events
     * @param uniqueWaiterCount  number of unique threads blocked
     * @param totalBlockedTime   total time threads spent blocked
     * @param maxSingleBlock     longest single block duration
     * @param ownershipFrequency map of thread ID → acquisition count
     * @param lastTimestamp      last time lock was blocked
     * @param blockingStacks     stack traces that caused contention
     * @param observationWindowMs observation window for normalization
     */
    public HotLockRecord(
            String lockId,
            String lockName,
            LockType lockType,
            long mainOwnerId,
            String mainOwnerName,
            List<Long> blockedThreadIds,
            List<String> blockedThreadNames,
            int blockCount,
            int uniqueWaiterCount,
            long totalBlockedTime,
            long maxSingleBlock,
            Map<Long, Integer> ownershipFrequency,
            long lastTimestamp,
            Map<StackTraceKey, Integer> blockingStacks,
            long observationWindowMs
    ) {
        super(lockId, lockName, lockType,
                mainOwnerId, mainOwnerName,
                blockedThreadIds, blockedThreadNames,
                blockCount, uniqueWaiterCount,
                totalBlockedTime, maxSingleBlock,
                ownershipFrequency, lastTimestamp,
                blockingStacks);

        // Precompute normalized blocked ratio for fast system-level analytics
        this.normalizedBlockedRatio = (double) totalBlockedTime / Math.max(1, observationWindowMs);

        // Precompute top 5 hot stack traces
        this.topHotStacks = blockingStacks.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /** @return normalized blocked ratio in [0,1] */
    public double normalizedBlockedRatio() {
        return normalizedBlockedRatio;
    }

    /** @return top hot stack traces causing lock contention */
    public List<StackTraceKey> topHotStacks() {
        return topHotStacks;
    }

    @Override
    public String toString() {
        return String.format(
                "HotLock[%s:%s] owner=%d blockedThreads=%d totalBlockedTime=%d maxBlock=%d convoy=%b normalized=%.2f topStacks=%d",
                lockType,
                lockName,
                mainOwnerId,
                uniqueWaiterCount,
                totalBlockedTime,
                maxSingleBlock,
                isConvoy(),
                normalizedBlockedRatio,
                topHotStacks.size()
        );
    }
}
