package org.apache.flink.streaming.examples.allowlatency;

import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.IntegerTypeInfo;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.metrics.Gauge;
import org.apache.flink.runtime.state.StateInitializationContext;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.operators.Output;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;

import java.util.HashMap;
import java.util.Map;

/**
 * Example illustrating a windowed stream join between two data streams.
 *
 * <p>The example works on two input streams with pairs (name, grade) and (name, salary)
 * respectively. It joins the streams based on "name" within a configurable window.
 *
 * <p>The example uses a built-in sample data generator that generates the streams of pairs at a
 * configurable rate.
 */
public class MyAggregator extends AbstractStreamOperator<Tuple2<Integer, Long>>
        implements OneInputStreamOperator<Integer, Tuple2<Integer, Long>> {
    private Map<Integer, Long> bundle;
    private final KeySelector<Integer, Integer> keySelector;
    private int numOfElements;
    private MapState<Integer, Long> kvStore;
    private long visits;

    public MyAggregator(KeySelector<Integer, Integer> keySelector) {
        super();
        this.keySelector = keySelector;
    }

    private Integer getKey(Integer input) throws Exception {
        return keySelector.getKey(input);
    }

    @Override
    public void initializeState(StateInitializationContext context) throws Exception {
        super.initializeState(context);

        kvStore =
                context.getKeyedStateStore()
                        .getMapState(
                                new MapStateDescriptor<Integer, Long>(
                                        "KV-Store",
                                        IntegerTypeInfo.INT_TYPE_INFO,
                                        IntegerTypeInfo.LONG_TYPE_INFO));
    }

    @Override
    public void open() throws Exception {
        super.open();
        this.numOfElements = 0;
        this.bundle = new HashMap<>();
        visits = 0;

        // counter metric to get the size of bundle
        getRuntimeContext()
                .getMetricGroup()
                .gauge("bundleSize", (Gauge<Integer>) () -> numOfElements);
    }

    @Override
    public void processElement(StreamRecord<Integer> element) throws Exception {
        Integer input = element.getValue();
        if (getExecutionConfig().getAllowedLatency() > 0) {
            Integer bundleKey = getKey(input);
            Long bundleValue = bundle.get(bundleKey);
            // get a new value after adding this element to bundle
            Long newBundleValue = bundleValue == null ? 1 : bundleValue + 1;
            bundle.put(bundleKey, newBundleValue);
        } else {
            visits += 1;
            Integer k = getKey(input);
            Long storeValue = kvStore.get(k);
            Long newStoreValue = storeValue == null ? 1 : storeValue + 1;
            kvStore.put(k, newStoreValue);
            output.collect(new StreamRecord<>(new Tuple2<>(k, newStoreValue)));
        }
    }

    @Override
    public void finish() throws Exception {
        System.out.println("RocksDB visits: " + visits);
    }

    public void finishBundle() {
        if (bundle != null && !bundle.isEmpty()) {
            numOfElements = 0;
            finishBundle(bundle, output);
            bundle.clear();
        }
    }

    public void finishBundle(
            Map<Integer, Long> bundle, Output<StreamRecord<Tuple2<Integer, Long>>> output) {
        bundle.forEach(
                (k, v) -> {
                    try {
                        visits += 1;
                        Long storeValue = kvStore.get(k);
                        Long newStoreValue = storeValue == null ? v : storeValue + v;
                        kvStore.put(k, newStoreValue);
                        output.collect(new StreamRecord<>(new Tuple2<>(k, newStoreValue)));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public void flush() {
        finishBundle();
    }
}
