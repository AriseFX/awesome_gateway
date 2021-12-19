package com.ewell.core.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.netty.util.internal.PlatformDependent.usedDirectMemory;

/**
 * @Author: wy
 * @Date: Created in 9:16 下午 2021/11/21
 * @Description:
 * @Modified: By：
 */
public class MetricsCollector {

    static MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    public String collect() {
        MonitorHandler instance = MonitorHandler.INSTANCE;
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage offHeap = memoryMXBean.getNonHeapMemoryUsage();
        List<Metric<? extends Number>> metrics = new ArrayList<>();
        metrics.add(new Metric<>(MetricType.gauge, "网络吞吐量", "read", instance.getRead()));
        metrics.add(new Metric<>(MetricType.gauge, "网络吞吐量", "write", instance.getWrite()));
        metrics.add(new Metric<>(MetricType.gauge, "api吞吐量", "api", instance.getApiTps()));
        metrics.add(new Metric<>(MetricType.gauge, "netty直接内存", "direct_memory_total", usedDirectMemory()));
        metrics.add(new Metric<>(MetricType.gauge, "堆内存使用量", "heap_memory_usage", heap.getUsed()));
        metrics.add(new Metric<>(MetricType.gauge, "堆内存容量", "heap_memory_committed", heap.getCommitted()));
        metrics.add(new Metric<>(MetricType.gauge, "非堆内存使用量", "offHeap_memory_usage", offHeap.getUsed()));
        metrics.add(new Metric<>(MetricType.gauge, "非堆内存容量", "offHeap_memory_committed", offHeap.getCommitted()));
        return metrics.stream().map(Metric::toString).collect(Collectors.joining());
    }
}
