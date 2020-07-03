package com.tuiasi.crawler_module.controller;

public interface ICrudController<T, I> {
    T add(T object);

    T get(I id);

    T update(T object, I id);

    void delete(I id);
}
