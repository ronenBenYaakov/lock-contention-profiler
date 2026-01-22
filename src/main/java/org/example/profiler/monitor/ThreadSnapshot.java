package org.example.profiler.monitor;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public record ThreadSnapshot(long threadId, String threadName, Thread.State threadState, StackTraceElement[] stackTrace,
                             List<LockEvent> lockedMonitors, List<LockEvent> lockedSynchronizers,
                             LockEvent lockWaitingOn, long sampleTime) {

    public boolean isBlocked() {
        return threadState == Thread.State.BLOCKED;
    }

    public int getHeldLockCount() {
        return lockedMonitors.size() + lockedSynchronizers.size();
    }

    @Override
    public String toString() {
        return String.format("[%d:%s] state=%s heldLocks=%d waitingOn=%s",
                threadId,
                threadName,
                threadState,
                getHeldLockCount(),
                lockWaitingOn != null ? lockWaitingOn.getClass() : "none"
        );
    }
}
