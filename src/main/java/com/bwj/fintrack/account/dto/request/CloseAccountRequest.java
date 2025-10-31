package com.bwj.fintrack.account.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 계좌 해지 요청
 * - 인증 미도입 상태라 userId를 직접 받는다.
 * - 계좌는 accountNumber로 식별한다.
 */
public record CloseAccountRequest(

        @NotNull
        Long userId,

        @NotNull
        @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{7}$", message = "계좌번호 형식이 올바르지 않습니다.")
        String accountNumber
) { }