package org.apache.flink.streaming.examples.allowlatency;

import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.accumulators.LongCounter;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.typeinfo.IntegerTypeInfo;
import org.apache.flink.api.java.typeutils.TupleTypeInfo;
import org.apache.flink.api.java.utils.MultipleParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

/**
 * Example illustrating a windowed stream join between two data streams.
 *
 * <p>The example works on two input streams with pairs (name, grade) and (name, salary)
 * respectively. It joins the streams based on "name" within a configurable window.
 *
 * <p>The example uses a built-in sample data generator that generates the streams of pairs at a
 * configurable rate.
 */
public class AllowLatency {
    public static void main(String[] args) throws Exception {

        final MultipleParameterTool params = MultipleParameterTool.fromArgs(args);

        Configuration config = new Configuration();
        config.setBoolean(DeploymentOptions.ATTACHED, true);
        final StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment(config);

        env.getConfig().enableObjectReuse();
        env.getConfig().disableGenericTypes();
        env.setRestartStrategy(RestartStrategies.noRestart());
        env.setParallelism(1);
        env.getConfig().setAutoWatermarkInterval(0);
        env.getCheckpointConfig().disableCheckpointing();

        env.setStateBackend(new EmbeddedRocksDBStateBackend());
        env.getConfig().setAllowedLatency(10000);

        long dataNum = (long) 1e8;
        if (params.has("dataNum")) {
            dataNum = Long.parseLong(params.get("dataNum"));
            System.out.println("Number of records: " + dataNum);
        }

        DataStream<Integer> ds1 = env.addSource(new MyJoinSource(dataNum));
        //        ds1.print();
        ds1.keyBy(value -> value)
                .transform(
                        "MyAggregator",
                        new TupleTypeInfo<>(
                                IntegerTypeInfo.INT_TYPE_INFO, IntegerTypeInfo.LONG_TYPE_INFO),
                        new MyAggregator(value -> value))
                .addSink(new CountingAndDiscardingSink<>());

        long startTime = System.currentTimeMillis();

        JobExecutionResult executionResult = env.execute("");
        long endTime = System.currentTimeMillis();
        System.out.println("Duration: " + (endTime - startTime));
        System.out.println("Throughput: " + (dataNum / (endTime - startTime)));
    }

    private static class CountingAndDiscardingSink<T> extends RichSinkFunction<T> {
        public static final String COUNTER_NAME = "numElements";

        private static final long serialVersionUID = 1L;

        private final LongCounter numElementsCounter = new LongCounter();

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            getRuntimeContext().addAccumulator(COUNTER_NAME, numElementsCounter);
        }

        @Override
        public void invoke(T value, Context context) {
            numElementsCounter.add(1L);
        }
    }
}
