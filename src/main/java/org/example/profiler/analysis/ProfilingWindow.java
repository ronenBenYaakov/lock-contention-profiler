package org.example.profiler.analysis;

public record ProfilingWindow(
        long startTime,
        long endTime
) {
    public long durationMs() {
        return endTime - startTime;
    }
}
