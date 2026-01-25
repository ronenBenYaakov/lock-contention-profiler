package org.example.profiler.server;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

@Configuration
public class ProfilerConfig {

    @PostConstruct
    public void enableContentionMonitoring() {
        ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();

        if (mxBean.isThreadContentionMonitoringSupported()) {
            mxBean.setThreadContentionMonitoringEnabled(true);
        }
    }
}