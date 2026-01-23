package org.example.profiler.server;

import java.util.List;

public class ProfilerSnapshot {
    private List<ProfilerThread> threads;
    private List<ProfilerLock> locks;
    private long timestamp;

    // Getters / Setters
    public List<ProfilerThread> getThreads() { return threads; }
    public void setThreads(List<ProfilerThread> threads) { this.threads = threads; }
    public List<ProfilerLock> getLocks() { return locks; }
    public void setLocks(List<ProfilerLock> locks) { this.locks = locks; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

class ProfilerThread {
    private long id;
    private String name;
    private String state;
    private long blockedTime;
    private String lockName;

    // Getters / Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public long getBlockedTime() { return blockedTime; }
    public void setBlockedTime(long blockedTime) { this.blockedTime = blockedTime; }
    public String getLockName() { return lockName; }
    public void setLockName(String lockName) { this.lockName = lockName; }
}

class ProfilerLock {
    private String lockId;
    private String lockType;
    private int contentionCount;

    // Getters / Setters
    public String getLockId() { return lockId; }
    public void setLockId(String lockId) { this.lockId = lockId; }
    public String getLockType() { return lockType; }
    public void setLockType(String lockType) { this.lockType = lockType; }
    public int getContentionCount() { return contentionCount; }
    public void setContentionCount(int contentionCount) { this.contentionCount = contentionCount; }
}
