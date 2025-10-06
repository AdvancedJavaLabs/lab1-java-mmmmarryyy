package org.itmo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

class Graph {
    private final int V;
    private int E;
    private final ArrayList<Integer>[] adjList;
    private AtomicInteger visitedCounter;

    Graph(int vertices) {
        this.V = vertices;
        this.E = 0;
        adjList = new ArrayList[vertices];

        for (int i = 0; i < vertices; ++i) {
            adjList[i] = new ArrayList<>();
        }

        visitedCounter = new AtomicInteger(0);
    }

    void addEdge(int src, int dest) {
        if (!adjList[src].contains(dest)) {
            adjList[src].add(dest);
            E++;
        }
    }

    int getVisitedCounterValue() {
        return visitedCounter.get();
    }

    int getNumberOfVertices() {
        return V;
    }

    private void processVertex(
            int vertex,
            AtomicIntegerArray visited,
            ConcurrentLinkedQueue<Integer> nextLevel
    ) {
        ArrayList<Integer> neighbors = adjList[vertex];

        for (int neighbor : neighbors) {
            if (visited.compareAndSet(neighbor, 0, 1)) {
                visitedCounter.incrementAndGet();
                nextLevel.add(neighbor);
            }
        }
    }

    private void brokenProcessVertex(
            int vertex,
            AtomicIntegerArray visited,
            ConcurrentLinkedQueue<Integer> nextLevel
    ) {
        ArrayList<Integer> neighbors = adjList[vertex];

        for (int neighbor : neighbors) {
            // НАМЕРЕННАЯ ОШИБКА: разделяем проверку и установку
            if (visited.get(neighbor) == 0) {
                visited.set(neighbor, 1);
                visitedCounter.incrementAndGet();
                nextLevel.add(neighbor);
            }
        }
    }

    private void processLevelInParallel(
            ConcurrentLinkedQueue<Integer> currentLevel,
            int numberOfThreads,
            AtomicIntegerArray visited,
            ConcurrentLinkedQueue<Integer> nextLevel,
            ExecutorService executor,
            boolean broken
    ) {
        CountDownLatch levelCompletionLatch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.execute(() -> {
                try {
                    while (true) {
                        Integer vertex = currentLevel.poll();
                        if (vertex == null) {
                            break;
                        }

                        if (broken) {
                            brokenProcessVertex(vertex, visited, nextLevel);
                        } else {
                            processVertex(vertex, visited, nextLevel);
                        }
                    }
                } finally {
                    levelCompletionLatch.countDown();
                }
            });
        }

        try {
            levelCompletionLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    void parallelBFS(int startVertex) {
        System.out.println("Вызываем bfs для графа с количеством вершин = " + V + "; количеством ребер = " + E);
        if (V < 1000 || E < V * 5) {
            System.out.println("Скорее всего эффективнее будет использовать однопоточную реализацию");
            bfs(startVertex);
            return;
        }

        int numberOfThreads = Math.min(Runtime.getRuntime().availableProcessors(), 4);
        parallelBFS(startVertex, numberOfThreads);
    }

    void brokenParallelBFS(int startVertex, int numberOfThreads) {
        parallelBFS(startVertex, numberOfThreads, 10, true);
    }

    void parallelBFS(int startVertex, int numberOfThreads) {
        parallelBFS(startVertex, numberOfThreads, 10, false);
    }

    void parallelBFS(int startVertex, int numberOfThreads, int parallelismThresholdConstant, boolean broken) {
        if (startVertex < 0 || startVertex >= V) {
            throw new IllegalArgumentException("Invalid start vertex: " + startVertex);
        }
        if (numberOfThreads <= 0) {
            throw new IllegalArgumentException("Invalid number of threads: " + numberOfThreads);
        }

        AtomicIntegerArray visited = new AtomicIntegerArray(V);

        if (!visited.compareAndSet(startVertex, 0, 1)) {
            return;
        }
        visitedCounter.incrementAndGet();

        ConcurrentLinkedQueue<Integer> currentLevel = new ConcurrentLinkedQueue<>();
        currentLevel.add(startVertex);

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        try {
            while (!currentLevel.isEmpty()) {
                ConcurrentLinkedQueue<Integer> nextLevel = new ConcurrentLinkedQueue<>();
                int levelSize = currentLevel.size();

                if (levelSize < numberOfThreads * parallelismThresholdConstant) {
                    Integer vertex;
                    while ((vertex = currentLevel.poll()) != null) {
                        if (broken) {
                            brokenProcessVertex(vertex, visited, nextLevel);
                        } else {
                            processVertex(vertex, visited, nextLevel);
                        }
                    }
                } else {
                    processLevelInParallel(currentLevel, numberOfThreads, visited, nextLevel, executor, broken);
                }

                currentLevel = nextLevel;
            }
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException exception) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    //Generated by ChatGPT
    void bfs(int startVertex) {
        boolean[] visited = new boolean[V];

        LinkedList<Integer> queue = new LinkedList<>();

        visited[startVertex] = true;
        queue.add(startVertex);
        visitedCounter.incrementAndGet();

        while (!queue.isEmpty()) {
            startVertex = queue.poll();

            for (int n : adjList[startVertex]) {
                if (!visited[n]) {
                    visited[n] = true;
                    visitedCounter.incrementAndGet();
                    queue.add(n);
                }
            }
        }
    }

}
