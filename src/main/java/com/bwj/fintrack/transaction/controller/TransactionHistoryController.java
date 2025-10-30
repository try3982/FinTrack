package com.bwj.fintrack.transaction.controller;

import com.bwj.fintrack.transaction.dto.request.TransactionHistoryRequest;
import com.bwj.fintrack.transaction.dto.response.TransactionHistoryPageResponse;
import com.bwj.fintrack.transaction.service.TransactionHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
public class TransactionHistoryController {

    private final TransactionHistoryService transactionHistoryService;

    @PostMapping("/history")
    public ResponseEntity<TransactionHistoryPageResponse> getHistory(
            @Valid @RequestBody TransactionHistoryRequest request
    ) {
        TransactionHistoryPageResponse body = transactionHistoryService.getTransactionHistory(request);
        return ResponseEntity.ok(body);
    }
}
