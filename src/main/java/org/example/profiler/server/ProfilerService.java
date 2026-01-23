package org.example.profiler.server;

import org.example.profiler.monitor.LockEvent;
import org.example.profiler.monitor.LockType;
import org.example.profiler.monitor.ThreadSnapshot;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ProfilerService {

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    public List<ThreadSnapshot> getThreadSnapshots() {
        ThreadInfo[] infos = threadMXBean.dumpAllThreads(true, true);
        List<ThreadSnapshot> snapshots = new ArrayList<>();

        for (ThreadInfo info : infos) {
            // Create lock events for monitors
            List<LockEvent> monitors = Arrays.stream(info.getLockedMonitors())
                    .map(m -> new LockEvent(
                            m.getClassName() + "@" + m.getIdentityHashCode(),
                            m.getClassName(),
                            LockType.MONITOR,
                            info.getThreadId(),
                            info.getThreadName(),
                            new StackTraceElement[]{m.getLockedStackFrame()},
                            System.currentTimeMillis(),
                            false
                    ))
                    .toList();

            // Create lock events for synchronizers
            List<LockEvent> synchronizers = Arrays.stream(info.getLockedSynchronizers())
                    .map(l -> new LockEvent(
                            l.getClass().getName() + "@" + l.hashCode(),
                            l.getClass().getName(),
                            LockType.SYNCHRONIZER,
                            info.getThreadId(),
                            info.getThreadName(),
                            new StackTraceElement[0],
                            System.currentTimeMillis(),
                            false
                    ))
                    .toList();

            // Lock the thread is waiting on (if any)
            LockEvent waitingLock = null;
            if (info.getLockName() != null) {
                waitingLock = new LockEvent(
                        info.getLockName(),
                        info.getLockName(),
                        LockType.SYNCHRONIZER, // assume SYNCHRONIZER
                        info.getLockOwnerId(),
                        info.getLockOwnerName(),
                        new StackTraceElement[0],
                        System.currentTimeMillis(),
                        info.getThreadState() == Thread.State.BLOCKED
                );
            }

            ThreadSnapshot snapshot = new ThreadSnapshot(
                    info.getThreadId(),
                    info.getThreadName(),
                    info.getThreadState(),
                    info.getStackTrace(),
                    monitors,
                    synchronizers,
                    waitingLock,
                    System.currentTimeMillis()
            );

            snapshots.add(snapshot);
        }

        return snapshots;
    }
}
