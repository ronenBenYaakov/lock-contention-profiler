package org.example.profiler.monitor;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ThreadSnapshot {

    private long threadId;
    private String threadName;
    private Thread.State threadState;
    private StackTraceElement[] stackTrace;
    private List<LockEvent> lockedMonitors;
    private List<LockEvent> lockedSynchronizers;
    private LockEvent lockWaitingOn;
    private long sampleTime;

    // Constructor
    public ThreadSnapshot(long threadId, String threadName, Thread.State threadState,
                          StackTraceElement[] stackTrace,
                          List<LockEvent> lockedMonitors,
                          List<LockEvent> lockedSynchronizers,
                          LockEvent lockWaitingOn,
                          long sampleTime) {
        this.threadId = threadId;
        this.threadName = threadName;
        this.threadState = threadState;
        this.stackTrace = stackTrace;
        this.lockedMonitors = lockedMonitors;
        this.lockedSynchronizers = lockedSynchronizers;
        this.lockWaitingOn = lockWaitingOn;
        this.sampleTime = sampleTime;
    }

    // Getters
    public long getThreadId() { return threadId; }
    public String getThreadName() { return threadName; }
    public Thread.State getThreadState() { return threadState; }
    public StackTraceElement[] getStackTrace() { return stackTrace; }
    public List<LockEvent> getLockedMonitors() { return lockedMonitors; }
    public List<LockEvent> getLockedSynchronizers() { return lockedSynchronizers; }
    public LockEvent getLockWaitingOn() { return lockWaitingOn; }
    public long getSampleTime() { return sampleTime; }

    // Setters
    public void setThreadId(long threadId) { this.threadId = threadId; }
    public void setThreadName(String threadName) { this.threadName = threadName; }
    public void setThreadState(Thread.State threadState) { this.threadState = threadState; }
    public void setStackTrace(StackTraceElement[] stackTrace) { this.stackTrace = stackTrace; }
    public void setLockedMonitors(List<LockEvent> lockedMonitors) { this.lockedMonitors = lockedMonitors; }
    public void setLockedSynchronizers(List<LockEvent> lockedSynchronizers) { this.lockedSynchronizers = lockedSynchronizers; }
    public void setLockWaitingOn(LockEvent lockWaitingOn) { this.lockWaitingOn = lockWaitingOn; }
    public void setSampleTime(long sampleTime) { this.sampleTime = sampleTime; }

    // Other methods
    public boolean isBlocked() {
        return threadState == Thread.State.BLOCKED;
    }

    public int getHeldLockCount() {
        int count = (lockedMonitors != null ? lockedMonitors.size() : 0)
                + (lockedSynchronizers != null ? lockedSynchronizers.size() : 0)
                + (lockWaitingOn != null ? 1 : 0);
        return count;
    }

    public static ThreadSnapshot from(ThreadInfo info, long sampleTime) {

        List<LockEvent> monitors = Arrays.stream(info.getLockedMonitors())
                .map(m -> new LockEvent(
                        m.getIdentityHashCode() + "",
                        m.getClassName(),
                        LockType.MONITOR,
                        info.getThreadId(),
                        info.getThreadName(),
                        LockEvent.getLockedStackTrace(info),
                        sampleTime,
                        false
                ))
                .toList();

        List<LockEvent> synchronizers = Arrays.stream(info.getLockedSynchronizers())
                .map(s -> new LockEvent(
                        s.getIdentityHashCode() + "",
                        s.getClassName(),
                        LockType.SYNCHRONIZER,
                        info.getThreadId(),
                        info.getThreadName(),
                        null,
                        sampleTime,
                        false
                ))
                .toList();

        LockEvent waitingOn = null;
        LockInfo lock = info.getLockInfo();

        if (lock != null) {
            waitingOn = new LockEvent(
                    lock.getIdentityHashCode() + "",
                    lock.getClassName(),
                    info.getThreadState() == Thread.State.BLOCKED
                            ? LockType.MONITOR
                            : LockType.SYNCHRONIZER,
                    info.getLockOwnerId(),
                    info.getLockOwnerName(),
                    info.getStackTrace(),
                    sampleTime,
                    true
            );
        }

        return new ThreadSnapshot(
                info.getThreadId(),
                info.getThreadName(),
                info.getThreadState(),
                info.getStackTrace(),
                monitors,
                synchronizers,
                waitingOn,
                sampleTime
        );
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
