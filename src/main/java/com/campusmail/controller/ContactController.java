package com.campusmail.controller;

import com.campusmail.entity.Contact;
import com.campusmail.service.ContactService;
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
        return ResponseEntity.ok(contactService.create(contact));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contact> update(@PathVariable("id") Long id, @Valid @RequestBody Contact contact) {
        contact.setId(id);
        return ResponseEntity.ok(contactService.update(contact));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        contactService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Contact>> list(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(contactService.listByUser(userId));
    }

    /**
     * 搜索联系人
     */
    @GetMapping("/search")
        public ResponseEntity<List<Contact>> search(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {
        return ResponseEntity.ok(contactService.search(userId, keyword));
    }
}
