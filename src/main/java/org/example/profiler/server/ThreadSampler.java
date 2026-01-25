package org.example.profiler.server;

import org.example.profiler.monitor.ThreadSnapshot;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

@Component
public class ThreadSampler {

    private final ThreadMXBean mxBean =
            ManagementFactory.getThreadMXBean();

    public List<ThreadSnapshot> sample() {
        long[] ids = mxBean.getAllThreadIds();
        ThreadInfo[] infos = mxBean.getThreadInfo(ids, true, true);

        long now = System.currentTimeMillis();
        List<ThreadSnapshot> snapshots = new ArrayList<>();

        for (ThreadInfo info : infos) {
            if (info == null) continue;
            snapshots.add(ThreadSnapshot.from(info, now));
        }
        return snapshots;
    }
}
