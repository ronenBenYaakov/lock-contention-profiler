package org.example.profiler.server;

import lombok.RequiredArgsConstructor;
import org.example.profiler.agent.ProfilerSampler;
import org.example.profiler.agent.ProfilerStats;
import org.example.profiler.monitor.ThreadSnapshot;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/profiler")
@RestController
@RequiredArgsConstructor
public class ProfilerController {

    private final SnapshotStore store;

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        List<ThreadSnapshot> snapshots = store.all();

        long totalThreads = snapshots.stream()
                .map(ThreadSnapshot::getThreadId)
                .distinct()
                .count();

        long blockedThreads = snapshots.stream()
                .filter(ThreadSnapshot::isBlocked)
                .count();

        long totalLocks = snapshots.stream()
                .mapToLong(ThreadSnapshot::getHeldLockCount)
                .sum();

        return Map.of(
                "timestamp", System.currentTimeMillis(),
                "totalThreads", totalThreads,
                "blockedThreads", blockedThreads,
                "totalLocks", totalLocks,
                "threads", snapshots
        );
    }
}