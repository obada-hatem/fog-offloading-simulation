package org.fog.test.perfeval.simulations;

import org.fog.test.perfeval.metrics.MetricsCollector;
import org.fog.test.perfeval.model.SystemModel;
import org.fog.test.perfeval.model.Task;
import org.fog.test.perfeval.rl.QLearningAgent;
import org.fog.test.perfeval.simulation.SimulationResult;

public class QLearningSimulation {
    
    // ========== INCREASED TRAINING ==========
    private static final int TRAIN_EPISODES = 30000;  // Increased to 30,000
    private static final double DEADLINE_MS = 200.0;
    
    private static final double LEARNING_RATE = 0.15;   // Increased
    private static final double DISCOUNT_FACTOR = 0.99;  // Increased
    private static final double INITIAL_EPSILON = 1.0;
    private static final double EPSILON_DECAY = 0.9995;  // Slower decay
    private static final double MIN_EPSILON = 0.01;
    
    public static SimulationResult run(Task[] allTasks, int numDevices) {
        
        int numResources = SystemModel.getNumResources();
        
        int trainSize = (int)(allTasks.length * 0.7);
        Task[] trainTasks = new Task[trainSize];
        Task[] testTasks = new Task[allTasks.length - trainSize];
        
        System.arraycopy(allTasks, 0, trainTasks, 0, trainSize);
        System.arraycopy(allTasks, trainSize, testTasks, 0, testTasks.length);
        
        System.out.println("\n[Q-Learning] Training with " + trainTasks.length + " tasks, " + TRAIN_EPISODES + " episodes...");
        
        QLearningAgent agent = new QLearningAgent(LEARNING_RATE, DISCOUNT_FACTOR, INITIAL_EPSILON, numResources);
        
        long startTraining = System.currentTimeMillis();
        
        for (int episode = 0; episode < TRAIN_EPISODES; episode++) {
            trainEpisode(agent, trainTasks, numDevices);
            agent.decayEpsilon(EPSILON_DECAY, MIN_EPSILON);
            
            if ((episode + 1) % 5000 == 0) {
                System.out.println("   Episode " + (episode + 1) + "/" + TRAIN_EPISODES + 
                                   ", epsilon = " + String.format("%.4f", agent.getEpsilon()));
            }
        }
        
        long trainingTime = System.currentTimeMillis() - startTraining;
        System.out.println("   Training completed in " + trainingTime + " ms");
        
        agent.setEpsilon(0.0);
        
        double[] readyTime = new double[numResources];
        
        double totalLatency = 0;
        double totalEnergy = 0;
        double totalNetwork = 0;
        
        for (Task task : testTasks) {
            
            String state = agent.discretizeState(readyTime, task.size);
            int action = agent.selectAction(state);
            
            double computationTimeMs = MetricsCollector.computeComputationTime(action, task.size);
            double transmissionTimeMs = MetricsCollector.computeTransmissionTime(action, task.size);
            
            double latency = MetricsCollector.computeLatency(
                action,
                task.size,
                readyTime[action],
                numDevices
            );
            
            double energy = MetricsCollector.computeEnergy(
                action,
                task.size,
                computationTimeMs,
                transmissionTimeMs
            );
            
            // FIXED: computeNetwork now takes 2 parameters (resource, taskSize)
            double network = MetricsCollector.computeNetwork(action, task.size, numDevices);
            
            readyTime[action] += computationTimeMs;
            
            totalLatency += latency;
            totalEnergy += energy;
            totalNetwork += network;
        }
        
        int n = testTasks.length;
        return new SimulationResult(
            totalLatency / n,
            totalEnergy / n,
            totalNetwork / n
        );
    }
    
    private static void trainEpisode(QLearningAgent agent, Task[] tasks, int numDevices) {
        
        int numResources = SystemModel.getNumResources();
        double[] readyTime = new double[numResources];
        
        for (Task task : tasks) {
            
            String state = agent.discretizeState(readyTime, task.size);
            int action = agent.selectAction(state);
            
            double computationTimeMs = MetricsCollector.computeComputationTime(action, task.size);
            double transmissionTimeMs = MetricsCollector.computeTransmissionTime(action, task.size);
            
            double latency = MetricsCollector.computeLatency(
                action,
                task.size,
                readyTime[action],
                numDevices
            );
            
            double energy = MetricsCollector.computeEnergy(
                action,
                task.size,
                computationTimeMs,
                transmissionTimeMs
            );
            
            // FIXED: computeNetwork now takes 2 parameters
            double network = MetricsCollector.computeNetwork(action, task.size, numDevices);
            
            boolean metDeadline = latency <= DEADLINE_MS;
            
            double reward = agent.calculateReward(action, latency, energy, network, metDeadline);
            
            readyTime[action] += computationTimeMs;
            
            String nextState = agent.discretizeState(readyTime, task.size);
            agent.updateQTable(state, action, reward, nextState);
        }
    }
}