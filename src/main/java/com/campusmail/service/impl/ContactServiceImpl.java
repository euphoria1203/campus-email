package com.campusmail.service.impl;

import com.campusmail.entity.Contact;
import com.campusmail.mapper.ContactMapper;
import com.campusmail.service.ContactService;
import com.campusmail.utils.SnowflakeIdGenerator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContactServiceImpl implements ContactService {

    private final ContactMapper contactMapper;
    private final SnowflakeIdGenerator idGen;

    public ContactServiceImpl(ContactMapper contactMapper, SnowflakeIdGenerator idGen) {
        this.contactMapper = contactMapper;
        this.idGen = idGen;
    }

    @Override
    @Transactional
    public Contact create(Long userId, Contact contact) {
        contact.setUserId(userId);
        contact.setId(idGen.nextId());
        contactMapper.insert(contact);
        return contact;
    }

    @Override
    @Transactional
    public Contact update(Long userId, Contact contact) {
        Contact existing = contactMapper.findById(contact.getId())
            .orElseThrow(() -> new RuntimeException("联系人不存在"));
        if (!existing.getUserId().equals(userId)) {
            throw new AccessDeniedException("无权修改该联系人");
        }
        contact.setUserId(existing.getUserId());
        contactMapper.update(contact);
        return contact;
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        contactMapper.findById(id).ifPresent(contact -> {
            if (!contact.getUserId().equals(userId)) {
                throw new AccessDeniedException("无权删除该联系人");
            }
            contactMapper.delete(id);
        });
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
