package cloudsimresearchprojectfinal;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

class Slice {
    int id;
    double minLatency;
    double maxLatency;
    List<Cloudlet> ueList;

    public Slice(int id, double minLatency, double maxLatency) {
        this.id = id;
        this.minLatency = minLatency;
        this.maxLatency = maxLatency;
        this.ueList = new ArrayList<>();
    }
}

class Cluster {
    int id;
    List<Cloudlet> ueList;

    public Cluster(int id) {
        this.ueList = new ArrayList<>();
    }
}

public class Heuristicaugment {

    private static double totalEnergy = 0.0; // Energy consumption

    private static List<Vm> vmList;
    private static List<Cloudlet> cloudletList;

    public static void main(String[] args) {
        System.out.println("Starting CloudSim simulation...");

        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        try {
            PrintStream out = new PrintStream(new FileOutputStream("C:\\Users\\vinay\\Cloudsim_ResearchProject\\simulation_output_heuristicaugmentupdatednewfour.txt"));
            System.setOut(out);
            System.setErr(out);

            Log.setOutput(out);

            long iterationStartTime = System.nanoTime();

            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            Datacenter datacenter0 = createDatacenter("Datacenter_0", 1);
            Datacenter datacenter1 = createDatacenter("Datacenter_1", 2);
            Datacenter datacenter2 = createDatacenter("Datacenter_2", 3);

            Log.printLine(datacenter0.getName() + " created.");
            Log.printLine(datacenter1.getName() + " created.");
            Log.printLine(datacenter2.getName() + " created.");

            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            Log.printLine("Broker created with ID: " + brokerId);

            vmList = createVMs(brokerId);
            broker.submitVmList(vmList);

            Log.printLine("VMs submitted to the broker.");

            cloudletList = loadCloudletsFromCSV(brokerId, "C:/Users/vinay/Research Project/GWA-T-12BitbrainsPreprocessing/preprocessed_fastStorage.csv");
            cloudletList.addAll(loadCloudletsFromCSV(brokerId, "C:/Users/vinay/Research Project/GWA-T-12BitbrainsPreprocessing/preprocessed_rnd.csv"));
            broker.submitCloudletList(cloudletList);

            Log.printLine("Cloudlets submitted to the broker.");

            simulateVMCommunication();

            Log.printLine("Starting CloudSim simulation...");
            CloudSim.startSimulation();

            List<Cloudlet> newList = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();
            Log.printLine("Simulation stopped. Cloudlet processing results collected.");

            double overallTaskCompletionTime = calculateOverallTaskCompletionTime(newList);

            Log.printLine("=== System Metrics ===");
            Log.printLine("Overall Task Completion Time: " + overallTaskCompletionTime + " seconds");

            totalEnergy = calculateTotalEnergy(newList);
            double coolingPower = calculateCoolingPower(totalEnergy);
            double totalEnergyWithCooling = totalEnergy + coolingPower;

            Log.printLine("Total Energy Consumption: " + totalEnergy + " Watts");
            Log.printLine("Cooling Power Consumption: " + coolingPower + " Watts");
            Log.printLine("Total Energy Consumption with Cooling: " + totalEnergyWithCooling + " Watts");

            long iterationEndTime = System.nanoTime();
            long iterationTime = iterationEndTime - iterationStartTime;

            Log.printLine("Iteration completed in " + (iterationTime / 1_000_000) + " ms.");
            Log.printLine("Total convergence time: " + (iterationTime / 1_000_000) + " ms.");

            writeEnergyResultsToFile("C:\\Users\\vinay\\Cloudsim_ResearchProject\\simulation_output_heuristicaugmentupdatednewfour.txt", totalEnergy, coolingPower, totalEnergyWithCooling, overallTaskCompletionTime);

            printCloudletList(newList);
            writeCloudletResultsToFile(newList, "C:\\Users\\vinay\\Cloudsim_ResearchProject\\simulation_output_heuristicaugmentupdatednewfour.txt");

            Log.printLine("CloudSim simulation finished!");

            System.out.flush();
            System.err.flush();

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Simulation failed due to an unexpected error.");
            System.out.flush();
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    // Function to simulate VM-to-VM communication
    private static void simulateVMCommunication() {
    	 // Network Slice 1
        simulateCommunicationBetweenVMs(0, 1, 5);
        simulateCommunicationBetweenVMs(1, 0, 5);

        // Network Slice 2
        simulateCommunicationBetweenVMs(3, 5, 7); // Inter-ring
        simulateCommunicationBetweenVMs(3, 8, 7); // Inter-slice to Slice 3
        simulateCommunicationBetweenVMs(4, 8, 7); // Inter-slice to Slice 3
        simulateCommunicationBetweenVMs(4, 12, 10); // Inter-slice to Slice 3
        simulateCommunicationBetweenVMs(5, 6, 7); // Inter-ring
        simulateCommunicationBetweenVMs(5, 3, 7); // Inter-ring
        simulateCommunicationBetweenVMs(5, 12, 10); // Inter-slice to Slice 3
        simulateCommunicationBetweenVMs(6, 5, 7); // Inter-ring

        // Network Slice 3
        simulateCommunicationBetweenVMs(7, 11, 7); // Inter-ring
        simulateCommunicationBetweenVMs(8, 1, 7); // Inter-slice to Slice 1
        simulateCommunicationBetweenVMs(8, 3, 7); // Inter-slice to Slice 2
        simulateCommunicationBetweenVMs(8, 4, 7); // Inter-slice to Slice 2
        simulateCommunicationBetweenVMs(11, 7, 7); // Inter-ring
        simulateCommunicationBetweenVMs(12, 4, 10); // Inter-slice to Slice 2
        simulateCommunicationBetweenVMs(12, 5, 10); // Inter-slice to Slice 2
    }

    private static Datacenter createDatacenter(String name, int numHosts) {
        List<Host> hostList = new ArrayList<>();

        for (int i = 0; i < numHosts; i++) {
            List<Pe> peList = new ArrayList<>();
            int mips = 1000;
            peList.add(new Pe(0, new PeProvisionerSimple(mips)));

            int ram = 8192;  
            long storage = 1000;
            int bw = 10000;

            hostList.add(new Host(
                    i,
                    new RamProvisionerSimple(ram),
                    new BwProvisionerSimple(bw),
                    storage,
                    peList,
                    new VmSchedulerTimeShared(peList)
            ));
        }

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, 0.1, 0.001, 0.0001, 0.00001, 0.00001);

        try {
            return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static DatacenterBroker createBroker() {
        try {
            return new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<Vm> createVMs(int brokerId) {
        List<Vm> vmlist = new ArrayList<>();

        long size = 20;
        int ram = 2;
        int mips = 50; 
        long bw = 5;
        int pesNumber = 4; 
        String vmm = "Xen";

        for (int i = 0; i < 13; i++) {
            Vm vm = new Vm(i, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
            vmlist.add(vm);
        }

        return vmlist;
    }

    private static List<Cloudlet> loadCloudletsFromCSV(int brokerId, String csvFilePath) {
        List<Cloudlet> cloudletList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] values = line.split(",");
                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i].trim();
                }

                if (values.length < 11) {
                    System.err.println("Invalid row with insufficient columns: " + line);
                    continue;
                }

                try {
                    int cores = parseInteger(values[1], 1);
                    double cpuUsageMHz = parseDouble(values[2], 1.0);

                    long fileSize = 1;
                    long outputSize = 1;

                    Cloudlet cloudlet = new Cloudlet(
                            cloudletList.size(), (long) cpuUsageMHz * 100, cores, fileSize, outputSize,
                            new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull()
                    );
                    cloudlet.setUserId(brokerId);
                    cloudletList.add(cloudlet);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number format in row: " + line);
                    continue;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return cloudletList;
    }

    private static int parseInteger(String value, int defaultValue) {
        try {
            int result = Integer.parseInt(value);
            return (result != 0) ? result : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    // Simulate communication with bandwidth, penalty, and energy costs
    private static void simulateCommunicationBetweenVMs(int vm1, int vm2, double bandwidth) {
        Log.printLine("Simulating communication between VM " + vm1 + " and VM " + vm2 + " with bandwidth: " + bandwidth + " KB/s");

        double dynamicBandwidth = bandwidth * (1.27 + Math.random() * 0.58);  // Smaller variation in bandwidth
        double bandwidthPenalty = dynamicBandwidth < 5 ? 1.87 : 1.27;  // Smaller increase in bandwidth penalty
        double communicationEnergyCost = bandwidth * 4.3;  // Smaller increase in communication energy cost multiplier
        double transmissionPowerBase = 9.3;  // Slightly increased transmission power base

        totalEnergy += bandwidthPenalty * transmissionPowerBase;
        totalEnergy += communicationEnergyCost;
        totalEnergy += bandwidthPenalty * communicationEnergyCost * 5;
        totalEnergy += transmissionPowerBase * 4;

        double networkDelay = communicationEnergyCost / dynamicBandwidth;
        Log.printLine("Network delay for communication between VM " + vm1 + " and VM " + vm2 + ": " + networkDelay + " seconds");
    }

    // Total energy calculation
    private static double calculateTotalEnergy(List<Cloudlet> cloudlets) {
        double totalEnergy = 0.0;
        double processingPower = 0.001045;  // Smaller increase in processing power
        for (Cloudlet cloudlet : cloudlets) {
            totalEnergy += cloudlet.getActualCPUTime() * processingPower;
        }
        return totalEnergy;
    }

    // Cooling power calculation
    public static double calculateCoolingPower(double totalEnergyConsumption) {
    	double coolingFactor = 0.0517;  // Slightly increased cooling factor
        return totalEnergyConsumption * coolingFactor;
    }
        
    private static double calculateOverallTaskCompletionTime(List<Cloudlet> cloudlets) {
        double earliestStartTime = Double.MAX_VALUE;
        double latestFinishTime = Double.MIN_VALUE;

        for (Cloudlet cloudlet : cloudlets) {
            Vm vm = vmList.get(cloudlet.getVmId()); // Get the VM assigned to the cloudlet
            double bandwidthPenalty = 0.1; // Example value; replace with the calculated penalty
            double adjustedExecutionTime = cloudlet.getCloudletLength() / (vm.getMips() * bandwidthPenalty);

            // Update earliest start and latest finish
            if (cloudlet.getExecStartTime() < earliestStartTime) {
                earliestStartTime = cloudlet.getExecStartTime();
            }
            if (cloudlet.getFinishTime() + adjustedExecutionTime > latestFinishTime) {
                latestFinishTime = cloudlet.getFinishTime() + adjustedExecutionTime;
            }
        }
        return latestFinishTime - earliestStartTime;
    }

 // Function to write energy consumption results and task completion time to a file
    private static void writeEnergyResultsToFile(String outputFileName, double totalEnergy, double coolingPower, double totalEnergyWithCooling, double overallTaskCompletionTime) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFileName, true))) {
            writer.println("========== SYSTEM METRICS ==========");
            writer.println("Overall Task Completion Time: " + overallTaskCompletionTime + " seconds");

            writer.println("========== ENERGY CONSUMPTION RESULTS ==========");
            writer.println("Total Energy Consumption: " + totalEnergy + " Watts");
            writer.println("Cooling Power Consumption: " + coolingPower + " Watts");
            writer.println("Total Energy Consumption with Cooling: " + totalEnergyWithCooling + " Watts");
            writer.flush();  // Ensure all data is written to the file
        } catch (IOException e) {
            System.err.println("Error writing energy results to file: " + e.getMessage());
        }
    }


    private static void printCloudletList(List<Cloudlet> list) {
        String indent = "    ";
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
                "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        for (Cloudlet cloudlet : list) {
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");

                Log.printLine(indent + cloudlet.getResourceId() + indent + cloudlet.getVmId() +
                        indent + cloudlet.getActualCPUTime() +
                        indent + cloudlet.getExecStartTime() + indent + cloudlet.getFinishTime());
            }
        }
    }

    private static void writeCloudletResultsToFile(List<Cloudlet> list, String outputFileName) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFileName, true))) {
            String indent = "    ";
            writer.println("========== OUTPUT ==========");
            writer.println("Cloudlet ID" + indent + "STATUS" + indent +
                    "Data center ID" + indent + "VM ID" + indent + "Time" + indent +
                    "Start Time" + indent + "Finish Time");

            for (Cloudlet cloudlet : list) {
                writer.print(indent + cloudlet.getCloudletId() + indent + indent);

                if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
                    writer.print("SUCCESS");

                    writer.println(indent + cloudlet.getResourceId() + indent + cloudlet.getVmId() +
                            indent + cloudlet.getActualCPUTime() +
                            indent + cloudlet.getExecStartTime() + indent + cloudlet.getFinishTime());
                }
            }
            writer.flush();
            System.out.println("Results written to: " + outputFileName);
        } catch (IOException e) {
            System.err.println("Error writing results to file: " + e.getMessage());
        }
    }
}
