package org.fog.test.perfeval.rl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class QLearningAgent {
    
    private Map<String, double[]> qTable;
    private Random rand;
    
    private double alpha;
    private double gamma;
    private double epsilon;
    
    private int actionSize;
    
    // Simplified normalization
    private static final double MAX_LATENCY = 10000.0;
    private static final double MAX_ENERGY = 5000.0;
    private static final double MAX_NETWORK = 200.0;
    
    // Reward weights - focus on latency and energy
    private static final double WEIGHT_LATENCY = 0.5;
    private static final double WEIGHT_ENERGY = 0.4;
    private static final double WEIGHT_NETWORK = 0.1;
    
    public QLearningAgent(double alpha, double gamma, double epsilon, int actionSize) {
        this.qTable = new HashMap<>();
        this.rand = new Random(42);
        this.alpha = alpha;
        this.gamma = gamma;
        this.epsilon = epsilon;
        this.actionSize = actionSize;
    }
    
    // SIMPLIFIED state representation
    public String discretizeState(double[] queueTimes, double taskSize) {
        // Only track the minimum queue and which resource type has it
        int bestResource = 0;
        double minQueue = queueTimes[0];
        for (int i = 1; i < queueTimes.length; i++) {
            if (queueTimes[i] < minQueue) {
                minQueue = queueTimes[i];
                bestResource = i;
            }
        }
        
        int queueLevel = getQueueLevel(minQueue);
        int sizeLevel = getSizeLevel(taskSize);
        int resourceType = getResourceType(bestResource);
        
        // Much smaller state space
        return queueLevel + "," + sizeLevel + "," + resourceType;
    }
    
    private int getQueueLevel(double queueTime) {
        if (queueTime < 100) return 0;
        if (queueTime < 500) return 1;
        if (queueTime < 1000) return 2;
        return 3;
    }
    
    private int getSizeLevel(double taskSize) {
        if (taskSize < 30) return 0;
        if (taskSize < 70) return 1;
        return 2;
    }
    
    private int getResourceType(int resource) {
        if (resource == 0) return 0;  // Local
        if (resource == actionSize - 1) return 2;  // Cloud
        return 1;  // Fog
    }
    
    public int selectAction(String state) {
        qTable.putIfAbsent(state, new double[actionSize]);
        
        if (rand.nextDouble() < epsilon) {
            // Smart exploration: prefer cloud (fastest)
            if (rand.nextDouble() < 0.6) {
                return actionSize - 1;  // Cloud
            }
            return 1 + rand.nextInt(actionSize - 2);  // Random fog
        }
        
        double[] qValues = qTable.get(state);
        int best = 0;
        for (int i = 1; i < actionSize; i++) {
            if (qValues[i] > qValues[best]) {
                best = i;
            }
        }
        return best;
    }
    
    public double calculateReward(int action, double latency, double energy, double network, boolean metDeadline) {
        
        double normLatency = Math.min(1.0, latency / MAX_LATENCY);
        double normEnergy = Math.min(1.0, energy / MAX_ENERGY);
        double normNetwork = Math.min(1.0, network / MAX_NETWORK);
        
        double cost = (WEIGHT_LATENCY * normLatency) + 
                      (WEIGHT_ENERGY * normEnergy) + 
                      (WEIGHT_NETWORK * normNetwork);
        
        double reward = -cost;
        
        // Strong deadline incentive
        if (metDeadline) {
            reward += 1.0;
        } else {
            reward -= 2.0;
        }
        
        return reward;
    }
    
    public void updateQTable(String state, int action, double reward, String nextState) {
        qTable.putIfAbsent(state, new double[actionSize]);
        qTable.putIfAbsent(nextState, new double[actionSize]);
        
        double oldQ = qTable.get(state)[action];
        double maxNext = Arrays.stream(qTable.get(nextState)).max().orElse(0.0);
        
        double newQ = oldQ + alpha * (reward + gamma * maxNext - oldQ);
        qTable.get(state)[action] = newQ;
    }
    
    public void decayEpsilon(double decayRate, double minEpsilon) {
        epsilon = Math.max(minEpsilon, epsilon * decayRate);
    }
    
    public void setEpsilon(double epsilon) { this.epsilon = epsilon; }
    public double getEpsilon() { return epsilon; }
}