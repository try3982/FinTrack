package com.bwj.fintrack.autotransfer.controller;

import com.bwj.fintrack.autotransfer.dto.request.CancelAutoTransferRequest;
import com.bwj.fintrack.autotransfer.dto.request.UpdateAutoTransferBody;
import com.bwj.fintrack.autotransfer.dto.request.UpdateAutoTransferRequest;
import com.bwj.fintrack.autotransfer.dto.response.AutoTransferItemResponse;
import com.bwj.fintrack.autotransfer.service.AutoTransferCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/v1/autotransfers")
@RequiredArgsConstructor
public class AutoTransferCommandController {

    private final AutoTransferCommandService autoTransferCommandService;

    @PatchMapping("/{autoTransferId}")
    public ResponseEntity<AutoTransferItemResponse> updateAutoTransfer(
            @PathVariable Long autoTransferId,
            @Valid @RequestBody UpdateAutoTransferBody body
    ) {
        AutoTransferItemResponse response =
                autoTransferCommandService.updateAutoTransfer(autoTransferId, body);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{autoTransferId}/cancel")
    public ResponseEntity<AutoTransferItemResponse> cancelAutoTransfer(
            @PathVariable Long autoTransferId,
            @Valid @RequestBody CancelAutoTransferRequest request
    ) {
        AutoTransferItemResponse body =
                autoTransferCommandService.cancelAutoTransfer(autoTransferId, request);

        return ResponseEntity.ok(body);
    }
}
