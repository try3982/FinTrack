package com.bwj.fintrack.transaction.command;


public interface TransactionCommand<TResponse> {


    TResponse execute();

    default TransactionCommand<?> compensate() {
        throw new UnsupportedOperationException("Compensation not supported for this command.");
    }
}