Lock Contention Profiler - Feature Summary
==========================================

1. Core Data Classes
-------------------

ThreadSnapshot:
- threadId, threadName, threadState
- stackTrace
- lockedMonitors, lockedSynchronizers
- lockWaitingOn
- sampleTime
Methods:
- isBlocked()
- getHeldLockCount()
- toString()

LockEvent:
- lockId, lockName, lockType
- ownerThreadId, ownerThreadName
- stackTrace
- acquiredTime
- isContended
Methods:
- isOwned()
- toString()

ContentionRecord:
- lockId, lockName, lockType
- owningThreadId, owningThreadName
- blockedThreadIds, blockedThreadNames
- blockCount, uniqueWaiterCount
- totalBlockedTime, maxSingleBlock
- ownershipFrequency, maxOwnership
- lastTimestamp, blockingStacks
Methods:
- hasContention()
- severityScore()
- isConvoy()
- toString()

LockContentionAccumulator:
- Tracks ongoing lock contention
- Fields: blockedThreadIds, blockedThreadNames, ownershipFrequency, totalBlockedTime, maxSingleBlock, blockCount, lastTimestamp, blockingStacks
- Methods:
  - recordBlock(LockEvent lock, ThreadSnapshot prev, ThreadSnapshot curr)
  - toRecord()

HotLockRecord (extends LockRecord):
- Fields: normalizedBlockedRatio, topHotStacks
- toString() includes convoy status, normalized ratio, top stacks

LockRecord (abstract):
- Base class for lock records

2. Analytics & Monitoring Classes
---------------------------------

ContentionAnalyzer:
- Methods: analyzeBlockedThreads, analyzeLockContention, analyzeHotLocks
- Features: dynamic maxOwnership, convoy detection, sorted outputs

WaitForGraph (WFG):
- Fields: adjacencyList, waitTimes, totalWaitTimeCache, ownerThreadsCache
- Methods: addThread, addEdge, removeThread, getTotalWaitTime, getWaitTime, getOwnerThreads, detectCycles, toString
- Optimizations: O(1) queries using caches

WaitForGraphBuilder:
- buildWaitForGraph(List<ThreadSnapshot>)
- Computes real wait durations using consecutive snapshots

3. LockHotness / Wave Analytics
--------------------------------
LockHotnessWave:
- Represents each lock as A * sin(ownershipFrequency * x)
- Sum of waves for system-wide lock hotness visualization
- Used for hot lock detection and convoy prediction

4. Optimization Features
------------------------
- O(1) getTotalWaitTime & getWaitTime
- O(1) getOwnerThreads
- Dynamic maxOwnership
- Real-time convoy detection (optional hooks)
- Tarjan SCC for deadlock detection
- HotLock analysis with normalized blocked ratios and top stack traces

5. Project Dependencies
-----------------------
- Lombok
- Guava (EventBus)
- Jedis/Redis (optional for convoy cache)

6. Thread / Lock Histories
--------------------------
- ThreadHistory: stores snapshots
- LockHistory: stores events
- Used by analyzers for blocked threads, lock contention, hot locks, convoy metrics

7. Summary of Algorithmic Improvements
--------------------------------------
Feature | Old Complexity | Optimized Complexity
--------|----------------|--------------------
getTotalWaitTime(thread) | O(D) | O(1)
getWaitTime(threadA, threadB) | O(1) | O(1)
getOwnerThreads() | O(E) | O(1)
isConvoy() | O(N per lock) | O(1) (dynamic maxOwnership)
Deadlock detection | DFS per query | Tarjan SCC, O(V+E)
Record block updates | basic accumulation | maxOwnership updated dynamically, hooks optional

8. Pending / Optional Features
-------------------------------
- Hook system to trigger convoy updates in Redis cache
- LockHotnessWave aggregation for real-time visualization
- Time-window normalization for hot lock metrics
