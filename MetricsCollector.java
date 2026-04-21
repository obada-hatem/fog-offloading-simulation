package org.fog.test.perfeval.metrics;

import org.fog.test.perfeval.model.SystemModel;

public class MetricsCollector {

   

    private static final double LOCAL_ENERGY_PER_MI = 0.5;
    private static final double FOG_ENERGY_PER_MI   = 0.8;
    private static final double CLOUD_ENERGY_PER_MI = 1.2;

    private static final double TX_ENERGY_PER_MB = 0.4;
    private static final double MI_TO_MB = 0.01;

  

    public static double computeTransmissionTime(int resource, double taskSizeMI) {

        if (SystemModel.isLocal(resource)) return 0.0;

        double dataSizeMB = taskSizeMI * MI_TO_MB;
        double bandwidth = SystemModel.BANDWIDTH[resource];

        return (dataSizeMB / bandwidth) * 1000.0; // ms
    }

    public static double computeComputationTime(int resource, double taskSizeMI) {

        double mips = SystemModel.MIPS[resource];
        return (taskSizeMI / mips) * 1000.0; // ms
    }


    public static double computeContention(int resource, int totalDevices) {

        double factor = Math.log1p(totalDevices);

        if (SystemModel.isLocal(resource)) {
            return factor * 4.0;
        }

        if (SystemModel.isFog(resource)) {
            return factor * 2.0;
        }

        return factor * 1.2;
    }
    public static double computeQueueDrain(int numDevices, double queueLength) {
        double congestion = computeCongestion(numDevices, queueLength);
        return 1.0 + 2.5 * Math.exp(-congestion);
    }
    
    public static double computeCongestion(int numDevices, double queueLength) {
        double loadPressure = numDevices / 1000.0;
        return loadPressure * (1.0 + Math.log1p(queueLength));
    }

  

    public static double computeLatency(int resource,
                                        double taskSizeMI,
                                        double queueTimeMs,
                                        int totalDevices) {

        double transmission = computeTransmissionTime(resource, taskSizeMI);
        double computation  = computeComputationTime(resource, taskSizeMI);
        double propagation  = SystemModel.PROP_DELAY[resource];
        double contention   = computeContention(resource, totalDevices);

        double queue = Math.min(queueTimeMs, 300.0);

        return transmission + computation + propagation + queue + contention;
    }

   

    public static double computeEnergy(int resource,
                                       double taskSizeMI,
                                       double computationTimeMs,
                                       double transmissionTimeMs) {

        double dataSizeMB = taskSizeMI * MI_TO_MB;

        double computationEnergy = getEnergyPerMI(resource) * taskSizeMI;

        double transmissionEnergy = 0.0;
        if (!SystemModel.isLocal(resource)) {
            transmissionEnergy = dataSizeMB * TX_ENERGY_PER_MB;
        }

        double activeTimeSec = computationTimeMs / 1000.0;
        double staticEnergy = getStaticPower(resource) * (activeTimeSec + 0.05);

        return computationEnergy + transmissionEnergy + staticEnergy;
    }

    

    public static double computeNetwork(int resource,
            double taskSizeMI,
            int numDevices) {

double dataSizeMB = taskSizeMI * MI_TO_MB;

double congestionFactor = 1.0 + (numDevices / 1000.0);

double base;

if (SystemModel.isLocal(resource)) {
base = dataSizeMB * 0.05;
} else if (SystemModel.isFog(resource)) {
base = dataSizeMB * 2.0;
} else {
base = dataSizeMB * 3.5;
}

return base * congestionFactor;
}



    private static double getEnergyPerMI(int resource) {
        if (SystemModel.isLocal(resource)) return LOCAL_ENERGY_PER_MI;
        if (SystemModel.isFog(resource))   return FOG_ENERGY_PER_MI;
        return CLOUD_ENERGY_PER_MI;
    }
  
    private static double getStaticPower(int resource) {
        if (SystemModel.isLocal(resource)) return 1.5;
        if (SystemModel.isFog(resource))   return 40.0;
        return 150.0;
    }
}
