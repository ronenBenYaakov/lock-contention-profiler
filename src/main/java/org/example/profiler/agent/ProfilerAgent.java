package org.example.profiler.agent;

import java.lang.instrument.Instrumentation;

import java.util.*;
import java.util.concurrent.*;


public class ProfilerAgent {

    public static void premain(String args, Instrumentation inst) {
        start();
    }

    public static void agentmain(String args, Instrumentation inst) {
        start();
    }

    private static void start() {
        ProfilerSampler.start();
        System.out.println("[ProfilerAgent] started");
    }
}

