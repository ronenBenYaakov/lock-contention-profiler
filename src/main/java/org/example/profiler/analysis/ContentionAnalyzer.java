package org.example.profiler.analysis;

import org.example.profiler.monitor.HotLockRecord;
import org.example.profiler.monitor.LockEvent;
import org.example.profiler.monitor.LockType;
import org.example.profiler.monitor.ThreadSnapshot;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ContentionAnalyzer {
    public List<ContentionRecord> analyzeLockContention(
            Map<Long, ThreadHistory> threadHistories
    ) {
        Map<String, LockContentionAccumulator> lockMap = new HashMap<>();

        for (ThreadHistory history : threadHistories.values()) {
            List<ThreadSnapshot> snapshots = history.getSnapshots();

            for (int i = 1; i < snapshots.size(); i++) {
                ThreadSnapshot prev = snapshots.get(i - 1);
                ThreadSnapshot curr = snapshots.get(i);

                if (!prev.isBlocked() || prev.lockWaitingOn() == null) continue;

                LockEvent lock = prev.lockWaitingOn();

                LockContentionAccumulator acc =
                        lockMap.computeIfAbsent(
                                lock.getLockId(),
                                id -> new LockContentionAccumulator(
                                        lock.getLockId(),
                                        lock.getLockName(),
                                        lock.getLockType()
                                )
                        );

                acc.recordBlock(lock, prev, curr);
            }
        }

        return lockMap.values().stream()
                .map(LockContentionAccumulator::toRecord)
                .sorted(Comparator.comparingLong(ContentionRecord::totalBlockedTime).reversed())
                .toList();
    }

    public List<HotLockRecord> analyzeHotLocks(Map<String, LockHistory> lockHistories, long observationWindowMs) {
        List<HotLockRecord> hotLocks = new ArrayList<>();

        for (LockHistory history : lockHistories.values()) {
            List<LockEvent> events = history.getEvents();
            if (events.isEmpty()) continue;

            Set<Long> blockedThreadIds = new HashSet<>();
            Set<String> blockedThreadNames = new HashSet<>();
            Map<Long, Integer> ownershipFrequency = new HashMap<>();
            Map<StackTraceKey, Integer> blockingStacks = new HashMap<>();

            long totalBlockedTime = 0;
            long maxSingleBlock = 0;
            int blockCount = 0;
            long lastTimestamp = 0;

            for (int i = 1; i < events.size(); i++) {
                LockEvent prev = events.get(i - 1);
                LockEvent curr = events.get(i);

                if (!prev.isContended()) continue;

                long delta = curr.getAcquiredTime() - prev.getAcquiredTime();
                totalBlockedTime += delta;
                maxSingleBlock = Math.max(maxSingleBlock, delta);
                blockCount++;
                lastTimestamp = Math.max(lastTimestamp, curr.getAcquiredTime());

                blockedThreadIds.add(prev.getOwnerThreadId());
                blockedThreadNames.add(prev.getOwnerThreadName());

                ownershipFrequency.merge(prev.getOwnerThreadId(), 1, Integer::sum);

                StackTraceKey key = new StackTraceKey(List.of(prev.getStackTrace()));
                blockingStacks.merge(key, 1, Integer::sum);
            }

            if (totalBlockedTime > 0) {
                long mainOwnerId = ownershipFrequency.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(-1L);

                String mainOwnerName = null; // Optional: can be fetched from history if needed

                HotLockRecord record = new HotLockRecord(
                        history.getLockId(),
                        history.getLockName(),
                        history.getLockType(),
                        mainOwnerId,
                        mainOwnerName,
                        List.copyOf(blockedThreadIds),
                        List.copyOf(blockedThreadNames),
                        blockCount,
                        blockedThreadIds.size(),
                        totalBlockedTime,
                        maxSingleBlock,
                        Map.copyOf(ownershipFrequency),
                        lastTimestamp,
                        Map.copyOf(blockingStacks),
                        observationWindowMs
                );

                hotLocks.add(record);
            }
        }

        hotLocks.sort(Comparator.comparingLong(HotLockRecord::totalBlockedTime).reversed());

        return hotLocks;
    }

    public static WFG buildWaitForGraph(List<ThreadSnapshot> snapshotBatch) {
        WFG wfg = new WFG();

        Map<Long, ThreadSnapshot> lastSnapshotMap = new HashMap<>();

        for (ThreadSnapshot snapshot : snapshotBatch) {
            long threadId = snapshot.threadId();
            wfg.addThread(threadId);

            LockEvent waitingLock = snapshot.lockWaitingOn();
            if (waitingLock != null && waitingLock.isOwned()) {
                long ownerThreadId = waitingLock.getOwnerThreadId();

                long durationMillis = 1;
                ThreadSnapshot prevSnapshot = lastSnapshotMap.get(threadId);
                if (prevSnapshot != null) {
                    durationMillis = Math.max(1, snapshot.sampleTime() - prevSnapshot.sampleTime());
                }

                wfg.addEdge(threadId, ownerThreadId, durationMillis);
            }

            lastSnapshotMap.put(threadId, snapshot);
        }

        return wfg;
    }

    public List<List<Long>> detectDeadBlocks(WFG wfg) {
        return wfg.getDeadBlocks();
    }

    public List<ContentionRecord> generateContentionRecords(List<ThreadSnapshot> snapshotBatch) {
    }

    public Map<String, Object> aggregateStatistics(Map<Long, ThreadHistory> threadHistories,
                                                   Map<String, LockHistory> lockHistories) {
    }

}