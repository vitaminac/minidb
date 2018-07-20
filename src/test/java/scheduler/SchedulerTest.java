package scheduler;

import promise.Promise;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class SchedulerTest {
    @Test
    public void test() {
        List<Integer> order = new ArrayList<>();
        Promise.create((DeferredTask<Integer>) (resolver, rejecter) -> Scheduler.defer(() -> {
            order.add(6);
            resolver.resolve(6);
        }, 2000)).onFulfilled(value -> {
            order.add(7);
            return 7;
        }).onFulfilled((result -> {
            order.add(8);
            throw new Exception("8");
        })).onRejected(e -> {
            order.add(9);
            return 9;
        }).onRejected(e -> {
            order.add(10);
            return 10;
        }).onFinally(() -> order.add(11));

        Promise.create((DeferredTask<Integer>) (resolver, rejecter) -> Scheduler.defer(() -> {
            order.add(1);
            rejecter.reject(new Exception("1"));
        }, 1500)).then(result -> {
            order.add(2);
            return 2;
        }, e -> {
            order.add(3);
            return 3;
        }).onRejected(e -> {
            order.add(4);
            return 4;
        }).onFinally(() -> order.add(5));

        Scheduler.run();
        int[] result = order.stream().mapToInt(i -> i).toArray();
        System.out.println(Arrays.toString(result));
        assertArrayEquals(new int[]{1, 3, 5, 6, 7, 8, 9, 11}, result);
    }

}