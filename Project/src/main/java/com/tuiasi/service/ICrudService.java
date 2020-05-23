package com.tuiasi.service;

public interface ICrudService<T, I> {
    T add(T object);

    T get(I id);

    T update(T object, I id);

    void delete(I id);
}