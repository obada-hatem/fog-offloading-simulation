package org.fog.test.perfeval.simulations;

import org.fog.test.perfeval.metrics.MetricsCollector;
import org.fog.test.perfeval.model.SystemModel;
import org.fog.test.perfeval.model.Task;
import org.fog.test.perfeval.simulation.SimulationResult;
import java.util.Random;

public class LoadBasedSimulation {
    
    private static final Random rand = new Random(42);
    
    public static SimulationResult run(Task[] tasks, int numDevices) {
        
        int numResources = SystemModel.getNumResources();
        double[] readyTime = new double[numResources];
        int[] taskCount = new int[numResources];
        
        double totalLatency = 0;
        double totalEnergy = 0;
        double totalNetwork = 0;
        
        System.out.println("\n[Load-Based] Processing " + tasks.length + " tasks...");
        
        for (Task task : tasks) {
            
            double minQueue = Double.MAX_VALUE;
            java.util.ArrayList<Integer> bestResources = new java.util.ArrayList<>();
            
            for (int j = 0; j < numResources; j++) {
                if (readyTime[j] < minQueue) {
                    minQueue = readyTime[j];
                    bestResources.clear();
                    bestResources.add(j);
                } else if (Math.abs(readyTime[j] - minQueue) < 1.0) {
                    // If queue times are similar, add to candidates
                    bestResources.add(j);
                }
            }
            
            // Randomly select among best resources for load balancing
            int bestResource;
            if (bestResources.size() > 1) {
                // Random choice to distribute load
                bestResource = bestResources.get(rand.nextInt(bestResources.size()));
            } else {
                bestResource = bestResources.get(0);
            }
            
            taskCount[bestResource]++;
            
            double computationTimeMs = MetricsCollector.computeComputationTime(bestResource, task.size);
            double transmissionTimeMs = MetricsCollector.computeTransmissionTime(bestResource, task.size);
            
            double latency = MetricsCollector.computeLatency(
                bestResource,
                task.size,
                readyTime[bestResource],
                numDevices
            );
            
            double energy = MetricsCollector.computeEnergy(
                bestResource,
                task.size,
                computationTimeMs,
                transmissionTimeMs
            );
            
            double network = MetricsCollector.computeNetwork(bestResource, task.size, numDevices);
            
            readyTime[bestResource] += computationTimeMs;
            
            totalLatency += latency;
            totalEnergy += energy;
            totalNetwork += network;
        }
        
        // Print distribution
        System.out.println("[Load-Based] Task Distribution:");
        int fogTotal = 0, localTotal = 0, cloudTotal = 0;
        for (int j = 0; j < numResources; j++) {
            String type = SystemModel.isLocal(j) ? "LOCAL" : 
                         (SystemModel.isFog(j) ? "FOG" : "CLOUD");
            System.out.printf("  %s %d: %d tasks (%.1f%%)%n", 
                type, j, taskCount[j], 100.0 * taskCount[j] / tasks.length);
            
            if (SystemModel.isLocal(j)) localTotal += taskCount[j];
            else if (SystemModel.isFog(j)) fogTotal += taskCount[j];
            else cloudTotal += taskCount[j];
        }
        System.out.printf("  TOTAL - LOCAL: %d, FOG: %d, CLOUD: %d%n", localTotal, fogTotal, cloudTotal);
        
        return new SimulationResult(
            totalLatency / tasks.length,
            totalEnergy / tasks.length,
            totalNetwork / tasks.length
        );
    }
}
