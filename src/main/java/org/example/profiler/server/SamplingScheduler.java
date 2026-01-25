package org.example.profiler.server;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SamplingScheduler {

    private final ThreadSampler sampler;
    private final SnapshotStore store;

    @Scheduled(fixedDelay = 1000)
    public void sample() {
        store.add(sampler.sample());
    }
}