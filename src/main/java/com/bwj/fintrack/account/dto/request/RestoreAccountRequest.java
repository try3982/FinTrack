package com.bwj.fintrack.account.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 계좌 활성화 요청
 * - 인증 미도입 상태라 userId를 직접 받는다.
 * - 해지된 계좌를 restoreUntil 기간 내에 다시 활성화한다.
 */
public record RestoreAccountRequest(

        @NotNull
        Long userId,

        @NotNull
        @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{7}$", message = "계좌번호 형식이 올바르지 않습니다.")
        String accountNumber
) { }