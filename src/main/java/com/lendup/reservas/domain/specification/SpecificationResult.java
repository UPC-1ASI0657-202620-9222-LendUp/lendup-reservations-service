package com.lendup.reservas.domain.specification;

import java.util.LinkedHashSet;
import java.util.Set;

public record SpecificationResult(boolean satisfied, Set<String> violations, Set<String> pendingChecks) {
  public SpecificationResult {
    violations = Set.copyOf(violations);
    pendingChecks = Set.copyOf(pendingChecks);
  }

  public static SpecificationResult passed() {
    return new SpecificationResult(true, Set.of(), Set.of());
  }

  public static SpecificationResult violated(String code) {
    return new SpecificationResult(false, Set.of(code), Set.of());
  }

  public static SpecificationResult pending(String code) {
    return new SpecificationResult(false, Set.of(), Set.of(code));
  }

  public SpecificationResult combine(SpecificationResult other) {
    LinkedHashSet<String> combinedViolations = new LinkedHashSet<>(violations);
    combinedViolations.addAll(other.violations);
    LinkedHashSet<String> combinedPending = new LinkedHashSet<>(pendingChecks);
    combinedPending.addAll(other.pendingChecks);
    return new SpecificationResult(satisfied && other.satisfied, combinedViolations, combinedPending);
  }
}
