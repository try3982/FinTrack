package com.bwj.fintrack.account.service.factory;

import com.bwj.fintrack.account.entity.Account;

// 생성 책임만 갖고 저장은 하지 않는다
public interface AccountFactory<TRequest> {


    Account createAccount(TRequest request);
}
