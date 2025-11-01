package com.bwj.fintrack.account.controller;


import com.bwj.fintrack.account.dto.request.*;
import com.bwj.fintrack.account.dto.response.*;
import com.bwj.fintrack.account.service.AccountService;
import com.bwj.fintrack.transaction.dto.request.DepositRequest;
import com.bwj.fintrack.transaction.dto.request.TransferRequest;
import com.bwj.fintrack.transaction.dto.request.WithdrawRequest;
import com.bwj.fintrack.transaction.dto.response.DepositResponse;
import com.bwj.fintrack.transaction.dto.response.TransferResponse;
import com.bwj.fintrack.transaction.dto.response.WithdrawResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/accounts")
public class AccountController {

    private final AccountService accountService;


    @PostMapping("/deposit")
    public ResponseEntity<CreateDepositAccountResponse> createDepositAccount(
            @Valid @RequestBody CreateDepositAccountRequest request
    ) {
        CreateDepositAccountResponse body = accountService.createDepositAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/savings")
    public ResponseEntity<CreateSavingsAccountResponse> createSavingsAccount(
            @Valid @RequestBody CreateSavingsAccountRequest request
    ) {
        CreateSavingsAccountResponse body = accountService.createSavingsAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountDetailResponse> getAccount(
            @PathVariable
            @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{7}$", message = "계좌번호 형식이 올바르지 않습니다.")
            String accountNumber
    ) {
        AccountDetailResponse body = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/close")
    public ResponseEntity<CloseAccountResponse> closeAccount(
            @Valid @RequestBody CloseAccountRequest request
    ) {
        CloseAccountResponse body = accountService.closeAccount(request);
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }

    @PostMapping("/restore")
    public ResponseEntity<RestoreAccountResponse> restoreAccount(
            @Valid @RequestBody RestoreAccountRequest request
    ) {
        RestoreAccountResponse body = accountService.restoreAccount(request);
        return ResponseEntity.ok(body);
    }
}
