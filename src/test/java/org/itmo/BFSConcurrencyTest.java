package org.itmo;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.Z_Result;

import java.util.Random;

@JCStressTest
@Outcome(id = "true", expect = Expect.ACCEPTABLE, desc = "Все вершины посещены по одному разу")
@Outcome(id = "false", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Обнаружена гонка данных: количество посещенных вершин != количеству вершин в графе")
@State
public class BFSConcurrencyTest {

    private Graph graph = null;

    private void setup() {
        Random r = new Random(42);
        graph = new RandomGraphGenerator().generateGraph(r, 50_000, 5_000_000);
    }

    @Actor
    public void actor() {
        setup();
        graph.parallelBFS(0, 8); // or brokenParallelBFS
    }

    @Arbiter
    public void arbiter(Z_Result r) {
        r.r1 = (graph.getVisitedCounterValue() == graph.getNumberOfVertices());
    }
}
