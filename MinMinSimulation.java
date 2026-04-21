package org.fog.test.perfeval.simulations;

import org.fog.test.perfeval.metrics.MetricsCollector;
import org.fog.test.perfeval.model.SystemModel;
import org.fog.test.perfeval.model.Task;
import org.fog.test.perfeval.simulation.SimulationResult;

public class MinMinSimulation {
    
    public static SimulationResult run(Task[] tasks, int numDevices) {
        
        int n = tasks.length;
        int numResources = SystemModel.getNumResources();
        
        boolean[] assigned = new boolean[n];
        double[] readyTime = new double[numResources];
        int[] taskCount = new int[numResources];  // Track tasks per resource
        
        double totalLatency = 0;
        double totalEnergy = 0;
        double totalNetwork = 0;
        
        int assignedCount = 0;
        
        System.out.println("\n[Min-Min] Processing " + n + " tasks...");
        
        while (assignedCount < n) {
            
            double[] taskMinCompletion = new double[n];
            int[] taskBestResource = new int[n];
            
            for (int i = 0; i < n; i++) {
                if (assigned[i]) continue;
                
                taskMinCompletion[i] = Double.MAX_VALUE;
                
                for (int j = 0; j < numResources; j++) {
                    double latency = MetricsCollector.computeLatency(
                        j,
                        tasks[i].size,
                        readyTime[j],
                        numDevices
                    );
                    double completionTime = readyTime[j] + latency;
                    
                    if (completionTime < taskMinCompletion[i]) {
                        taskMinCompletion[i] = completionTime;
                        taskBestResource[i] = j;
                    }
                }
            }
            
            int bestTask = -1;
            double globalMin = Double.MAX_VALUE;
            
            for (int i = 0; i < n; i++) {
                if (!assigned[i] && taskMinCompletion[i] < globalMin) {
                    globalMin = taskMinCompletion[i];
                    bestTask = i;
                }
            }
            
            if (bestTask == -1) break;
            
            int bestResource = taskBestResource[bestTask];
            assigned[bestTask] = true;
            assignedCount++;
            taskCount[bestResource]++;
            
            double computationTimeMs = MetricsCollector.computeComputationTime(bestResource, tasks[bestTask].size);
            double transmissionTimeMs = MetricsCollector.computeTransmissionTime(bestResource, tasks[bestTask].size);
            
            double latency = MetricsCollector.computeLatency(
                bestResource,
                tasks[bestTask].size,
                readyTime[bestResource],
                numDevices
            );
            
            double energy = MetricsCollector.computeEnergy(
                bestResource,
                tasks[bestTask].size,
                computationTimeMs,
                transmissionTimeMs
            );
            
            double network = MetricsCollector.computeNetwork(bestResource, tasks[bestTask].size, numDevices);
            
            readyTime[bestResource] += computationTimeMs;
            
            totalLatency += latency;
            totalEnergy += energy;
            totalNetwork += network;
        }
        
        System.out.println("[Min-Min] Task Distribution:");
        for (int j = 0; j < numResources; j++) {
            String type = SystemModel.isLocal(j) ? "LOCAL" : 
                         (SystemModel.isFog(j) ? "FOG" : "CLOUD");
            System.out.printf("  %s %d: %d tasks (%.1f%%)%n", 
                type, j, taskCount[j], 100.0 * taskCount[j] / n);
        }
        
        return new SimulationResult(
            totalLatency / n,
            totalEnergy / n,
            totalNetwork / n
        );
    }
}
