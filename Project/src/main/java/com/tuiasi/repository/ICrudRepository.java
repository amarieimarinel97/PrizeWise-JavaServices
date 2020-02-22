package com.tuiasi.repository;

import java.util.Optional;

public interface ICrudRepository<T> {
    T add(T object);

    Optional<T> get(int id);

    Optional<T> update(T object, int id);

    void delete(int id);
}
