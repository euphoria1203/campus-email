package com.campusmail.service.impl;

import com.campusmail.entity.Contact;
import com.campusmail.mapper.ContactMapper;
import com.campusmail.service.ContactService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContactServiceImpl implements ContactService {

    private final ContactMapper contactMapper;

    public ContactServiceImpl(ContactMapper contactMapper) {
        this.contactMapper = contactMapper;
    }

    @Override
    @Transactional
    public Contact create(Contact contact) {
        contactMapper.insert(contact);
        return contact;
    }

    @Override
    @Transactional
    public Contact update(Contact contact) {
        contactMapper.update(contact);
        return contact;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        contactMapper.delete(id);
    }

    @Override
    public List<Contact> listByUser(Long userId) {
        return contactMapper.findByUserId(userId);
    }

    @Override
    public List<Contact> search(Long userId, String keyword) {
        return contactMapper.search(userId, keyword);
    }
}
