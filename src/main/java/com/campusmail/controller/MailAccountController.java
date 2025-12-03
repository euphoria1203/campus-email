package com.campusmail.controller;

import com.campusmail.entity.MailAccount;
import com.campusmail.service.MailAccountService;
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
        return ResponseEntity.ok(mailAccountService.create(account));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MailAccount> update(@PathVariable("id") Long id, @Valid @RequestBody MailAccount account) {
        account.setId(id);
        return ResponseEntity.ok(mailAccountService.update(account));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        mailAccountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MailAccount>> listByUser(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(mailAccountService.listByUser(userId));
    }
}
