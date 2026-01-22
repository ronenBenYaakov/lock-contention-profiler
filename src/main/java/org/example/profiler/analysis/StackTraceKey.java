package org.example.profiler.analysis;

import java.util.List;

public record StackTraceKey(List<StackTraceElement> frames) {}
