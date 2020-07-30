package me.dandrew.ultimateaurapro.util;

public class ObjectContainer<T extends Object> {

    private T object = null;

    public void setObject(T object) {
        this.object = object;
    }

    public T getObject() {
        return object;
    }

}
