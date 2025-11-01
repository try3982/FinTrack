package com.bwj.fintrack.transaction.command;


import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionExecutor {

    @Transactional
    public <T> T execute(TransactionCommand<T> command) {
        return command.execute();
    }
}
