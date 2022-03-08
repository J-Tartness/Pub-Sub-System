package main;

import java.io.Serializable;
import java.util.List;

public class TopicSub implements Serializable {
    boolean isSubscribe;
    String subscriberName;
    List<String> topicSelected;

    public TopicSub(boolean isSubscribe, String subscriberName, List<String> topicSelected) {
        this.isSubscribe = isSubscribe;
        this.subscriberName = subscriberName;
        this.topicSelected = topicSelected;
    }

    public boolean isSubscribe() {
        return isSubscribe;
    }

    public void setSubscribe(boolean subscribe) {
        isSubscribe = subscribe;
    }

    public String getSubscriberName() {
        return subscriberName;
    }

    public void setSubscriberName(String subscriberName) {
        this.subscriberName = subscriberName;
    }

    public List<String> getTopicSelected() {
        return topicSelected;
    }

    public void setTopicSelected(List<String> topicSelected) {
        this.topicSelected = topicSelected;
    }

    @Override
    public String toString() {
        return "TopicSub{" +
                "isSubscribe=" + isSubscribe +
                ", subscriberName='" + subscriberName + '\'' +
                ", topicSelected=" + topicSelected +
                '}';
    }
}
