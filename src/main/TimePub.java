package main;

import java.io.Serializable;

public class TimePub implements Serializable {
    String topic;
    int count;
    Long timestamp;

    public TimePub(String topic, int count, Long timestamp) {
        this.topic = topic;
        this.count = count;
        this.timestamp = timestamp;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "TimPub{" +
                "topic='" + topic + '\'' +
                ", count=" + count +
                ", timestamp=" + timestamp +
                '}';
    }
}
