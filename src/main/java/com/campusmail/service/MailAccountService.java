package com.campusmail.service;

import com.campusmail.entity.MailAccount;

import java.util.List;

public interface MailAccountService {

    MailAccount create(Long userId, MailAccount account);

    MailAccount update(Long userId, MailAccount account);

    void delete(Long userId, Long id);

    List<MailAccount> listByUser(Long userId);
}
