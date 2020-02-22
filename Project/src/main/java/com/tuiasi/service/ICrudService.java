package com.tuiasi.service;

import org.springframework.stereotype.Service;

public interface ICrudService<T> {
    T add(T object);

    T get(int id);

    T update(T object, int id);

    void delete(int id);
}