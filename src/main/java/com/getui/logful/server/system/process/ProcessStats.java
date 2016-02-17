package com.getui.logful.server.system.process;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

public class ProcessStats {
    private static final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
    private static final Method openFileDescriptorCountMethod =
            findMethod("getOpenFileDescriptorCount", operatingSystemMXBean.getClass());
    private static final Method maxFileDescriptorCountMethod =
            findMethod("getMaxFileDescriptorCount", operatingSystemMXBean.getClass());
    private static final long PID = findPid();

    private static Method findMethod(final String methodName, final Class<?> clazz) {
        try {
            final Method method = clazz.getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method;
        } catch (Exception e) {
            return null;
        }
    }

    private static long findPid() {
        try {
            final String processId = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
            return Long.parseLong(processId);
        } catch (Exception e) {
            return -1L;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeMethod(final Method method, Object object, T defaultValue) {
        try {
            return (T) openFileDescriptorCountMethod.invoke(operatingSystemMXBean);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    static long getOpenFileDescriptorCount() {
        return invokeMethod(openFileDescriptorCountMethod, operatingSystemMXBean, -1L);
    }

    static long getMaxFileDescriptorCount() {
        return invokeMethod(maxFileDescriptorCountMethod, operatingSystemMXBean, -1L);
    }

    @JsonProperty
    public long pid;

    @JsonProperty
    public long openFileDescriptors;

    @JsonProperty
    public long maxFileDescriptors;

    @JsonProperty
    public Memory memory;

    public static ProcessStats create() {
        ProcessStats stats = new ProcessStats();
        stats.pid = PID;
        stats.openFileDescriptors = getOpenFileDescriptorCount();
        stats.maxFileDescriptors = getMaxFileDescriptorCount();

        Runtime runtime = Runtime.getRuntime();
        stats.memory = Memory.create(runtime.freeMemory(), runtime.totalMemory(), runtime.maxMemory());

        return stats;
    }

    @JsonAutoDetect
    public static class Memory {

        @JsonProperty
        public long freeMemory;

        @JsonProperty
        public long totalMemory;

        @JsonProperty
        public long maxMemory;

        public static Memory create(long freeMemory,
                                    long totalMemory,
                                    long maxMemory) {
            Memory memory = new Memory();
            memory.freeMemory = freeMemory;
            memory.totalMemory = totalMemory;
            memory.maxMemory = maxMemory;
            return memory;
        }

    }

}