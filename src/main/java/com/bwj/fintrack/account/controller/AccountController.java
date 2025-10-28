package com.bwj.fintrack.account.controller;


import com.bwj.fintrack.account.dto.request.CreateAccountRequest;
import com.bwj.fintrack.account.dto.response.CreateAccountResponse;
import com.bwj.fintrack.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
