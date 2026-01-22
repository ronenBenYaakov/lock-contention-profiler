package org.example.profiler.monitor;

import org.example.profiler.analysis.StackTraceKey;

import java.util.List;
import java.util.Map;

public abstract class LockRecord {

    protected final String lockId;
    protected final String lockName;
    protected final LockType lockType;

    protected final long mainOwnerId;
    protected final String mainOwnerName;

    protected final List<Long> blockedThreadIds;
    protected final List<String> blockedThreadNames;

    protected final int blockCount;
    protected final int uniqueWaiterCount;

    protected final long totalBlockedTime;
    protected final long maxSingleBlock;

    protected final Map<Long, Integer> ownershipFrequency;
    protected final long lastTimestamp;

    protected final Map<StackTraceKey, Integer> blockingStacks;

    protected LockRecord(
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
            Map<StackTraceKey, Integer> blockingStacks
    ) {
        this.lockId = lockId;
        this.lockName = lockName;
        this.lockType = lockType;
        this.mainOwnerId = mainOwnerId;
        this.mainOwnerName = mainOwnerName;
        this.blockedThreadIds = blockedThreadIds;
        this.blockedThreadNames = blockedThreadNames;
        this.blockCount = blockCount;
        this.uniqueWaiterCount = uniqueWaiterCount;
        this.totalBlockedTime = totalBlockedTime;
        this.maxSingleBlock = maxSingleBlock;
        this.ownershipFrequency = ownershipFrequency;
        this.lastTimestamp = lastTimestamp;
        this.blockingStacks = blockingStacks;
    }

    // ---------------------
    // Common utility methods
    // ---------------------

    public boolean hasContention() {
        return blockCount > 0;
    }

    public double severityScore() {
        if (blockCount == 0) return 0.0;
        double ownerInstability = ownershipFrequency.size();
        return totalBlockedTime
                * Math.log(blockCount + 1)
                * Math.log(ownerInstability + 1)
                / (maxSingleBlock + 1);
    }

    public boolean isConvoy() {
        if (ownershipFrequency.isEmpty()) return false;

        int maxOwnership = ownershipFrequency.values().stream()
                .max(Integer::compare)
                .orElse(0);

        return maxOwnership > blockCount * 0.6
                && uniqueWaiterCount > 3;
    }

    @Override
    public String toString() {
        return String.format(
                "Lock[%s:%s] owner=%d blockedThreads=%d totalBlockedTime=%d maxBlock=%d convoy=%b",
                lockType,
                lockName,
                mainOwnerId,
                uniqueWaiterCount,
                totalBlockedTime,
                maxSingleBlock,
                isConvoy()
        );
    }

    public String lockId() { return lockId; }
    public String lockName() { return lockName; }
    public LockType lockType() { return lockType; }
    public long mainOwnerId() { return mainOwnerId; }
    public String mainOwnerName() { return mainOwnerName; }
    public List<Long> blockedThreadIds() { return blockedThreadIds; }
    public List<String> blockedThreadNames() { return blockedThreadNames; }
    public int blockCount() { return blockCount; }
    public int uniqueWaiterCount() { return uniqueWaiterCount; }
    public long totalBlockedTime() { return totalBlockedTime; }
    public long maxSingleBlock() { return maxSingleBlock; }
    public Map<Long, Integer> ownershipFrequency() { return ownershipFrequency; }
    public long lastTimestamp() { return lastTimestamp; }
    public Map<StackTraceKey, Integer> blockingStacks() { return blockingStacks; }
}