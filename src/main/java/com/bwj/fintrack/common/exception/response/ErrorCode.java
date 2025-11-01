package com.bwj.fintrack.common.exception.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT_DATA("잘못된 입력 데이터입니다.", HttpStatus.BAD_REQUEST),
    DATA_INTEGRITY_VIOLATION("데이터 무결성에 위배되었습니다.", HttpStatus.CONFLICT),
    UNSUPPORTED_MEDIA_TYPE_ERROR("지원하지 않는 미디어 타입입니다.", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    RESOURCE_NOT_FOUND("리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR("서버 오류입니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    METHOD_NOT_ALLOWED("지원하지 않는 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
    FORBIDDEN_ACCESS("접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    USER_NOT_FOUND("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_AMOUNT("유효하지 않은 금액 형식입니다.", HttpStatus.BAD_REQUEST),


    INITIAL_DEPOSIT_REQUIRED("초기 예치금이 필요합니다.", HttpStatus.BAD_REQUEST),
    INITIAL_DEPOSIT_BELOW_MIN("초기 예치금은 최소 10000원 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    DUPLICATE_ACCOUNT_NUMBER("중복된 계좌번호가 존재합니다.", HttpStatus.CONFLICT),
    INVALID_ACCOUNT_NUMBER_FORMAT("유효하지 않은 계좌번호 형식입니다. 형식: ###-####-#######", HttpStatus.BAD_REQUEST),
    AMOUNT_MUST_BE_POSITIVE("금액은 0보다 커야 합니다.", HttpStatus.BAD_REQUEST),
    ACCOUNT_NUMBER_GENERATION_FAILED("계좌번호 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ACCOUNT_NOT_ACTIVE("활성화된 계좌가 아닙니다.", HttpStatus.BAD_REQUEST),
    MIN_BALANCE_VIOLATION("최소 유지 잔액을 충족하지 못합니다.", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BALANCE("잔액이 부족합니다.", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_FOUND("요청하신 계좌를 찾을 수 없습니다.",HttpStatus.NOT_FOUND),


    FORBIDDEN_ACCOUNT_ACCESS("해당 계좌에 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    AUTO_TRANSFER_NOT_FOUND("자동이체 정보를를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ACCOUNT_RESTORE_WINDOW_EXPIRED("복구 가능 기간이 지났습니다.",HttpStatus.BAD_REQUEST),
    ACCOUNT_ALREADY_CLOSED  ("이미 닫힌 계좌입니다.",HttpStatus.CONFLICT),
    ACCOUNT_BALANCE_NOT_ZERO("계좌 잔액이 0이어야 해지가 가능합니다.", HttpStatus.BAD_REQUEST),
    ACCOUNT_CLOSE_FORBIDDEN("본인 소유의 계좌만 해지할 수 있습니다", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_CLOSED("해지된 계좌가 아닙니다",HttpStatus.BAD_REQUEST),
    ACCOUNT_RESTORE_EXPIRED("계좌 복원 가능 기간이 만료되었습니다",HttpStatus.BAD_REQUEST),
    ACCOUNT_RESTORE_FORBIDDEN("본인 소유의 계좌만 활성화할 수 있습니다",HttpStatus.FORBIDDEN),
    INVALID_INITIAL_DEPOSIT( "초기 입금액은 0원 이상이어야 합니다",HttpStatus.BAD_REQUEST),
    AMOUNT_EXCEEDS_LIMIT("단건거래 한도를 초과했습니다.", HttpStatus.BAD_REQUEST),
    INVALID_INITIAL_DEPOSIT_FOR_SAVINGS( "적금 계좌의 초기 입금액은 최소 10,000원 이상이어야 합니다",HttpStatus.BAD_REQUEST),
    INVALID_MONTHLY_AMOUNT("월 납입액은 최소 10,000원 이상이어야 합니다",HttpStatus.BAD_REQUEST),
    AUTO_TRANSFER_ACCESS_FORBIDDEN( "본인의 자동이체 설정만 사용할 수 있습니다",HttpStatus.FORBIDDEN),
    AUTO_TRANSFER_REQUIRED( "자동이체 설정이 필요합니다.",HttpStatus.BAD_REQUEST),
    DAILY_LIMIT_EXCEEDED( "일일 출금/이체 한도를 초과했습니다.",HttpStatus.BAD_REQUEST),
    INVALID_CURSOR("잘못된 커서 값입니다. 커서 형식이 올바르지 않습니다.",HttpStatus.BAD_REQUEST),



    ;


    private final String description;
    private final HttpStatus status;

}
