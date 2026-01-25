package org.example.profiler.attachment;

import com.sun.tools.attach.VirtualMachine;

public class JvmAttacher {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java JvmAttacher <PID> <path-to-agent-jar>");
            System.exit(1);
        }

        String pid = args[0];
        String agentPath = args[1];

        System.out.println("Attaching to PID: " + pid);
        VirtualMachine vm = VirtualMachine.attach(pid);
        vm.loadAgent(agentPath);
        vm.detach();
        System.out.println("Agent loaded successfully.");
    }
}
