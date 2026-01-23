package org.example.profiler.kafka;

import org.apache.kafka.clients.producer.*;
import org.example.profiler.monitor.ThreadSnapshot;

import java.time.Duration;
import java.util.Properties;

public class KafkaSnapshotProducer {

    private final KafkaProducer<String, ThreadSnapshot> producer;
    private final String topic;

    /**
     * Construct a producer for ThreadSnapshots.
     *
     * @param bootstrapServers Kafka bootstrap servers, e.g., "localhost:9092"
     * @param topic            Kafka topic to publish snapshots
     */
    public KafkaSnapshotProducer(String bootstrapServers, String topic) {
        this.topic = topic;

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.example.profiler.kafka.ThreadSnapshotSerializer");

        // Optional performance configs
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);

        this.producer = new KafkaProducer<>(props);
    }

    /**
     * Send a ThreadSnapshot to Kafka asynchronously.
     *
     * @param snapshot ThreadSnapshot to send
     */
    public void send(ThreadSnapshot snapshot) {
        ProducerRecord<String, ThreadSnapshot> record =
                new ProducerRecord<>(topic, String.valueOf(snapshot.threadId()), snapshot);

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                System.err.println("Failed to send snapshot for thread " + snapshot.threadId());
                exception.printStackTrace();
            }
        });
    }

    /**
     * Flush and close the Kafka producer.
     */
    public void close() {
        producer.flush();
        producer.close(Duration.ofSeconds(5));
    }
}
