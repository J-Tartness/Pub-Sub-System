package main;

import java.io.Serializable;

public class Heart implements Serializable {
    String isAlive;

    public Heart(String isAlive) {
        this.isAlive = isAlive;
    }

    public String getIsAlive() {
        return isAlive;
    }

    public void setIsAlive(String isAlive) {
        this.isAlive = isAlive;
    }

    @Override
    public String toString() {
        return "Heart{" +
                "isAlive='" + isAlive + '\'' +
                '}';
    }
}
