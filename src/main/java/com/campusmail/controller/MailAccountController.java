package com.campusmail.controller;

import com.campusmail.entity.MailAccount;
import com.campusmail.service.MailAccountService;
import com.campusmail.utils.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mail-accounts")
public class MailAccountController {

    private final MailAccountService mailAccountService;

    public MailAccountController(MailAccountService mailAccountService) {
        this.mailAccountService = mailAccountService;
    }

    @PostMapping
    public ResponseEntity<MailAccount> create(@Valid @RequestBody MailAccount account) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(mailAccountService.create(userId, account));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MailAccount> update(@PathVariable("id") Long id, @Valid @RequestBody MailAccount account) {
        Long userId = SecurityUtils.getCurrentUserId();
        account.setId(id);
        return ResponseEntity.ok(mailAccountService.update(userId, account));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        mailAccountService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MailAccount>> listByUser(@PathVariable("userId") Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).build();
        }
        if (!currentUserId.equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(mailAccountService.listByUser(currentUserId));
    }
}
