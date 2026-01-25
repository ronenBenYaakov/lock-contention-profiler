package org.example.profiler.analysis;

public record LockStats(
        String lockId,
        int heldByThreads
) {}