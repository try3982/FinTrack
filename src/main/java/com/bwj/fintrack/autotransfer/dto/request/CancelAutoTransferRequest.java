package com.bwj.fintrack.autotransfer.dto.request;

import jakarta.validation.constraints.NotNull;

public record CancelAutoTransferRequest(

        @NotNull
        Long userId
) { }