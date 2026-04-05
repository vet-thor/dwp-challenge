package uk.gov.dwp.uc.pairtest.model;

import java.math.BigDecimal;

public record Account(Long id, String name, BigDecimal balance) implements IdentifiableEntity<Long> {
}
