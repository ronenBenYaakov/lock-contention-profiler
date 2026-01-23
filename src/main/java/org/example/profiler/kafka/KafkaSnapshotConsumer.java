package org.example.profiler.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.profiler.analysis.LockContentionAccumulator;
import org.example.profiler.monitor.LockEvent;
import org.example.profiler.monitor.ThreadSnapshot;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaSnapshotConsumer {

    private final KafkaConsumer<String, ThreadSnapshot> consumer;
    private final ConcurrentHashMap<String, LockContentionAccumulator> lockMap;
        private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * Construct a consumer for ThreadSnapshots.
     *
     * @param bootstrapServers Kafka bootstrap servers
     * @param topic            Kafka topic to subscribe to
     * @param groupId          Kafka consumer group ID
     * @param lockMap          Shared concurrent map to accumulate LockContention
     */
    public KafkaSnapshotConsumer(String bootstrapServers,
                                 String topic,
                                 String groupId,
                                 ConcurrentHashMap<String, LockContentionAccumulator> lockMap) {
        this.lockMap = lockMap;

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.example.profiler.kafka.ThreadSnapshotDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // start from newest messages
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        this.consumer = new KafkaConsumer<>(props);
        this.consumer.subscribe(Collections.singletonList(topic));
    }

    /**
     * Polls Kafka continuously and updates lock contention accumulators.
     */
    public void start() {
        while (running.get()) {
            ConsumerRecords<String, ThreadSnapshot> records = consumer.poll(Duration.ofMillis(100));

            // Flatten all records into a list for parallel processing
                List<ConsumerRecord<String, ThreadSnapshot>> recordList = new ArrayList<>();
            records.partitions().forEach(tp -> recordList.addAll(records.records(tp)));

            // Process snapshots in parallel
            recordList.parallelStream().forEach(record -> {
                ThreadSnapshot snapshot = record.value();
                if (snapshot == null) return;

                LockEvent waitLock = snapshot.lockWaitingOn();
                if (waitLock != null && waitLock.isOwned()) {
                    String lockId = waitLock.getLockId();

                    LockContentionAccumulator acc = lockMap.computeIfAbsent(lockId, id ->
                            new LockContentionAccumulator(
                                    waitLock.getLockId(),
                                    waitLock.getLockName(),
                                    waitLock.getLockType())
                    );

                    // For simplicity, prev snapshot = current snapshot
                    acc.recordBlock(waitLock, snapshot, snapshot);
                }
            });
        }
    }


    /**
     * Stop the consumer.
     */
    public void stop() {
        running.set(false);
        consumer.wakeup();
        consumer.close(Duration.ofSeconds(5));
    }
}