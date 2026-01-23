package org.example.profiler.server;

import org.springframework.stereotype.Service;

import java.lang.management.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfilerService {

    private final ThreadMXBean threadMXBean;

    public ProfilerService() {
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.threadMXBean.setThreadContentionMonitoringEnabled(true);
    }

    public ProfilerSnapshot getSnapshot() {
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);

        List<ProfilerThread> threads = Arrays.stream(threadInfos)
                .map(ti -> {
                    ProfilerThread thread = new ProfilerThread();
                    thread.setId(ti.getThreadId());
                    thread.setName(ti.getThreadName());
                    thread.setState(ti.getThreadState().name());
                    thread.setBlockedTime(ti.getBlockedTime());
                    thread.setLockName(ti.getLockName());
                    return thread;
                })
                .collect(Collectors.toList());

        Map<String, ProfilerLock> locks = new HashMap<>();

        for (ThreadInfo ti : threadInfos) {
            if (ti.getLockName() != null) {
                locks.computeIfAbsent(ti.getLockName(), k -> {
                    ProfilerLock l = new ProfilerLock();
                    l.setLockId(k);
                    l.setLockType(ti.getLockInfo() != null ? ti.getLockInfo().getClassName() : "MONITOR");
                    l.setContentionCount(0);
                    return l;
                }).setContentionCount(
                        locks.get(ti.getLockName()).getContentionCount() + 1
                );
            }
        }

        ProfilerSnapshot snapshot = new ProfilerSnapshot();
        snapshot.setThreads(threads);
        snapshot.setLocks(new ArrayList<>(locks.values()));
        snapshot.setTimestamp(System.currentTimeMillis());

        return snapshot;
    }
}
