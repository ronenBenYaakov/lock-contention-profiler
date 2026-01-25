package org.example.profiler.monitor;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class LockEvent {

    String lockId;
    String lockName;
    LockType lockType;

    long ownerThreadId;
    String ownerThreadName;

    StackTraceElement[] stackTrace;

    long acquiredTime;
    boolean contended;

    /* ------------------------
       Convenience logic
       ------------------------ */

    public boolean isOwned() {
        return ownerThreadId > 0;
    }

    /* ------------------------
       Factory methods
       ------------------------ */

    /** Monitor locks (synchronized blocks / methods) */
    public static LockEvent fromMonitor(MonitorInfo monitor, long sampleTime) {
        return new LockEvent(
                monitor.getClassName() + "@" + monitor.getIdentityHashCode(),
                monitor.getClassName(),
                LockType.MONITOR,
                -1,
                null,
                monitor.getLockedStackFrame() != null
                        ? new StackTraceElement[]{monitor.getLockedStackFrame()}
                        : new StackTraceElement[0],
                sampleTime,
                false
        );
    }

    /** java.util.concurrent locks */
    public static LockEvent fromSynchronizer(LockInfo lock, long sampleTime) {
        return new LockEvent(
                lock.getClassName() + "@" + lock.getIdentityHashCode(),
                lock.getClassName(),
                LockType.SYNCHRONIZER,
                -1,
                null,
                new StackTraceElement[0],
                sampleTime,
                false
        );
    }

    /** Lock the thread is BLOCKED or WAITING on */
    public static LockEvent fromWaitingOn(
            LockInfo lock,
            long ownerThreadId,
            String ownerThreadName,
            StackTraceElement[] stack,
            long sampleTime
    ) {
        return new LockEvent(
                lock.getClassName() + "@" + lock.getIdentityHashCode(),
                lock.getClassName(),
                LockType.SYNCHRONIZER,
                ownerThreadId,
                ownerThreadName,
                stack,
                sampleTime,
                true
        );
    }

    public static StackTraceElement[] getLockedStackTrace(ThreadInfo info) {
        if (info == null) {
            return new StackTraceElement[0];
        }

        // 1. If thread is blocked -> top stack frame
        if (info.getThreadState() == Thread.State.BLOCKED && info.getLockName() != null) {
            StackTraceElement[] stack = info.getStackTrace();
            return stack.length > 0 ? new StackTraceElement[]{stack[0]} : stack;
        }

        // 2. If thread owns a monitor -> return the locked stack frame of the first monitor
        MonitorInfo[] monitors = info.getLockedMonitors();
        if (monitors != null && monitors.length > 0) {
            StackTraceElement lockedFrame = monitors[0].getLockedStackFrame();
            return lockedFrame != null ? new StackTraceElement[]{lockedFrame} : new StackTraceElement[0];
        }

        // 3. Fallback -> full thread stack
        return info.getStackTrace();
    }
}