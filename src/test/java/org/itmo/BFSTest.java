package org.itmo;

import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

public class BFSTest {

    @Test
    public void bfsTest() throws IOException {
        int[] sizes = new int[]{10, 100, 1000, 10_000, 10_000, 50_000, 100_000, 1_000_000, 2_000_000, 5_000_000};
        int[] connections = new int[]{50, 500, 5000, 50_000, 100_000, 1_000_000, 1_000_000, 10_000_000, 10_000_000, 25_000_000};
        Random r = new Random(42);
        try (FileWriter fw = new FileWriter("tmp/results.txt")) {
            for (int i = 0; i < sizes.length; i++) {
                System.out.println("--------------------------");
                System.out.println("Generating graph of size " + sizes[i] + " ...wait");
                Graph g = new RandomGraphGenerator().generateGraph(r, sizes[i], connections[i]);
                System.out.println("Generation completed!\nStarting bfs");
                long serialTime = executeSerialBfsAndGetTime(g);
                long parallelTime = executeParallelBfsAndGetTime(g);
                fw.append("Times for " + sizes[i] + " vertices and " + connections[i] + " connections: ");
                fw.append("\nSerial: " + serialTime);
                fw.append("\nParallel: " + parallelTime);
                fw.append("\n--------\n");
            }
            fw.flush();
        }
    }

    @Test
    public void bfsTestWithCustomThread() throws IOException {

        int graphSize = 5_000_000;
        int connections = 25_000_000;
        Random r = new Random(42);

        Graph g = new RandomGraphGenerator().generateGraph(r, graphSize, connections);


        List<Integer> threadCounts = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            threadCounts.add(i);
        }

        try (FileWriter fw = new FileWriter("tmp/thread_scaling_results.txt")) {
            fw.append("Thread Scaling Test for " + graphSize + " vertices and " + connections + " connections:\n");

            for (int numThreads : threadCounts) {
                long time = executeParallelBfsWithThreads(g, numThreads);

                fw.append("Threads: " + numThreads + ", Time: " + time + " ms");
                fw.append("\n--------\n");
            }
            fw.flush();
        }
    }

    private long executeParallelBfsWithThreads(Graph g, int numThreads) {

        long startTime = System.currentTimeMillis();
        g.parallelBFSWithCustomThreads(0, numThreads);
        long endTime = System.currentTimeMillis();
        return endTime - startTime;

    }


    private long executeSerialBfsAndGetTime(Graph g) {

        long startTime = System.currentTimeMillis();
        g.bfs(0);
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    private long executeParallelBfsAndGetTime(Graph g) {
        long startTime = System.currentTimeMillis();
        g.parallelBFS(0);
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

}
