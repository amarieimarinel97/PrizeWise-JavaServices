package com.tuiasi.controller;

public interface ICrudController<T> {
    T add(T object);

    T get(int id);

    T update(T object, int id);

    void delete(int id);
}
