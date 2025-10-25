package com.bwj.fintrack.common.money;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money {

    public static final int SCALE = 2;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal value;

    private Money(BigDecimal v) {
        this.value = v.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal v) {
        Objects.requireNonNull(v, "Money value must not be null");
        return new Money(v);
    }

    public Money plus(Money other) {
        return of(this.value.add(other.value));
    }

    public Money minus(Money other) {
        return of(this.value.subtract(other.value));
    }

    public boolean isPositive() {
        return value.signum() > 0;
    }

    public boolean isNegative() {
        return value.signum() < 0;
    }

    public boolean lt(Money other) {
        return this.value.compareTo(other.value) < 0;
    }

    public boolean lte(Money other) {
        return this.value.compareTo(other.value) <= 0;
    }

    public boolean gt(Money other) {
        return this.value.compareTo(other.value) > 0;
    }

    public BigDecimal asBigDecimal() { return value; }

    @Override public String toString() { return value.toPlainString(); }
}