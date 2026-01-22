package org.example.profiler.analysis;

import org.example.profiler.monitor.ThreadSnapshot;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;

@Builder
@AllArgsConstructor
@Getter
@ToString
public class ThreadHistory {

    private long threadId;
    private String threadName;

    @Builder.Default
    private List<ThreadSnapshot> snapshots = new ArrayList<>();

    public void addSnapshot(ThreadSnapshot snapshot) {
        snapshots.add(snapshot);
    }

    public long getTotalBlockedTime() {
        long blockedTime = 0;
        for (int i = 1; i < snapshots.size(); i++) {
            ThreadSnapshot prev = snapshots.get(i - 1);
            ThreadSnapshot curr = snapshots.get(i);
            if (prev.isBlocked()) {
                blockedTime += curr.sampleTime() - prev.sampleTime();
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
