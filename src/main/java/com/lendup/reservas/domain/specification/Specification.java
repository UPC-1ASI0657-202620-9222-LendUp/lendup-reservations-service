package com.lendup.reservas.domain.specification;

import java.util.Objects;

@FunctionalInterface
public interface Specification<T> {
  SpecificationResult evaluate(T candidate);

  default Specification<T> and(Specification<T> other) {
    Objects.requireNonNull(other, "La especificación compuesta es obligatoria");
    return candidate -> evaluate(candidate).combine(other.evaluate(candidate));
  }
}
