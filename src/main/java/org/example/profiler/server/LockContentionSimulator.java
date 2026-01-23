package org.example.profiler.server;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class LockContentionSimulator {

    private final Object HOT_LOCK = new Object();
    private final ExecutorService pool = Executors.newFixedThreadPool(6);

    @PostConstruct
    public void start() {
        for (int i = 0; i < 6; i++) {
            pool.submit(this::work);
        }
    }

    private void work() {
        while (true) {
            synchronized (HOT_LOCK) {
                try {
                    Thread.sleep(50); // force contention
                } catch (InterruptedException ignored) {}
            }
        }
    }
}
