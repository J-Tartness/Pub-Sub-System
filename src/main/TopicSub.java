package main;

import java.io.Serializable;
import java.util.List;

public class TopicSub implements Serializable {
    String subscriberName;
    List<String> topicSelected;

    public TopicSub(String subscriberName, List<String> topicSelected) {
        this.topicSelected = topicSelected;
        this.subscriberName = subscriberName;
    }

    public List<String> getTopicSelected() {
        return topicSelected;
    }

    public void setTopicSelected(List<String> topicSelected) {
        this.topicSelected = topicSelected;
    }

    public String getSubscriberName() {
        return subscriberName;
    }

    public void setSubscriberName(String subscriberName) {
        this.subscriberName = subscriberName;
    }

    @Override
    public String toString() {
        return "TopicSub{" +
                "subscriberName='" + subscriberName + '\'' +
                ", topicSelected='" + topicSelected + '\'' +
                '}';
    }
}
