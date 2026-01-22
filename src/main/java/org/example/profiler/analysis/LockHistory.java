package org.example.profiler.analysis;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.example.profiler.monitor.LockEvent;
import org.example.profiler.monitor.LockType;

import java.util.ArrayList;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
@ToString
public class LockHistory {

    String lockId;
    String lockName;
    LockType lockType;

    @Builder.Default
    List<LockEvent> events = new ArrayList<>();

    long firstSeen;
    long lastSeen;

    @Builder.Default
    int maxContendedCount = 0;

    @Builder.Default
    int totalContentionEvents = 0;

    public void addEvent(LockEvent event) {
        events.add(event);
        long eventTime = event.getAcquiredTime();
        if (firstSeen == 0) {
            firstSeen = eventTime;
        }
        lastSeen = eventTime;

        if (event.isContended()) {
            maxContendedCount = Math.max(maxContendedCount, 1);
            totalContentionEvents++;
        }
    }

    public int getEventCount() {
        return events.size();
    }
}
