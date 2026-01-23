package org.example.profiler.server;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.example.profiler.analysis.ThreadHistory;
import org.example.profiler.analysis.LockHistory;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilerRequest {
    private Map<Long, ThreadHistory> threadHistories;
    private Map<String, LockHistory> lockHistories;
}

