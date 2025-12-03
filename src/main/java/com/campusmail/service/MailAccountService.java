package com.campusmail.service;

import com.campusmail.entity.MailAccount;

import java.util.List;

public interface MailAccountService {

    MailAccount create(MailAccount account);

    MailAccount update(MailAccount account);

    void delete(Long id);

    List<MailAccount> listByUser(Long userId);
}
