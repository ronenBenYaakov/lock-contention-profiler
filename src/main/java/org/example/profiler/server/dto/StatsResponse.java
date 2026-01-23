package org.example.profiler.server.dto;


import org.example.profiler.analysis.ContentionRecord;
import org.example.profiler.monitor.ThreadSnapshot;

import java.util.List;

public class StatsResponse {
    private List<ThreadSnapshot> threads;
    private List<ContentionRecord> locks;
    private long timestamp;

    public StatsResponse(List<ThreadSnapshot> threads, List<ContentionRecord> locks) {
        this.threads = threads;
        this.locks = locks;
        this.timestamp = System.currentTimeMillis();
    }

    public List<ThreadSnapshot> getThreads() {
        return threads;
    }

    public List<ContentionRecord> getLocks() {
        return locks;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
