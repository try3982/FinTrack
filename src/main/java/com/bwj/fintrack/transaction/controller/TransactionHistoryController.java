package com.bwj.fintrack.transaction.controller;

import com.bwj.fintrack.account.service.AccountService;
import com.bwj.fintrack.transaction.dto.request.DepositRequest;
import com.bwj.fintrack.transaction.dto.request.TransactionHistoryRequest;
import com.bwj.fintrack.transaction.dto.request.TransferRequest;
import com.bwj.fintrack.transaction.dto.request.WithdrawRequest;
import com.bwj.fintrack.transaction.dto.response.DepositResponse;
import com.bwj.fintrack.transaction.dto.response.TransactionHistoryPageResponse;
import com.bwj.fintrack.transaction.dto.response.TransferResponse;
import com.bwj.fintrack.transaction.dto.response.WithdrawResponse;
import com.bwj.fintrack.transaction.service.TransactionHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
public class TransactionHistoryController {

    private final TransactionHistoryService transactionHistoryService;
    private final AccountService accountService;


    @PostMapping("/deposit")
    public ResponseEntity<DepositResponse> deposit(
            @Valid @RequestBody DepositRequest request
    ) {
        DepositResponse body = accountService.deposit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WithdrawResponse> withdraw(@Valid @RequestBody WithdrawRequest request) {
        WithdrawResponse body = accountService.withdraw(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/history")
    public ResponseEntity<TransactionHistoryPageResponse> getHistory(
            @Valid @RequestBody TransactionHistoryRequest request
    ) {
        TransactionHistoryPageResponse body = transactionHistoryService.getTransactionHistory(request);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request
    ) {
        TransferResponse body = accountService.transfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }



}
