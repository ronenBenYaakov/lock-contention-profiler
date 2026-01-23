package org.example.profiler.analysis;

import org.example.profiler.monitor.ThreadSnapshot;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

@Builder
@AllArgsConstructor
@Getter
@ToString
public class ThreadHistory {

    private long threadId;
    private String threadName;

    @Builder.Default
    private final List<ThreadSnapshot> snapshots = new ArrayList<>();

    // Explicit no-args constructor for Lombok builder safety
    public ThreadHistory() {
        this.snapshots = new ArrayList<>();
    }

    public ThreadHistory(long threadId, String threadName) {
        this.threadId = threadId;
        this.threadName = threadName;
        this.snapshots = new ArrayList<>();
    }

    public void addSnapshot(ThreadSnapshot snapshot) {
        snapshots.add(snapshot);
    }

    public long getTotalBlockedTime() {
        long blockedTime = 0;
        for (int i = 1; i < snapshots.size(); i++) {
            ThreadSnapshot prev = snapshots.get(i - 1);
            ThreadSnapshot curr = snapshots.get(i);
            if (prev.isBlocked()) {
                blockedTime += curr.getSampleTime() - prev.getSampleTime();
            }
        }
        return blockedTime;
    }

    public int getMaxHeldLocks() {
        return snapshots.stream()
                .mapToInt(ThreadSnapshot::getHeldLockCount)
                .max()
                .orElse(0);
    }
}
