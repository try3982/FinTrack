package com.bwj.fintrack.account.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountType {

    DEPOSIT("예금", 0),
    SAVINGS("적금", 10000),
    // LOANS("대출", 0L, 0.3), //TODO : 추후 확장 예정
    ;

    private String display;
    private final int minimumInitial;
}