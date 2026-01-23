package org.example.profiler.analysis;

import org.example.profiler.monitor.LockEvent;
import org.example.profiler.monitor.LockType;
import org.example.profiler.monitor.ThreadSnapshot;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LockContentionAccumulator {

    private final String lockId;
    private final String lockName;
    private final LockType lockType;

    private final Set<Long> blockedThreadIds = new HashSet<>();
    private final Set<String> blockedThreadNames = new HashSet<>();
    private final Map<Long, Integer> ownershipFrequency = new HashMap<>();

    private long totalBlockedTime = 0;
    private long maxSingleBlock = 0;
    private int blockCount = 0;
    private long lastTimestamp = 0;

    private AtomicInteger maxOwnership = new AtomicInteger(0);          // updated dynamically
    private int uniqueWaiterCount = 0;     // updated dynamically

    private final Map<StackTraceKey, Integer> blockingStacks = new HashMap<>();

    public LockContentionAccumulator(String lockId, String lockName, LockType lockType) {
        this.lockId = lockId;
        this.lockName = lockName;
        this.lockType = lockType;
    }

    /**
     * Record a blocking event for this lock.
     * Updates all metrics incrementally so that isConvoy() is O(1)
     */
    public void recordBlock(LockEvent lock, ThreadSnapshot prev, ThreadSnapshot curr) {
        long delta = curr.getSampleTime() - prev.getSampleTime();

        // Update blocked threads info
        blockedThreadIds.add(prev.getThreadId());
        blockedThreadNames.add(prev.getThreadName());

        // Update timing metrics
        totalBlockedTime += delta;
        maxSingleBlock = Math.max(maxSingleBlock, delta);
        blockCount++;
        lastTimestamp = Math.max(lastTimestamp, curr.getSampleTime());

        int newOwnerCount = ownershipFrequency.merge(lock.getOwnerThreadId(), 1, Integer::sum);

        maxOwnership.set(Math.max(maxOwnership.intValue(), newOwnerCount));

        uniqueWaiterCount = blockedThreadIds.size();

        StackTraceKey key = new StackTraceKey(List.of(prev.getStackTrace()));
        blockingStacks.merge(key, 1, Integer::sum);
    }

    /**
     * Produce a ContentionRecord from the current accumulator state
     */
    public ContentionRecord toRecord() {
        long mainOwnerId = ownershipFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1L);

        String mainOwnerName = null; // optionally track owner names

        return new ContentionRecord(
                lockId,
                lockName,
                lockType,
                mainOwnerId,
                mainOwnerName,
                List.copyOf(blockedThreadIds),
                List.copyOf(blockedThreadNames),
                blockCount,
                uniqueWaiterCount,   // O(1)
                totalBlockedTime,
                maxSingleBlock,
                Map.copyOf(ownershipFrequency),
                maxOwnership,        // O(1)
                lastTimestamp,
                Map.copyOf(blockingStacks)
        );
    }

    /**
     * O(1) convoy detection based on dynamically maintained metrics
     */
    public boolean isConvoy() {
        return maxOwnership.get() > blockCount * 0.6 && uniqueWaiterCount > 3;
    }

    /**
     * Simple check if this lock has any contention
     */
    public boolean hasContention() {
        return blockCount > 0;
    }

}