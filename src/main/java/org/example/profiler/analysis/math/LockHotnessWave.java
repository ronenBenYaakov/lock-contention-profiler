package org.example.profiler.analysis.math;

import org.example.profiler.monitor.HotLockRecord;

import java.util.List;
import java.util.stream.DoubleStream;

public class LockHotnessWave {

    private final List<HotLockRecord> locks;

    public LockHotnessWave(List<HotLockRecord> locks) {
        this.locks = locks;
    }

    /**
     * Compute the combined system hotness at a given x
     * F(x) = sum_i A_i * sin(omega_i * x)
     *
     * @param x time or progression variable
     * @return total system hotness at x
     */
    public double systemHotnessAt(double x) {
        double sum = 0.0;
        for (HotLockRecord lock : locks) {
            double A = lock.normalizedBlockedRatio(); // amplitude
            double omega = lock.ownershipFrequency()
                    .getOrDefault(lock.mainOwnerId(), 1); // frequency
            sum += A * Math.sin(omega * x);
        }
        return sum;
    }

    /**
     * Compute the maximum hotness over a range of x values
     *
     * @param xs array of x values
     * @return maximum F(x)
     */
    public double maxHotness(double[] xs) {
        return DoubleStream.of(xs)
                .map(this::systemHotnessAt)
                .max()
                .orElse(0.0);
    }

    /**
     * Compute the average hotness over a range of x values
     *
     * @param xs array of x values
     * @return average F(x)
     */
    public double averageHotness(double[] xs) {
        return DoubleStream.of(xs)
                .map(this::systemHotnessAt)
                .average()
                .orElse(0.0);
    }

    /**
     * Compute the variance of hotness over a range of x values
     *
     * @param xs array of x values
     * @return variance of F(x)
     */
    public double varianceHotness(double[] xs) {
        double avg = averageHotness(xs);
        return DoubleStream.of(xs)
                .map(x -> Math.pow(systemHotnessAt(x) - avg, 2))
                .average()
                .orElse(0.0);
    }

    /**
     * Count the longest hot streak above a threshold
     *
     * @param xs        array of x values
     * @param threshold hotness threshold
     * @return length of the longest consecutive points above threshold
     */
    public int hotStreakCount(double[] xs, double threshold) {
        int streak = 0;
        int maxStreak = 0;
        for (double x : xs) {
            if (systemHotnessAt(x) > threshold) {
                streak++;
            } else {
                maxStreak = Math.max(maxStreak, streak);
                streak = 0;
            }
        }
        return Math.max(maxStreak, streak);
    }
}
