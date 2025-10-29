package com.bwj.fintrack.account.controller;


import com.bwj.fintrack.account.dto.request.CreateAccountRequest;
import com.bwj.fintrack.account.dto.response.CreateAccountResponse;
import com.bwj.fintrack.account.service.AccountService;
import com.bwj.fintrack.transaction.dto.request.DepositRequest;
import com.bwj.fintrack.transaction.dto.response.DepositResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<CreateAccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        CreateAccountResponse body = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/deposit")
    public ResponseEntity<DepositResponse> deposit(
            @Valid @RequestBody DepositRequest request
    ) {
        DepositResponse body = accountService.deposit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
