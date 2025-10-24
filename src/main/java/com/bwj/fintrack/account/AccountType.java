package com.bwj.fintrack.account;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountType {

    DEPOSIT("예금", 1000),
    SAVINGS("적금", 10000),
    // LOANS("대출", 0L, 0.3),
    ;

    private String display;
    private final int minimumInitial;
}