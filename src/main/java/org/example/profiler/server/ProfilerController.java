package org.example.profiler.server;

import org.example.profiler.analysis.ContentionAnalyzer;
import org.example.profiler.analysis.ContentionRecord;
import org.example.profiler.monitor.ThreadSnapshot;
import org.example.profiler.server.dto.StatsResponse;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/profiler")
@RestController
public class ProfilerController {

    private final ProfilerService profilerService;

    public ProfilerController(ProfilerService profilerService) {
        this.profilerService = profilerService;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        List<ThreadSnapshot> threads = profilerService.getThreadSnapshots();

        int totalThreads = threads.size();
        int totalLocks = threads.stream().mapToInt(ThreadSnapshot::getHeldLockCount).sum();
        int blockedThreads = (int) threads.stream().filter(ThreadSnapshot::isBlocked).count();

        Map<String, Object> response = new HashMap<>();
        response.put("threads", threads);
        response.put("totalThreads", totalThreads);
        response.put("totalLocks", totalLocks);
        response.put("blockedThreads", blockedThreads);

        return response;
    }
}