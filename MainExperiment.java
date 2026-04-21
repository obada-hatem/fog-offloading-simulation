package org.fog.test.perfeval;

import org.fog.test.perfeval.metrics.ResultsExporter;
import org.fog.test.perfeval.metrics.SimpleChartGenerator;
import org.fog.test.perfeval.model.Task;
import org.fog.test.perfeval.simulation.SimulationResult;
import org.fog.test.perfeval.simulations.LoadBasedSimulation;
import org.fog.test.perfeval.simulations.MinMinSimulation;
import org.fog.test.perfeval.simulations.QLearningSimulation;
import org.fog.test.perfeval.model.TaskGenerator;
import java.io.IOException;

public class MainExperiment {

    public static void main(String[] args) {

        // Initialize CSV file for results
        ResultsExporter.initFile();

        int[] iotSizes = {100, 250, 500, 750, 1000, 1250, 1500};
        int runs = 10;  // Number of runs per configuration

        System.out.println("==========================================");
        System.out.println("Fog Computing Task Offloading Simulation");
        System.out.println("Comparing: Q-Learning vs Min-Min vs Load-Based");
        System.out.println("==========================================");

        for (int users : iotSizes) {

            System.out.println("\n===============================");
            System.out.println("IoT Devices: " + users);
            System.out.println("===============================");

            double totalQLat = 0, totalQEng = 0, totalQNet = 0;
            double totalMLat = 0, totalMEng = 0, totalMNet = 0;
            double totalLLat = 0, totalLEng = 0, totalLNet = 0;

            for (int i = 0; i < runs; i++) {

                System.out.println("\nRun " + (i + 1));

                Task[] tasks = TaskGenerator.generateTasks(users);

                
                long startTime = System.nanoTime();
                SimulationResult qRes = QLearningSimulation.run(tasks, users);
                long endTime = System.nanoTime();
                double qTime = (endTime - startTime) / 1_000_000.0; // ms
                
                System.out.printf("Q-Learning: %.4f ms (exec time: %.2f ms)%n", qRes.latency, qTime);

                totalQLat += qRes.latency;
                totalQEng += qRes.energy;
                totalQNet += qRes.network;

                
                startTime = System.nanoTime();
                SimulationResult mRes = MinMinSimulation.run(tasks, users);
                endTime = System.nanoTime();
                double mTime = (endTime - startTime) / 1_000_000.0;
                
                System.out.printf("Min-Min: %.4f ms (exec time: %.2f ms)%n", mRes.latency, mTime);

                totalMLat += mRes.latency;
                totalMEng += mRes.energy;
                totalMNet += mRes.network;

             
                startTime = System.nanoTime();
                SimulationResult lRes = LoadBasedSimulation.run(tasks, users);
                endTime = System.nanoTime();
                double lTime = (endTime - startTime) / 1_000_000.0;
                
                System.out.printf("Load-Based: %.4f ms (exec time: %.2f ms)%n", lRes.latency, lTime);

                totalLLat += lRes.latency;
                totalLEng += lRes.energy;
                totalLNet += lRes.network;
            }

          
            double avgQLat = totalQLat / runs;
            double avgQEng = totalQEng / runs;
            double avgQNet = totalQNet / runs;

            double avgMLat = totalMLat / runs;
            double avgMEng = totalMEng / runs;
            double avgMNet = totalMNet / runs;

            double avgLLat = totalLLat / runs;
            double avgLEng = totalLEng / runs;
            double avgLNet = totalLNet / runs;

           
            System.out.println("\n--- AVERAGE RESULTS (" + users + " Devices) ---");
            
            System.out.println(String.format("%-12s | %-12s | %-12s | %-12s", 
                "Algorithm", "Latency", "Energy", "Network"));
            System.out.println("------------------------------------------------------------");
            
            System.out.println(String.format("%-12s | %-12.4f | %-12.4f | %-12.4f", 
                "Q-Learning", avgQLat, avgQEng, avgQNet));
            
            System.out.println(String.format("%-12s | %-12.4f | %-12.4f | %-12.4f", 
                "Min-Min", avgMLat, avgMEng, avgMNet));
            
            System.out.println(String.format("%-12s | %-12.4f | %-12.4f | %-12.4f", 
                "Load-Based", avgLLat, avgLEng, avgLNet));

            
            ResultsExporter.writeResult("Q-Learning", users, avgQLat, avgQEng, avgQNet);
            ResultsExporter.writeResult("Min-Min", users, avgMLat, avgMEng, avgMNet);
            ResultsExporter.writeResult("Load-Based", users, avgLLat, avgLEng, avgLNet);
        }

        System.out.println("\n==========================================");
        System.out.println(" ALL EXPERIMENTS COMPLETED ");
        System.out.println("==========================================");
        
       
        System.out.println("\n Generating charts from CSV data...");
        
        try {
            SimpleChartGenerator.generateFromCSV();
            System.out.println("\n Charts generated successfully!");
            System.out.println("   Charts are displayed in separate windows.");
            System.out.println("   Data saved to: results.csv");
        } catch (IOException e) {
            System.err.println("\n Error generating charts: " + e.getMessage());
            System.err.println("   Please check that results.csv was created properly.");
        } catch (Exception e) {
            System.err.println("\n Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n==========================================");
        System.out.println(" Simulation Complete. Ready for Springer.");
        System.out.println("==========================================");
    }
}
