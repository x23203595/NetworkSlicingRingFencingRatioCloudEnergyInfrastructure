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

public class IterativeHeuristicNonConvexEnergyAware {

    private static List<Vm> vmList;
    private static List<Cloudlet> cloudletList;
    
 // Parameters for energy consumption with small increments
    private static double zetaCC = 0.995;  // Efficiency of Compute and Cooling
    private static double pComputeCC = 2.0;  // Compute power consumption
    private static double pCoolCC = 4.95;  // Cooling power consumption
    private static double pProcCC = 1.03;  // Processing power consumption
    private static double zetaRF = 1.015;  // Efficiency of Ring Fences
    private static double pStaticRF = 9.925;  // Static power consumption of Ring Fences
    private static double commBandwidth = 1010;  // Communication bandwidth
    private static double commPower = 0.0103;  // Communication power consumption
    private static double tau = 0.00505;  // Chip Design coefficient
    private static double taskExecutionTime = 0.0204;  // Task execution time
    private static double dynamicScalingFactor = 1.04;  // Dynamic Voltage Scaling (DVS) factor
    private static double alphaMax = 0.1025;  // Max latency weight
    private static double betaMin = 0.0205;  // Min latency weight

    public static void main(String[] args) {

        System.out.println("Starting CloudSim simulation...");

        PrintStream originalOut = System.out;  // Save original System.out
        PrintStream originalErr = System.err;  // Save original System.err

        try {
            // Redirect output to a file
            PrintStream out = new PrintStream(new FileOutputStream("C:\\Users\\vinay\\Cloudsim_ResearchProject\\simulation_output_iterativeheuristicnonconvexenergyawareupdatednewthree.txt"));
            System.setOut(out);  // Redirect System.out to the file
            System.setErr(out);  // Redirect System.err to the file

            Log.setOutput(out);

            // Initialize CloudSim for one iteration
            long iterationStart = System.nanoTime();  // Start time for the iteration

            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            // Create Datacenters
            Datacenter datacenter0 = createDatacenter("Datacenter_0", 1);  // Slice 1
            Datacenter datacenter1 = createDatacenter("Datacenter_1", 2);  // Slice 2
            Datacenter datacenter2 = createDatacenter("Datacenter_2", 3);  // Slice 3

            if (datacenter0 == null || datacenter1 == null || datacenter2 == null) {
                throw new RuntimeException("Datacenter creation failed.");
            }

            Log.printLine(datacenter0.getName() + " created.");
            Log.printLine(datacenter1.getName() + " created.");
            Log.printLine(datacenter2.getName() + " created.");

            // Create Broker
            DatacenterBroker broker = createBroker();
            if (broker == null) {
                throw new RuntimeException("Broker creation failed.");
            }
            int brokerId = broker.getId();
            Log.printLine("Broker created with ID: " + brokerId);

            // Create and submit VMs and Cloudlets
            vmList = createVMs(brokerId);
            if (vmList == null || vmList.isEmpty()) {
                throw new RuntimeException("VM creation failed.");
            }
            broker.submitVmList(vmList);  // Submit VMs to the broker
            Log.printLine("VMs submitted to the broker.");

            // Load and submit cloudlets
            cloudletList = loadCloudletsFromCSV(brokerId, "C:/Users/vinay/Research Project/GWA-T-12BitbrainsPreprocessing/preprocessed_fastStorage.csv");
            cloudletList.addAll(loadCloudletsFromCSV(brokerId, "C:/Users/vinay/Research Project/GWA-T-12BitbrainsPreprocessing/preprocessed_rnd.csv"));
            if (cloudletList == null || cloudletList.isEmpty()) {
                throw new RuntimeException("Cloudlet creation failed.");
            }
            broker.submitCloudletList(cloudletList);  // Submit cloudlets to the broker
            Log.printLine("Cloudlets submitted to the broker.");

            // Simulate VM Communication
            simulateVMCommunication();

            // Initialize the newList variable
            List<Cloudlet> newList = null;

            // Start the simulation
            Log.printLine("Starting CloudSim simulation...");
            CloudSim.startSimulation();

            // Retrieve cloudlet results after the simulation ends
            newList = broker.getCloudletReceivedList();
            if (newList == null || newList.isEmpty()) {
                throw new RuntimeException("No cloudlets received after simulation.");
            }
            CloudSim.stopSimulation();
            Log.printLine("Simulation stopped. Cloudlet processing results collected.");
            
            // Calculate Overall Task Completion Time
            double overallTaskCompletionTime = calculateOverallTaskCompletionTime(newList);
            
            // Log Response Time
            Log.printLine("=== System Metrics ===");
            Log.printLine("Overall Task Completion Time: " + overallTaskCompletionTime + " seconds");

            // Energy calculations
            double totalEnergy = calculateTotalEnergyWithLatencyAndDVS(newList, vmList);
            double coolingPower = calculateCoolingPower(totalEnergy);
            double totalEnergyWithCooling = totalEnergy + coolingPower;

            Log.printLine("Total Energy Consumption: " + totalEnergy + " Watts");
            Log.printLine("Cooling Power Consumption: " + coolingPower + " Watts");
            Log.printLine("Total Energy Consumption with Cooling: " + totalEnergyWithCooling + " Watts");

            long iterationEnd = System.nanoTime();  // End time for the iteration
            long iterationTime = iterationEnd - iterationStart;

            Log.printLine("Iteration completed in " + (iterationTime / 1_000_000) + " ms.");
            Log.printLine("Total convergence time: " + (iterationTime / 1_000_000) + " ms.");

            // Print cloudlet results
            printCloudletList(newList);
            writeCloudletResultsToFile(newList, "C:\\Users\\vinay\\Cloudsim_ResearchProject\\simulation_output_iterativeheuristicnonconvexenergyawareupdatednewthree.txt");

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

    // Calculate total energy consumption with latency and dynamic voltage scaling
    private static double calculateTotalEnergyWithLatencyAndDVS(List<Cloudlet> cloudlets, List<Vm> vms) {
        double totalEnergy = 0.0;
        double totalComputeEnergy = (pComputeCC + pCoolCC) / zetaCC;
        double totalProcessingEnergy = 0.0;
        double totalCommEnergy = 0.0;
        double totalStaticEnergyRF = pStaticRF / zetaRF;
        double totalLatencyEnergy = 0.0;
        double dynamicVoltageEnergy = 0.0;

        for (Cloudlet cloudlet : cloudlets) {
            Vm vm = getVmById(vms, cloudlet.getVmId());

            double cpuUsage = cloudlet.getActualCPUTime();  // Get the actual CPU time (in seconds)
            totalProcessingEnergy += pProcCC * cpuUsage;    // Processing energy

            double cpuSpeed = vm.getMips() / 1000.0;  // Normalize CPU speed (in GHz)
            dynamicVoltageEnergy += tau * Math.pow(cpuSpeed * dynamicScalingFactor, 3) * cpuUsage;

            double Dmax = 0.1;  // Placeholder for max latency
            double Dmin = 0.05; // Placeholder for min latency
            totalLatencyEnergy += (alphaMax * Dmax + betaMin * Dmin) * cpuUsage;

            totalLatencyEnergy += tau * Math.pow(cpuSpeed, 3) * taskExecutionTime;  // Task execution energy
        }

        totalCommEnergy = commPower * commBandwidth;

        totalEnergy = totalComputeEnergy + totalProcessingEnergy + totalStaticEnergyRF + totalCommEnergy + totalLatencyEnergy + dynamicVoltageEnergy;

        return totalEnergy;
    }

    // Calculate cooling power consumption based on total energy consumption
    public static double calculateCoolingPower(double totalEnergyConsumption) {
        double coolingFactor = 0.05;  // Revised 5% cooling overhead
        return totalEnergyConsumption * coolingFactor;
    }

    // Helper method to get the VM by ID
    private static Vm getVmById(List<Vm> vms, int vmId) {
        for (Vm vm : vms) {
            if (vm.getId() == vmId) {
                return vm;
            }
        }
        return null;  // Return null if no VM with the specified ID is found
    }

    // Ensure datacenters and brokers are properly created
    private static Datacenter createDatacenter(String name, int numHosts) {
        List<Host> hostList = new ArrayList<>();

        for (int i = 0; i < numHosts; i++) {
            List<Pe> peList = new ArrayList<>();
            int mips = 1000;
            peList.add(new Pe(0, new PeProvisionerSimple(mips)));  // Processing Element (CPU core)

            int ram = 2048; // Host memory (MB)
            long storage = 1000000; // Host storage (MB)
            int bw = 10000; // Bandwidth

            hostList.add(new Host(
                    i,
                    new RamProvisionerSimple(ram),
                    new BwProvisionerSimple(bw),
                    storage,
                    peList,
                    new VmSchedulerTimeShared(peList)
            ));
        }

        String arch = "x86"; // Architecture
        String os = "Linux"; // Operating system
        String vmm = "Xen"; // Virtual machine monitor

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, 10.0, 3.0, 0.05, 0.1, 0.1);

        try {
            return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Create Broker
    private static DatacenterBroker createBroker() {
        try {
            return new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

	    // Create VMs (representing L3 switches in your ring fences)
	    private static List<Vm> createVMs(int brokerId) {
	        List<Vm> vmlist = new ArrayList<>();

	        long size = 10000; // Image size (MB)
	        int ram = 512; // VM memory (MB)
	        int mips = 1000;
	        long bw = 1000;
	        int pesNumber = 1; // Number of CPUs
	        String vmm = "Xen"; // VMM name

	        // Network Slice 1 - 1 ring fence with 3 L3 switches
	        for (int i = 0; i < 3; i++) {
	            Vm vm = new Vm(i, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
	            vmlist.add(vm);
	        }

	        // Network Slice 2 - 2 ring fences, 2 L3 switches in each ring fence (4 total)
	        for (int i = 3; i < 7; i++) {
	            Vm vm = new Vm(i, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
	            vmlist.add(vm);
	        }

	        // Network Slice 3 - 3 ring fences with 1, 2, and 3 L3 switches respectively
	        for (int i = 7; i < 13; i++) {
	            Vm vm = new Vm(i, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
	            vmlist.add(vm);
	        }

	        return vmlist;
	    }

	    // Load and create Cloudlets from the preprocessed CSV files
	    private static List<Cloudlet> loadCloudletsFromCSV(int brokerId, String csvFilePath) {
	        List<Cloudlet> cloudletList = new ArrayList<>();

	        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
	            String line;
	            boolean firstLine = true;

	            while ((line = br.readLine()) != null) {
	                // Skip the header row
	                if (firstLine) {
	                    firstLine = false;
	                    continue;
	                }

	                // Split by comma and trim spaces
	                String[] values = line.split(",");
	                for (int i = 0; i < values.length; i++) {
	                    values[i] = values[i].trim();  // Trim whitespace around values
	                }

	                // Ensure that there are enough columns (at least 11 columns after trimming)
	                if (values.length < 11) {
	                    System.err.println("Invalid row with insufficient columns: " + line);
	                    continue;
	                }

	                try {
	                    // Parse the relevant values
	                    int cores = parseInteger(values[1], 1);  // CPU_cores, default to 1 if parsing fails or is zero
	                    double cpuUsageMHz = parseDouble(values[2], 1000.0);  // CPU_usage_MHz, default to 1000 if invalid

	                    long fileSize = 300;  // Arbitrary file size value
	                    long outputSize = 300;  // Arbitrary output size value

	                    // Create Cloudlet object
	                    Cloudlet cloudlet = new Cloudlet(
	                        cloudletList.size(), (long) cpuUsageMHz, cores, fileSize, outputSize,
	                        new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull()
	                    );
	                    cloudlet.setUserId(brokerId);
	                    cloudletList.add(cloudlet);
	                } catch (NumberFormatException e) {
	                    System.err.println("Invalid number format in row: " + line);
	                    continue;  // Skip invalid rows
	                }
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }

	        return cloudletList;  // Return the list of cloudlets
	    }

	    // Helper method to parse integers with default fallback
	    private static int parseInteger(String value, int defaultValue) {
	        try {
	            int result = Integer.parseInt(value);
	            return (result != 0) ? result : defaultValue;  // Ensure cores are non-zero
	        } catch (NumberFormatException e) {
	            return defaultValue;  // Return default value if parsing fails
	        }
	    }

	    // Helper method to parse doubles with default fallback
	    private static double parseDouble(String value, double defaultValue) {
	        try {
	            return Double.parseDouble(value);
	        } catch (NumberFormatException e) {
	            return defaultValue;  // Return default value if parsing fails
	        }
	    }

	    // Simulate VM-to-VM communication based on the architecture
	    private static void simulateVMCommunication() {
	    	 // Network Slice 1
	        simulateCommunicationBetweenVMs(0, 1, 1000); // Intra-ring communication
	        simulateCommunicationBetweenVMs(1, 0, 1000); // Bidirectional

	        // Network Slice 2
	        simulateCommunicationBetweenVMs(3, 5, 1500); // Inter-ring communication
	        simulateCommunicationBetweenVMs(3, 8, 2000); // Inter-slice communication
	        simulateCommunicationBetweenVMs(4, 8, 2000); // Inter-slice communication
	        simulateCommunicationBetweenVMs(4, 12, 2500); // Inter-slice communication
	        simulateCommunicationBetweenVMs(5, 6, 1500); // Intra-ring communication
	        simulateCommunicationBetweenVMs(5, 3, 1500); // Inter-ring communication
	        simulateCommunicationBetweenVMs(5, 12, 2500); // Inter-slice communication
	        simulateCommunicationBetweenVMs(6, 5, 1500); // Bidirectional

	        // Network Slice 3
	        simulateCommunicationBetweenVMs(7, 11, 1500); // Inter-ring communication
	        simulateCommunicationBetweenVMs(8, 1, 2000); // Inter-slice communication (to Slice 1)
	        simulateCommunicationBetweenVMs(8, 3, 2000); // Inter-slice communication (to Slice 2)
	        simulateCommunicationBetweenVMs(8, 4, 2000); // Inter-slice communication (to Slice 2)
	        simulateCommunicationBetweenVMs(11, 7, 1500); // Bidirectional
	        simulateCommunicationBetweenVMs(12, 4, 2500); // Inter-slice communication (to Slice 2)
	        simulateCommunicationBetweenVMs(12, 5, 2500); // Inter-slice communication (to Slice 2)
	    }

	    // Helper method to simulate communication between two VMs
	    private static void simulateCommunicationBetweenVMs(int vm1, int vm2, double bandwidth) {
	        Log.printLine("Simulating communication between VM " + vm1 + " and VM " + vm2 + " with bandwidth: " + bandwidth);
	        // Placeholder for communication simulation logic (can add delays or resource sharing based on bandwidth)
	    }
	       
	    private static double calculateOverallTaskCompletionTime(List<Cloudlet> cloudlets) {
	        double earliestStartTime = Double.MAX_VALUE;
	        double latestFinishTime = Double.MIN_VALUE;

	        for (Cloudlet cloudlet : cloudlets) {
	            if (cloudlet.getExecStartTime() < earliestStartTime) {
	                earliestStartTime = cloudlet.getExecStartTime();
	            }
	            if (cloudlet.getFinishTime() > latestFinishTime) {
	                latestFinishTime = cloudlet.getFinishTime();
	            }
	        }

	        return latestFinishTime - earliestStartTime;
	    }
	    
	    // Print Cloudlet execution results
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

	    // Function to write the Cloudlet results to a file
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
	            writer.flush();  // Ensure all data is written to the file
	            System.out.println("Results written to: " + outputFileName);
	        } catch (IOException e) {
	            System.err.println("Error writing results to file: " + e.getMessage());
	        }
	    }
}
