package org.itmo;

import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.util.HashSet;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

public class BFSTest {

    @Test
    public void bfsTest() throws IOException {
        int[] sizes = new int[]{2_000_000, 1_000_000, 100_000, 100_000, 50_000, 50_000, 10_000, 10_000, 10_000, 1_000, 1_000, 1_000, 1_000, 100, 100, 10, 10, 10};
        int[] connections = new int[]{10_000_000, 10_000_000, 5_000_000, 2_000_000, 5_000_000, 1_000_000, 5_000_000, 2_000_000, 1_000_000, 900_000, 500_000, 100_000, 50_000, 9_900, 5_000, 90, 50, 10};

        Random r = new Random(42);
        int approximateConst = 5;

        try (FileWriter fw = new FileWriter("results/results.txt")) {
            for (int i = 0; i < sizes.length; i++) {
                System.out.println("--------------------------");
                System.out.println("Generating graph of size " + sizes[i] + " ...wait");

                long serialTime = 0;
                long parallelTime = 0;

                for (int j = 0; j < approximateConst; j++) {
                    Graph g = new RandomGraphGenerator().generateGraph(r, sizes[i], connections[i]);
                    System.out.println("Generation " + j + " completed\nStarting bfs");

                    serialTime += executeSerialBfsAndGetTime(g);
                    parallelTime += executeParallelBfsAndGetTime(g);

                    if (g.getVisitedCounterValue() != g.getNumberOfVertices() * 2) {
                        System.out.println("[WARN] graph.getVisitedCounterValue() = " + g.getVisitedCounterValue() / 2 + "\ngraph.getNumberOfVertices() = " + g.getNumberOfVertices());
                    }

                    Runtime.getRuntime().gc();
                }

                serialTime /= approximateConst;
                parallelTime /= approximateConst;

                fw.append("Times for " + sizes[i] + " vertices and " + connections[i] + " connections: ");
                fw.append("\nSerial: " + serialTime);
                fw.append("\nParallel: " + parallelTime);
                fw.append("\n--------\n");
            }

            fw.flush();
        }
    }

    @Test
    public void threadCountExperiment() throws IOException {
        int[] threadCounts = {1, 2, 4, 8, 12, 16, 20, 24, 28, 32};
        int graphSize = 2_000_000;
        int connections = 10_000_000;
        int approximateConst = 10;

        Graph g = new RandomGraphGenerator().generateGraph(new Random(42), graphSize, connections);

        try (FileWriter fw = new FileWriter("results/thread_experiment.txt")) {
            fw.append("Threads,Time\n");

            for (int threads : threadCounts) {
                long totalTime = 0;

                for (int i = 0; i < approximateConst; i++) {
                    totalTime += executeParallelBFSWithThreads(g, threads);
                    Runtime.getRuntime().gc();
                }

                long avgTime = totalTime / approximateConst;
                fw.append(threads + "," + avgTime + "\n");
            }
        }
    }

    @Test
    public void thresholdConstantExperiment() throws IOException {
        int[] constants = {1, 2, 3, 4, 6, 8, 10, 12, 14};
        int graphSize = 2_000_000;
        int connections = 10_000_000;
        int threads = 4;
        int approximateConst = 10;

        Graph g = new RandomGraphGenerator().generateGraph(new Random(42), graphSize, connections);

        try (FileWriter fw = new FileWriter("results/threshold_constant_experiment.txt")) {
            fw.append("Constant,Time\n");

            for (int constant : constants) {
                long totalTime = 0;

                for (int i = 0; i < approximateConst; i++) {
                    totalTime += executeParallelBFSWithThreadsAndConstant(g, threads, constant);
                    Runtime.getRuntime().gc();
                }

                long avgTime = totalTime / approximateConst;
                fw.append(constant + "," + avgTime + "\n");
            }
        }
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

    private long executeParallelBFSWithThreads(Graph g, int numberOfThreads) {
        long startTime = System.currentTimeMillis();
        g.parallelBFS(0, numberOfThreads);
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    private long executeParallelBFSWithThreadsAndConstant(Graph g, int numberOfThreads, int constant) {
        long startTime = System.currentTimeMillis();
        g.parallelBFS(0, numberOfThreads, constant, false);
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

}
