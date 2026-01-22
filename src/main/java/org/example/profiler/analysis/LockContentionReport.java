package org.example.profiler.analysis;

import org.example.profiler.analysis.ContentionRecord;

public record LockContentionReport(
        ContentionRecord contention,
        double severityScore,
        double blockedRatio,
        boolean convoy
) {
    public static LockContentionReport from(
            ContentionRecord record,
            ProfilingWindow window
    ) {
        double ratio =
                (double) record.totalBlockedTime() / window.durationMs();

        return new LockContentionReport(
                record,
                record.severityScore(),
                ratio,
                record.isConvoy()
        );
    }
}
