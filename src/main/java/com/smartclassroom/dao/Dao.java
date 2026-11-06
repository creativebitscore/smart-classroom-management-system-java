package com.smartclassroom.dao;

import java.util.List;
import java.util.Optional;

/**
 * Common contract for data-access classes (abstraction). The service layer depends on this
 * contract, not on how the data is stored. Insert/update differ per entity (some need an open
 * transaction connection), so they are declared in the concrete classes.
 */
public interface Dao<T> {
    List<T> findAll();

    Optional<T> findById(int id);

    void delete(int id);

    /** Default implementation; fine for the small tables used in a classroom system. */
    default int count() {
        return findAll().size();
    }
}
