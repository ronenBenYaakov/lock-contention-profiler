package org.example.profiler.analysis;

import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class WFG {
    private final Map<Long, Set<Long>> adjacencyList = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Long>> waitTimes = new ConcurrentHashMap<>();
    private final Map<Long, Long> totalWaitTimeCache = new ConcurrentHashMap<>();
    private Set<Long> ownerThreadsCache = new ConcurrentHashMap<>().newKeySet();

    private long indexCounter = 0;
    private final Map<Long, Long> indices = new HashMap<>();
    private final Map<Long, Long> lowlinks = new HashMap<>();
    private final Deque<Long> stack = new ArrayDeque<>();
    private final Set<Long> onStack = new HashSet<>();
    private final List<List<Long>> sccs = new ArrayList<>();

    private final List<List<Long>> deadBlockCache = new ArrayList<>();
    public boolean deadBlockCacheValid = false;

    public void addThread(long threadId) {
        adjacencyList.putIfAbsent(threadId, ConcurrentHashMap.newKeySet());
        waitTimes.putIfAbsent(threadId, new ConcurrentHashMap<>());
        deadBlockCacheValid = false;
    }

    public void addEdge(long srcThreadId, long tarThreadId, long durationMillis) {
        addThread(srcThreadId);
        addThread(tarThreadId);

        adjacencyList.get(srcThreadId).add(tarThreadId);
        waitTimes.get(srcThreadId).merge(tarThreadId, durationMillis, Long::sum);
        totalWaitTimeCache.merge(srcThreadId, durationMillis, Long::sum);
        ownerThreadsCache.add(tarThreadId);
        deadBlockCacheValid = false;
    }

    public void removeThread(long threadId) {
        adjacencyList.remove(threadId);
        waitTimes.remove(threadId);
        totalWaitTimeCache.remove(threadId);

        for (Set<Long> targets : adjacencyList.values()) {
            targets.remove(threadId);
        }

        for (Map<Long, Long> ownerMap : waitTimes.values()) {
            ownerMap.remove(threadId);
        }

        ownerThreadsCache.clear();
        adjacencyList.values().forEach(ownerThreadsCache::addAll);
        deadBlockCacheValid = false;
    }

    public List<List<Long>> detectCycles() {
        indices.clear();
        lowlinks.clear();
        stack.clear();
        onStack.clear();
        sccs.clear();
        indexCounter = 0;

        for (Long node : adjacencyList.keySet()) {
            if (!indices.containsKey(node)) {
                strongConnect(node);
            }
        }

        List<List<Long>> cycles = new ArrayList<>();
        for (List<Long> scc : sccs) {
            if (scc.size() > 1) {
                cycles.add(scc);
            }
        }

        return cycles;
    }

    private void strongConnect(Long v) {
        indices.put(v, indexCounter);
        lowlinks.put(v, indexCounter);
        indexCounter++;
        stack.push(v);
        onStack.add(v);

        for (Long w : adjacencyList.getOrDefault(v, Collections.emptySet())) {
            if (!indices.containsKey(w)) {
                strongConnect(w);
                lowlinks.put(v, Math.min(lowlinks.get(v), lowlinks.get(w)));
            } else if (onStack.contains(w)) {
                lowlinks.put(v, Math.min(lowlinks.get(v), indices.get(w)));
            }
        }

        if (lowlinks.get(v).equals(indices.get(v))) {
            List<Long> scc = new ArrayList<>();
            Long w;
            do {
                w = stack.pop();
                onStack.remove(w);
                scc.add(w);
            } while (!w.equals(v));
            sccs.add(scc);
        }
    }

    public boolean hasDeadlock() {
        return !detectCycles().isEmpty();
    }

    public Set<Long> getBlockedThreads() {
        Set<Long> blocked = new HashSet<>();
        for (Map.Entry<Long, Set<Long>> entry : adjacencyList.entrySet()) {
            if (!entry.getValue().isEmpty()) blocked.add(entry.getKey());
        }
        return blocked;
    }

    public Set<Long> getOwnerThreads() {
        return ownerThreadsCache;
    }

    public long getWaitTime(long srcThreadId, long tarThreadId) {
        return waitTimes.getOrDefault(srcThreadId, Map.of())
                .getOrDefault(tarThreadId, 0L);
    }

    public long getTotalWaitTime(long threadId) {
        return totalWaitTimeCache.getOrDefault(threadId, 0L);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WaitForGraph:\n");
        for (Map.Entry<Long, Set<Long>> entry : adjacencyList.entrySet()) {
            sb.append("Thread ").append(entry.getKey())
                    .append(" waits on ").append(entry.getValue());
            if (waitTimes.containsKey(entry.getKey())) {
                sb.append(" with times=").append(waitTimes.get(entry.getKey()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public void invalidateCaches() {
        deadBlockCacheValid = false;
    }

    public List<List<Long>> getDeadBlocks() {
        if (deadBlockCacheValid) return deadBlockCache;

        deadBlockCache.clear();
        Set<Long> visited = new HashSet<>();

        for (Long threadId : adjacencyList.keySet()) {
            if (!visited.contains(threadId)) {
                List<Long> chain = new ArrayList<>();
                exploreBlockedChain(threadId, visited, chain);
                if (!chain.isEmpty()) deadBlockCache.add(chain);
            }
        }

        deadBlockCacheValid = true;
        return deadBlockCache;
    }

    private void exploreBlockedChain(Long threadId, Set<Long> visited, List<Long> chain) {
        if (visited.contains(threadId)) return;
        visited.add(threadId);

        Set<Long> waitingOn = adjacencyList.getOrDefault(threadId, Collections.emptySet());
        if (!waitingOn.isEmpty()) {
            for (Long target : waitingOn) {
                chain.add(threadId);
                exploreBlockedChain(target, visited, chain);
            }
        } else {
            chain.add(threadId);
        }
    }
}
