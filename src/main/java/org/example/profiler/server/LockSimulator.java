package org.example.profiler.server;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

@Component
public class LockSimulator {

    private final ReentrantLock lock = new ReentrantLock();

    @PostConstruct
    public void startSimulation() {
        // Create 5 threads contending for the same lock
        for (int i = 0; i < 5; i++) {
            new Thread(this::simulateWork, "Worker-" + i).start();
        }
    }

    private void simulateWork() {
        while (true) {
            try {
                lock.lock(); // Try to acquire lock
                Thread.sleep(50); // hold lock for a bit
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                lock.unlock();
            }

            try {
                Thread.sleep(20); // pause before next attempt
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
