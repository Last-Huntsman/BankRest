package org.zuzukov.bank_rest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zuzukov.bank_rest.entity.Role;
import org.zuzukov.bank_rest.entity.User;
import org.zuzukov.bank_rest.repository.UserRepository;
import org.zuzukov.bank_rest.exception.NotFoundException;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminService {
    private final UserRepository userRepository;


    @Transactional(readOnly = true)
    public Page<User> list(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public void setRole(UUID userId, Role role, boolean enabled) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Set<Role> roles = new HashSet<>(user.getRoles());
        if (enabled) roles.add(role); else roles.remove(role);
        user.setRoles(roles);
    }

    @Transactional
    public void delete(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }
        userRepository.deleteById(userId);
    }
}


