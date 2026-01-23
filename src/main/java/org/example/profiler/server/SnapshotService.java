package org.example.profiler.server;

import org.example.profiler.monitor.LockEvent;
import org.example.profiler.monitor.LockType;
import org.example.profiler.monitor.ThreadSnapshot;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

public class SnapshotService {

    public static List<ThreadSnapshot> getCurrentSnapshots() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        threadMXBean.setThreadContentionMonitoringEnabled(true);
        ThreadInfo[] infos = threadMXBean.dumpAllThreads(true, true);

        List<ThreadSnapshot> snapshots = new ArrayList<>();
        for (ThreadInfo info : infos) {
            LockEvent waitingLock = null;

            if (info.getLockName() != null) {
                // Determine lock type from thread state
                LockType lockType = info.getThreadState() == Thread.State.BLOCKED ? LockType.MONITOR : LockType.SYNCHRONIZER;

                waitingLock = new LockEvent(
                        info.getLockName(),                 // lockId
                        info.getLockName(),                 // lockName
                        lockType,                           // lockType
                        info.getLockOwnerId(),              // ownerThreadId (-1 if none)
                        info.getLockOwnerName(),            // ownerThreadName
                        new StackTraceElement[0],           // stackTrace (can add info.getStackTrace())
                        System.currentTimeMillis(),         // acquiredTime
                        false                               // isContended (initially false)
                );
            }

            ThreadSnapshot snapshot = new ThreadSnapshot(
                    info.getThreadId(),
                    info.getThreadName(),
                    info.getThreadState(),
                    info.getStackTrace() != null ? info.getStackTrace() : new StackTraceElement[0],
                    List.of(),        // lockedMonitors
                    List.of(),        // lockedSynchronizers
                    waitingLock,      // lockWaitingOn
                    System.currentTimeMillis()
            );

            snapshots.add(snapshot);
        }
        return snapshots;
    }
}