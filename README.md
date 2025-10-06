# Лабораторная работа № 1: определение достижимости параллелизма и реализация параллельных алгоритмов.

Шаги выполнения:
1) Выберите один из алгоритмов обхода графа (BFS или BFS).
2) Разберитесь с выбранным алгоритмом и выделите основные этапы его выполнения. Идентифицируйте зависимости между этапами и выберите те, которые можно эффективно распараллелить (для этого постройте граф зависимостей (можно в голове))
3) Напишите программу на выбранном вами языке программирования (java, c++), реализующую выбранный алгоритм с учётом параллельных возможностей.
4) С помощью инструментов (ThreadSanitizer && Helgrind для С++, JCStress тесты для Java) проанализировать программу на предмет отсутствия ошибок синхронизации данных. Если ошибок не нашлось, то внести их и найти.
5) Эксперименты и анализ результатов:\
Проведите эксперименты, измеряя производительность параллельной реализации алгоритма на различных объемах входных данных. Сравните результаты с последовательной версией и опишите полученные выводы.
* Постройте график зависимости времени выполнения параллельной версий алгоритма от выделенных ресурсов.
* Постройте график зависимости времени выполнения параллельной и последовательной версий алгоритма в зависимости от объема входных данных.\
\
**Загрузить графики в отдельную директорию в репозитории** \
**Для построения графиков можно воспользоваться чем угодно**
  
## Решение
## Основные этапы алгоритма:
1) Иницализация. На данном этапе мы иницализируем массив атомарных флагов, которыми мы будем помечать просмотренные вершины, список вершин текущего уровня (только вершина startVertex), пул потоков фиксированного размера.
```java
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        AtomicBoolean[] visited = new AtomicBoolean[V];
        for (int i = 0; i < V; i++) {
            visited[i] = new AtomicBoolean(false);
        }

        
        final List<Integer> currentLevel = new ArrayList<>();
        visited[startVertex].set(true);
        currentLevel.add(startVertex);

```

2) Опеределение размера батча и оптимального размера количества потоков для просмотра текущего уровня вершин. На данном этапе мы определяем количество вершин на текущем уровне, оптимальное количество потоков, которое эффективно использовать и также размер батча - промежуток вершин, которые будет обрабатывать конкретный поток.
```java
        int levelSize = currentLevel.size();
        int actualThreads = Math.min(numThreads, levelSize);
        int batchSize = (int) Math.ceil((double) levelSize / actualThreads);
        CompletableFuture<List<Integer>>[] futures = new CompletableFuture[actualThreads];

```
3) Этап параллельной обработки вершин. Каждый поток обрабатывает свой диапозон вершин. Для обеспечении потокобезопаности мы используем атомарную операцию compareAndSet. Каждый поток собирает локально список каких-то вершин следующего уровня (тут обеспечение потокобезопасности не нужно, так как каждый поток работает со своей переменной). Заметим, что данный этап очень хорошо распараллеливается за счёт разделение диапазона вершин по потокам.
```java
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

```
4) Этап синхронизации. В главном потоке ожидаем выполнения всех рабочих потоков, которые просматривают текущий уровень вершин.
```java

        CompletableFuture.allOf(futures).join();
```
5) Этап объединение результатов. В главном потоке формируем новый текущий уровень вершин, исходя из результатов выполнения потоков.
```java
        currentLevel.clear();
        for (CompletableFuture<List<Integer>> future : futures) {
            currentLevel.addAll(future.join());
        }
```

## Анализ с помощью JCStress.

Напишем тест, который проверяет, что все вершины были посещены один раз. Для этого создадим метод в классе Graph для JCStress.

```java
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
```

И сам тест:

```java
package org.itmo;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.Map;
import java.util.Random;

@JCStressTest
@Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "All 10 vertices visited correctly once")
@Outcome(id = "0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Race condition: some vertices missed")
@Outcome(id = "2", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Some vertices have been visited several times.")
@State
public class JCStressTestParallelBfs {

    private final Graph graph = new RandomGraphGenerator().generateGraph(new Random(42), 10, 50);
    private final long EXPECTED_COUNT_OF_VERTEX = 10;

    @Actor
    public void actor(I_Result r) {

        Map<Integer, Integer> result = graph.parallelBFSForJCStress(0);

        boolean flag = result.keySet().size() == EXPECTED_COUNT_OF_VERTEX;

        if (!flag) {
            r.r1 = 0;
            return;
        }


        for (Map.Entry<Integer, Integer> entry : result.entrySet()) {

            if (entry.getValue() != 1){
                flag = false;
                break;
            }

        }

        r.r1 = flag ? 1 : 2;
    }


}
```

Результаты теста:

<img width="2520" height="706" alt="image" src="https://github.com/user-attachments/assets/3495e136-f06e-42a0-8017-3c80ed7a6db4" />

Результаты говорят о том, что все вершины всегда были посещены один раз.
Теперь создадим метод, в котором специально уберём CAS и создадим гонку данных.

```java
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
```

И результаты для теста с таким методом:

<img width="2523" height="690" alt="image" src="https://github.com/user-attachments/assets/fb29853a-279b-4c6e-aeaa-11c6f998de01" />

Как мы видим абсолютно всегда какая-то вершина была посещена несколько раз из-за отсутствия синхронизации.

## Анализ результатов

Построем график зависимости времени выполнения параллельной и последовательной версий алгоритма в зависимости от объема входных данных:

<img width="1189" height="790" alt="загруженное (1)" src="https://github.com/user-attachments/assets/0f608020-4c64-4b3b-8416-d4ef98c4d3cc" />

Параллельная версия BFS эффективна только на больших графах (от 100,000+ вершин), где она показывает ускорение в 2-4 раза. На маленьких графах параллелизация неэффективна из-за накладных расходов.


Построем график зависимости времени выполнения параллельной версий алгоритма от выделенных ресурсов:

<img width="1189" height="590" alt="загруженное" src="https://github.com/user-attachments/assets/4326505b-ac96-408d-b10c-2938909fa674" />

Параллельный BFS эффективно масштабируется до 16-20 потоков. Дальнейшее увеличение потоков не дает значительного прироста.







