package org.example.profiler.server;

import org.example.profiler.monitor.ThreadSnapshot;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Component
public class SnapshotStore {

    private final Deque<List<ThreadSnapshot>> window =
            new ArrayDeque<>();

    private static final int MAX_SAMPLES = 20;

    public synchronized void add(List<ThreadSnapshot> sample) {
        window.addLast(sample);
        if (window.size() > MAX_SAMPLES) {
            window.removeFirst();
        }
    }

    public synchronized List<ThreadSnapshot> all() {
        return window.stream().flatMap(List::stream).toList();
    }
}
