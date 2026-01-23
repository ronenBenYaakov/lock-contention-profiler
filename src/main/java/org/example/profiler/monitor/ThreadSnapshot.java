package org.example.profiler.monitor;

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
