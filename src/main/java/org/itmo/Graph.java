package org.itmo;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class Graph {

    private final int V;
    private final ArrayList<Integer>[] adjList;

    Graph(int vertices) {
        this.V = vertices;
        adjList = new ArrayList[vertices];
        for (int i = 0; i < vertices; ++i) {
            adjList[i] = new ArrayList<>();
        }
    }

    void addEdge(int src, int dest) {
        if (!adjList[src].contains(dest)) {
            adjList[src].add(dest);
        }
    }


    void parallelBFS(int startVertex) {

        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        AtomicBoolean[] visited = new AtomicBoolean[V];
        for (int i = 0; i < V; i++) {
            visited[i] = new AtomicBoolean(false);
        }

        try {
            final List<Integer> currentLevel = new ArrayList<>();
            visited[startVertex].set(true);
            currentLevel.add(startVertex);

            while (!currentLevel.isEmpty()) {

                int levelSize = currentLevel.size();
                int actualThreads = Math.min(numThreads, levelSize);
                int batchSize = (int) Math.ceil((double) levelSize / actualThreads);

                CompletableFuture<List<Integer>>[] futures = new CompletableFuture[actualThreads];

                for (int i = 0; i < actualThreads; i++) {
                    final int start = i * batchSize;
                    final int end = Math.min(start + batchSize, levelSize);

                    futures[i] = CompletableFuture.supplyAsync(() -> {
                        List<Integer> localNextLevel = new ArrayList<>();
                        for (int j = start; j < end; j++) {
                            int node = currentLevel.get(j);
                            for (int neighbor : adjList[node]) {
                                if (!visited[neighbor].get() && visited[neighbor].compareAndSet(false, true)) {
                                    localNextLevel.add(neighbor);
                                }
                            }
                        }
                        return localNextLevel;
                    }, executor);
                }



                CompletableFuture.allOf(futures).join();
                currentLevel.clear();
                for (CompletableFuture<List<Integer>> future : futures) {
                    currentLevel.addAll(future.join());
                }

            }
        } finally {
            executor.shutdown();
        }
    }

    Map<Integer, Integer> parallelBFSForJCStress(int startVertex) {

        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        AtomicBoolean[] visited = new AtomicBoolean[V];
        for (int i = 0; i < V; i++) {
            visited[i] = new AtomicBoolean(false);
        }

        AtomicInteger[] visitedCount = new AtomicInteger[V];
        for (int i = 0; i < V; i++) {
            visitedCount[i] = new AtomicInteger(0);
        }

        try {
            final List<Integer> currentLevel = new ArrayList<>();
            visited[startVertex].set(true);
            currentLevel.add(startVertex);
            visitedCount[startVertex].incrementAndGet();

            while (!currentLevel.isEmpty()) {
                int levelSize = currentLevel.size();


                int actualThreads = Math.min(numThreads, levelSize);
                int batchSize = (int) Math.ceil((double) levelSize / actualThreads);

                CompletableFuture<List<Integer>>[] futures = new CompletableFuture[actualThreads];

                for (int i = 0; i < actualThreads; i++) {
                    final int start = i * batchSize;
                    final int end = Math.min(start + batchSize, levelSize);

                    futures[i] = CompletableFuture.supplyAsync(() -> {
                        List<Integer> localNextLevel = new ArrayList<>();
                        for (int j = start; j < end; j++) {
                            int node = currentLevel.get(j);
                            for (int neighbor : adjList[node]) {
                                if (!visited[neighbor].get() && visited[neighbor].compareAndSet(false, true)) {

                                    visitedCount[neighbor].incrementAndGet();
                                    localNextLevel.add(neighbor);
                                }
                            }
                        }
                        return localNextLevel;
                    }, executor);
                }


                CompletableFuture.allOf(futures).join();
                currentLevel.clear();
                for (CompletableFuture<List<Integer>> future : futures) {
                    currentLevel.addAll(future.join());
                }

            }
        } finally {
            executor.shutdown();
        }


        Map<Integer, Integer> result = new HashMap<>();
        for (int i = 0; i < V; i++) {
            result.put(i, visitedCount[i].get());
        }

        return result;

    }

    Map<Integer, Integer> parallelBFSForJCStressWithBug(int startVertex) {


        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        AtomicBoolean[] visited = new AtomicBoolean[V];
        for (int i = 0; i < V; i++) {
            visited[i] = new AtomicBoolean(false);
        }

        AtomicInteger[] visitedCount = new AtomicInteger[V];
        for (int i = 0; i < V; i++) {
            visitedCount[i] = new AtomicInteger(0);
        }

        try {
            final List<Integer> currentLevel = new ArrayList<>();
            visited[startVertex].set(true);
            currentLevel.add(startVertex);
            visitedCount[startVertex].incrementAndGet();

            while (!currentLevel.isEmpty()) {
                int levelSize = currentLevel.size();


                int actualThreads = Math.min(numThreads, levelSize);
                int batchSize = (int) Math.ceil((double) levelSize / actualThreads);

                CompletableFuture<List<Integer>>[] futures = new CompletableFuture[actualThreads];

                for (int i = 0; i < actualThreads; i++) {
                    final int start = i * batchSize;
                    final int end = Math.min(start + batchSize, levelSize);

                    futures[i] = CompletableFuture.supplyAsync(() -> {
                        List<Integer> localNextLevel = new ArrayList<>();
                        for (int j = start; j < end; j++) {
                            int node = currentLevel.get(j);
                            for (int neighbor : adjList[node]) {
                                if (!visited[neighbor].get()) {

                                    visitedCount[neighbor].incrementAndGet();
                                    localNextLevel.add(neighbor);
                                }
                            }
                        }
                        return localNextLevel;
                    }, executor);
                }


                CompletableFuture.allOf(futures).join();
                currentLevel.clear();
                for (CompletableFuture<List<Integer>> future : futures) {
                    currentLevel.addAll(future.join());
                }

            }
        } finally {
            executor.shutdown();
        }


        Map<Integer, Integer> result = new HashMap<>();
        for (int i = 0; i < V; i++) {
            result.put(i, visitedCount[i].get());
        }

        return result;

    }


    //Generated by ChatGPT
    void bfs(int startVertex) {
        boolean[] visited = new boolean[V];

        LinkedList<Integer> queue = new LinkedList<>();

        visited[startVertex] = true;
        queue.add(startVertex);

        while (!queue.isEmpty()) {
            startVertex = queue.poll();

            for (int n : adjList[startVertex]) {
                if (!visited[n]) {
                    visited[n] = true;
                    queue.add(n);
                }
            }
        }
    }

    public void parallelBFSWithCustomThreads(int startVertex, int numThreads) {

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        AtomicBoolean[] visited = new AtomicBoolean[V];
        for (int i = 0; i < V; i++) {
            visited[i] = new AtomicBoolean(false);
        }

        try {
            final List<Integer> currentLevel = new ArrayList<>();
            visited[startVertex].set(true);
            currentLevel.add(startVertex);

            while (!currentLevel.isEmpty()) {

                int levelSize = currentLevel.size();
                int actualThreads = Math.min(numThreads, levelSize);
                int batchSize = (int) Math.ceil((double) levelSize / actualThreads);

                CompletableFuture<List<Integer>>[] futures = new CompletableFuture[actualThreads];

                for (int i = 0; i < actualThreads; i++) {
                    final int start = i * batchSize;
                    final int end = Math.min(start + batchSize, levelSize);

                    futures[i] = CompletableFuture.supplyAsync(() -> {
                        List<Integer> localNextLevel = new ArrayList<>();
                        for (int j = start; j < end; j++) {
                            int node = currentLevel.get(j);
                            for (int neighbor : adjList[node]) {
                                if (!visited[neighbor].get() && visited[neighbor].compareAndSet(false, true)) {
                                    localNextLevel.add(neighbor);
                                }
                            }
                        }
                        return localNextLevel;
                    }, executor);
                }


                CompletableFuture.allOf(futures).join();
                currentLevel.clear();
                for (CompletableFuture<List<Integer>> future : futures) {
                    currentLevel.addAll(future.join());
                }

            }
        } finally {
            executor.shutdown();
        }

    }
}
