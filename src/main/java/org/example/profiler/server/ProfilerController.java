package org.example.profiler.server;

import org.example.profiler.analysis.ContentionAnalyzer;
import org.example.profiler.analysis.ContentionRecord;
import org.example.profiler.monitor.ThreadSnapshot;
import org.example.profiler.server.dto.StatsResponse;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/profiler")
public class ProfilerController {

    private final ContentionAnalyzer analyzer = new ContentionAnalyzer();

    @GetMapping("/stats")
    public StatsResponse getStats() {
        // 1️⃣ Get raw snapshots (you might already have a snapshot service)
        List<ThreadSnapshot> snapshots = SnapshotService.getCurrentSnapshots();

        // 2️⃣ Use ContentionAnalyzer to analyze locks
        List<ContentionRecord> lockRecords = analyzer.generateContentionRecords(snapshots, 3);

        // 3️⃣ Return as structured JSON
        return new StatsResponse(snapshots, lockRecords);
    }
}