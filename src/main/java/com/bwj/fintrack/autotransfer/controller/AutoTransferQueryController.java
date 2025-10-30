package com.bwj.fintrack.autotransfer.controller;

import com.bwj.fintrack.autotransfer.dto.request.ListAutoTransferRequest;
import com.bwj.fintrack.autotransfer.dto.response.ListAutoTransferResponse;
import com.bwj.fintrack.autotransfer.service.AutoTransferQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/autotransfers")
@RequiredArgsConstructor
public class AutoTransferQueryController {

    private final AutoTransferQueryService autoTransferQueryService;

    @PostMapping("/list")
    public ResponseEntity<ListAutoTransferResponse> listUserAutoTransfers(
            @Valid @RequestBody ListAutoTransferRequest request
    ) {
        ListAutoTransferResponse body = autoTransferQueryService.listUserAutoTransfers(request);
        return ResponseEntity.ok(body);
    }
}