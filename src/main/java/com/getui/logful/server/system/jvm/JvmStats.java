package com.getui.logful.server.system.jvm;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.List;

@JsonAutoDetect
public class JvmStats {

    public static final JvmStats INSTANCE;

    static {
        final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        final long heapInit = memoryMXBean.getHeapMemoryUsage().getInit();
        final long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        final long nonHeapInit = memoryMXBean.getNonHeapMemoryUsage().getInit();
        final long nonHeapMax = memoryMXBean.getNonHeapMemoryUsage().getMax();
        long directMemoryMax;
        try {
            Class<?> vmClass = Class.forName("sun.misc.VM");
            directMemoryMax = (long) vmClass.getMethod("maxDirectMemory").invoke(null);
        } catch (Throwable t) {
            directMemoryMax = -1;
        }
        final Memory memory = Memory.create(heapInit, heapMax, nonHeapInit, nonHeapMax, directMemoryMax);

        final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        final long startTime = runtimeMXBean.getStartTime();
        final String version = runtimeMXBean.getSystemProperties().get("java.version");
        final String vmName = runtimeMXBean.getVmName();
        final String vmVendor = runtimeMXBean.getVmVendor();
        final String vmVersion = runtimeMXBean.getVmVersion();

        final String specName = runtimeMXBean.getSpecName();
        final String specVendor = runtimeMXBean.getSpecVendor();
        final String specVersion = runtimeMXBean.getSpecVersion();

        final List<String> inputArguments = runtimeMXBean.getInputArguments();
        //final String bootClassPath = runtimeMXBean.getBootClassPath();
        //final String classPath = runtimeMXBean.getClassPath();

        //final Map<String, String> systemProperties = runtimeMXBean.getSystemProperties();

        final List<GarbageCollectorMXBean> gcMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
        final List<String> garbageCollectors = new ArrayList<>(gcMxBeans.size());
        for (GarbageCollectorMXBean gcMxBean : gcMxBeans) {
            garbageCollectors.add(gcMxBean.getName());
        }

        final List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        final List<String> memoryPools = new ArrayList<>(memoryPoolMXBeans.size());
        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
            memoryPools.add(memoryPoolMXBean.getName());
        }

        INSTANCE = JvmStats.create(version, vmName, vmVersion, vmVendor, specName, specVersion, specVendor, startTime, memory, inputArguments, garbageCollectors, memoryPools);
    }

    @JsonProperty
    public String version;

    @JsonProperty
    public String vmName;

    @JsonProperty
    public String vmVersion;

    @JsonProperty
    public String vmVendor;

    @JsonProperty
    public String specName;

    @JsonProperty
    public String specVersion;

    @JsonProperty
    public String specVendor;

    @JsonProperty
    public long startTime;

    @JsonProperty
    public Memory memory;

    @JsonProperty
    public List<String> inputArguments;

    //@JsonProperty
    //public String bootClassPath;

    //@JsonProperty
    //public String classPath;

    //@JsonProperty
    //public Map<String, String> systemProperties;

    @JsonProperty
    public List<String> garbageCollectors;

    @JsonProperty
    public List<String> memoryPools;

    public static JvmStats create(String version,
                                  String vmName,
                                  String vmVersion,
                                  String vmVendor,
                                  String specName,
                                  String specVersion,
                                  String specVendor,
                                  long startTime,
                                  JvmStats.Memory memory,
                                  List<String> inputArguments,
                                  List<String> garbageCollectors,
                                  List<String> memoryPools) {
        JvmStats jvmStats = new JvmStats();
        jvmStats.version = version;
        jvmStats.vmName = vmName;
        jvmStats.vmVersion = vmVersion;
        jvmStats.vmVendor = vmVendor;
        jvmStats.specName = specName;
        jvmStats.specVersion = specVersion;
        jvmStats.specVendor = specVendor;
        jvmStats.startTime = startTime;
        jvmStats.memory = memory;
        jvmStats.inputArguments = inputArguments;
        jvmStats.garbageCollectors = garbageCollectors;
        jvmStats.memoryPools = memoryPools;
        return jvmStats;
    }

    @JsonAutoDetect
    public static class Memory {

        @JsonProperty
        public long heapInit;

        @JsonProperty
        public long heapMax;

        @JsonProperty
        public long nonHeapInit;

        @JsonProperty
        public long nonHeapMax;

        @JsonProperty
        public long directMemoryMax;

        public static Memory create(long heapInit,
                                    long heapMax,
                                    long nonHeapInit,
                                    long nonHeapMax,
                                    long directMemoryMax) {
            Memory memory = new Memory();
            memory.heapInit = heapInit;
            memory.heapMax = heapMax;
            memory.nonHeapInit = nonHeapInit;
            memory.nonHeapMax = nonHeapMax;
            memory.directMemoryMax = directMemoryMax;
            return memory;
        }
    }
}
