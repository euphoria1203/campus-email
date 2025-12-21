package com.campusmail.controller;

import com.campusmail.entity.Contact;
import com.campusmail.service.ContactService;
import com.campusmail.utils.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<Contact> create(@Valid @RequestBody Contact contact) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(contactService.create(userId, contact));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contact> update(@PathVariable("id") Long id, @Valid @RequestBody Contact contact) {
        contact.setId(id);
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(contactService.update(userId, contact));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        contactService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Contact>> list(@PathVariable("userId") Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).build();
        }
        if (!currentUserId.equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(contactService.listByUser(currentUserId));
    }

    /**
     * 搜索联系人
     */
    @GetMapping("/search")
        public ResponseEntity<List<Contact>> search(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).build();
        }
        if (!currentUserId.equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(contactService.search(currentUserId, keyword));
    }
}
