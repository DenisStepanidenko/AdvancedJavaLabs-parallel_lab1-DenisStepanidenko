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
