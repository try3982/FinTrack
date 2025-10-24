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




    ;

    private final String description;
    private final HttpStatus status;

}
