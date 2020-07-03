package com.tuiasi.crawler_module.repository;

import java.util.Optional;

public interface ICrudRepository<T, I> {
    T add(T object);

    Optional<T> get(I id);

    Optional<T> update(T object, I id);

    void delete(I id);
}
