package com.xadale.playerlogger.core;

import java.util.Optional;
import java.util.stream.Stream;

public interface DataRepository<T extends AbstractData<T, ID>, ID> {

  Optional<T> get(ID identifier);

  void add(T data);

  void remove(T data);

  void update(T data);

  Stream<T> getAll();
}
