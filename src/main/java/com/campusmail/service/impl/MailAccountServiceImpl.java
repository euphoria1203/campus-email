package com.campusmail.service.impl;

import com.campusmail.entity.MailAccount;
import com.campusmail.mapper.MailAccountMapper;
import com.campusmail.service.MailAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MailAccountServiceImpl implements MailAccountService {

    private final MailAccountMapper mailAccountMapper;

    public MailAccountServiceImpl(MailAccountMapper mailAccountMapper) {
        this.mailAccountMapper = mailAccountMapper;
    }

    @Override
    @Transactional
    public MailAccount create(MailAccount account) {
        account.setCreatedAt(LocalDateTime.now());
        normalizeDefault(account);
        mailAccountMapper.insert(account);
        ensureSingleDefault(account);
        return account;
    }

    @Override
    @Transactional
    public MailAccount update(MailAccount account) {
        // 确保有 userId（从数据库获取原记录）
        MailAccount existing = mailAccountMapper.findById(account.getId()).orElse(null);
        if (existing == null) {
            throw new RuntimeException("邮箱账号不存在");
        }
        account.setUserId(existing.getUserId());
        normalizeDefault(account);
        mailAccountMapper.update(account);
        ensureSingleDefault(account);
        return account;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MailAccount existing = mailAccountMapper.findById(id).orElse(null);
        if (existing == null) {
            return;
        }
        mailAccountMapper.delete(id);

        int defaultCount = mailAccountMapper.countDefaultByUserId(existing.getUserId());
        if (defaultCount == 0) {
            List<MailAccount> remaining = mailAccountMapper.findByUserId(existing.getUserId());
            if (!remaining.isEmpty()) {
                mailAccountMapper.updateDefaultFlag(remaining.get(0).getId(), true);
            }
        }
    }

    @Override
    public List<MailAccount> listByUser(Long userId) {
        return mailAccountMapper.findByUserId(userId);
    }

    private void normalizeDefault(MailAccount account) {
        if (Boolean.TRUE.equals(account.getIsDefault())) {
            mailAccountMapper.clearDefaultByUserId(account.getUserId());
            account.setIsDefault(true);
        } else {
            int defaultCount = mailAccountMapper.countDefaultByUserId(account.getUserId());
            if (defaultCount == 0) {
                account.setIsDefault(true);
            } else {
                account.setIsDefault(false);
            }
        }
    }

    private void ensureSingleDefault(MailAccount account) {
        int defaultCount = mailAccountMapper.countDefaultByUserId(account.getUserId());
        if (defaultCount == 0 && account.getId() != null) {
            mailAccountMapper.updateDefaultFlag(account.getId(), true);
        }
    }
}
