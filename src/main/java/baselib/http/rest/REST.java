package baselib.http.rest;

import java.util.Optional;

public interface REST<E, K> {
  Optional<E> get(K k);
  void delete(K k);
  E put(K k, E e);
  E patch(K k, E e);
  E post(K k, E e);
}
