package com.bwj.fintrack.autotransfer.controller;


import com.bwj.fintrack.autotransfer.dto.request.CreateAutoTransferRequest;
import com.bwj.fintrack.autotransfer.dto.response.CreateAutoTransferResponse;
import com.bwj.fintrack.autotransfer.service.AutoTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/autotransfers")
@RequiredArgsConstructor
public class AutoTransferController {

    private final AutoTransferService autoTransferService;


    @PostMapping
    public ResponseEntity<CreateAutoTransferResponse> create(
            @Valid @RequestBody CreateAutoTransferRequest request
    ) {
        CreateAutoTransferResponse body = autoTransferService.createAutoTransfer(request);
        return ResponseEntity.ok(body);
    }
}
