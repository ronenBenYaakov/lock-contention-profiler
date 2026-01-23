package org.example.profiler.monitor;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString
@Setter
public class LockEvent {

    String lockId;
    String lockName;
    LockType lockType;
    long ownerThreadId;
    String ownerThreadName;
    StackTraceElement[] stackTrace;
    long acquiredTime;
    boolean isContended;

    public boolean isOwned() {
        return ownerThreadId > 0;
    }

    @Override
    public String toString() {
        return String.format("%s[%s] ownedBy=%s(%d) contended=%s",
                lockType,
                lockName,
                ownerThreadName != null ? ownerThreadName : "none",
                ownerThreadId,
                isContended);
    }
}
