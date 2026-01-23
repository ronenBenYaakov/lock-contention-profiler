package org.example.profiler.analysis;

import org.example.profiler.monitor.HotLockRecord;
import org.example.profiler.monitor.LockEvent;
import org.example.profiler.monitor.LockType;
import org.example.profiler.monitor.ThreadSnapshot;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class ContentionAnalyzer {
    private final ConcurrentHashMap<String, LockContentionAccumulator> lockMap = new ConcurrentHashMap<>();

    public List<ContentionRecord> analyzeLockContention(Map<Long, ThreadHistory> threadHistories) {
        Map<String, LockContentionAccumulator> lockMap = new HashMap<>();

        // 1️⃣ Build LockContentionAccumulators from snapshots
        for (ThreadHistory history : threadHistories.values()) {
            List<ThreadSnapshot> snapshots = history.getSnapshots();

            for (int i = 1; i < snapshots.size(); i++) {
                ThreadSnapshot prev = snapshots.get(i - 1);
                ThreadSnapshot curr = snapshots.get(i);

                if (!prev.isBlocked() || prev.getLockWaitingOn() == null) continue;

                LockEvent lock = prev.getLockWaitingOn();

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

        // 2️⃣ Use a max-heap to avoid full sort
        PriorityQueue<ContentionRecord> pq = new PriorityQueue<>(
                Comparator.comparingLong(ContentionRecord::totalBlockedTime)
        );

        for (LockContentionAccumulator acc : lockMap.values()) {
            ContentionRecord record = acc.toRecord();
            pq.offer(record);
        }

        // 3️⃣ Extract in descending order
        List<ContentionRecord> records = new ArrayList<>();
        while (!pq.isEmpty()) {
            records.add(pq.poll());
        }
        Collections.reverse(records); // largest totalBlockedTime first

        return records;
    }

    public List<HotLockRecord> analyzeHotLocks(Map<String, LockHistory> lockHistories, long observationWindowMs) {
        PriorityQueue<HotLockRecord> pq = new PriorityQueue<>(
                Comparator.comparingLong(HotLockRecord::totalBlockedTime)
        );

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

                String mainOwnerName = null;

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

                pq.offer(record);
            }
        }

        // Extract all records from heap in descending order
        List<HotLockRecord> hotLocks = new ArrayList<>();
        while (!pq.isEmpty()) {
            hotLocks.add(pq.poll());
        }
        Collections.reverse(hotLocks); // largest totalBlockedTime first

        return hotLocks;
    }

    public static WFG buildWaitForGraph(List<ThreadSnapshot> snapshotBatch) {
        WFG wfg = new WFG();

        Map<Long, ThreadSnapshot> lastSnapshotMap = new HashMap<>();

        for (ThreadSnapshot snapshot : snapshotBatch) {
            long threadId = snapshot.getThreadId();
            wfg.addThread(threadId);

            LockEvent waitingLock = snapshot.getLockWaitingOn();
            if (waitingLock != null && waitingLock.isOwned()) {
                long ownerThreadId = waitingLock.getOwnerThreadId();

                long durationMillis = 1;
                ThreadSnapshot prevSnapshot = lastSnapshotMap.get(threadId);
                if (prevSnapshot != null)
                    durationMillis = Math.max(1, snapshot.getSampleTime() - prevSnapshot.getSampleTime());

                wfg.addEdge(threadId, ownerThreadId, durationMillis);
            }

            lastSnapshotMap.put(threadId, snapshot);
        }

        return wfg;
    }

    public List<List<Long>> detectDeadBlocks(WFG wfg) {
        return wfg.getDeadBlocks();
    }

    public List<ContentionRecord> generateContentionRecords(List<ThreadSnapshot> snapshotBatch, int topK) {

        Map<Long, ThreadSnapshot> lastSnapshotPerThread = new HashMap<>();

        // 1️⃣ Process each snapshot
        for (ThreadSnapshot snapshot : snapshotBatch) {
            long threadId = snapshot.getThreadId();
            ThreadSnapshot prevSnapshot = lastSnapshotPerThread.get(threadId);
            lastSnapshotPerThread.put(threadId, snapshot);

            LockEvent waitLock = snapshot.getLockWaitingOn();
            if (waitLock != null && waitLock.isOwned()) {
                String lockId = waitLock.getLockId();

                LockContentionAccumulator acc = lockMap.computeIfAbsent(lockId, id ->
                        new LockContentionAccumulator(
                                waitLock.getLockId(),
                                waitLock.getLockName(),
                                waitLock.getLockType())
                );

                if (prevSnapshot != null) {
                    acc.recordBlock(waitLock, prevSnapshot, snapshot);
                } else {
                    acc.recordBlock(waitLock, snapshot, snapshot);
                }
            }
        }

        // 2️⃣ Use a max-heap (priority queue) to get top hot locks
        PriorityQueue<ContentionRecord> pq = new PriorityQueue<>(
                Comparator.comparingLong(ContentionRecord::totalBlockedTime)
        );

        for (LockContentionAccumulator acc : lockMap.values()) {
            ContentionRecord record = acc.toRecord();

            if (topK > 0) {
                pq.offer(record);
                if (pq.size() > topK) pq.poll(); // keep only top K
            } else {
                pq.offer(record);
            }
        }

        // 3️⃣ Extract from heap into a descending list
        List<ContentionRecord> records = new ArrayList<>();
        while (!pq.isEmpty()) {
            records.add(pq.poll());
        }
        Collections.reverse(records); // largest totalBlockedTime first

        return records;
    }

    public Map<String, Object> aggregateStatistics(
            Map<Long, ThreadHistory> threadHistories,
            Map<String, LockHistory> lockHistories
    ) {
        Map<String, Object> stats = new HashMap<>();

        // 1️⃣ Thread-level statistics
        long totalThreads = threadHistories.size();
        long totalBlockedThreads = 0;
        long totalBlockedTime = 0;
        long maxBlockedTime = 0;
        LongAdder totalBlockEvents = new LongAdder();

        // For hot threads
        PriorityQueue<Map.Entry<Long, Long>> hotThreads = new PriorityQueue<>(
                Comparator.comparingLong(Map.Entry::getValue)
        ); // min-heap

        // Compute thread stats
        for (ThreadHistory history : threadHistories.values()) {
            long threadBlockedTime = 0;
            List<ThreadSnapshot> snapshots = history.getSnapshots();
            for (int i = 1; i < snapshots.size(); i++) {
                ThreadSnapshot prev = snapshots.get(i - 1);
                ThreadSnapshot curr = snapshots.get(i);
                if (prev.isBlocked()) {
                    long delta = curr.getSampleTime() - prev.getSampleTime();
                    threadBlockedTime += delta;
                    totalBlockedTime += delta;
                    totalBlockEvents.increment();
                }
            }

            if (threadBlockedTime > 0) {
                totalBlockedThreads++;
                maxBlockedTime = Math.max(maxBlockedTime, threadBlockedTime);
                hotThreads.offer(Map.entry(history.getThreadId(), threadBlockedTime));
                if (hotThreads.size() > 5) hotThreads.poll(); // keep top 5
            }
        }

        // 2️⃣ Lock-level statistics
        long totalLocks = lockHistories.size();
        long totalContendedLocks = 0;
        long maxLockBlockedTime = 0;

        PriorityQueue<HotLockRecord> hotLocksHeap = new PriorityQueue<>(
                Comparator.comparingLong(HotLockRecord::totalBlockedTime)
        );

        for (LockHistory history : lockHistories.values()) {
            List<LockEvent> events = history.getEvents();
            long lockBlockedTime = 0;
            for (int i = 1; i < events.size(); i++) {
                LockEvent prev = events.get(i - 1);
                LockEvent curr = events.get(i);
                if (prev.isContended()) {
                    lockBlockedTime += curr.getAcquiredTime() - prev.getAcquiredTime();
                }
            }

            if (lockBlockedTime > 0) {
                totalContendedLocks++;
                maxLockBlockedTime = Math.max(maxLockBlockedTime, lockBlockedTime);

                HotLockRecord record = new HotLockRecord(
                        history.getLockId(),
                        history.getLockName(),
                        history.getLockType(),
                        -1L,
                        null,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        0, 0,
                        lockBlockedTime, lockBlockedTime,
                        Collections.emptyMap(),
                        0,
                        Collections.emptyMap(),
                        0
                );
                hotLocksHeap.offer(record);
                if (hotLocksHeap.size() > 5) hotLocksHeap.poll();
            }
        }

        // Extract top hot threads
        List<Map.Entry<Long, Long>> topHotThreads = new ArrayList<>();
        while (!hotThreads.isEmpty()) topHotThreads.add(hotThreads.poll());
        Collections.reverse(topHotThreads);

        // Extract top hot locks
        List<HotLockRecord> topHotLocks = new ArrayList<>();
        while (!hotLocksHeap.isEmpty()) topHotLocks.add(hotLocksHeap.poll());
        Collections.reverse(topHotLocks);

        // 3️⃣ Aggregate into stats map
        stats.put("totalThreads", totalThreads);
        stats.put("totalBlockedThreads", totalBlockedThreads);
        stats.put("totalBlockedTime", totalBlockedTime);
        stats.put("maxBlockedTimePerThread", maxBlockedTime);
        stats.put("totalBlockEvents", totalBlockEvents.sum());

        stats.put("totalLocks", totalLocks);
        stats.put("totalContendedLocks", totalContendedLocks);
        stats.put("maxBlockedTimePerLock", maxLockBlockedTime);

        stats.put("topHotThreads", topHotThreads);
        stats.put("topHotLocks", topHotLocks);

        return stats;
    }

}