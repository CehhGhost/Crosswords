package com.backend.crosswords.admin.services;

import com.backend.crosswords.admin.models.Role;
import com.backend.crosswords.admin.repositories.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class RoleService {
    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Role getRoleWithAuthorities(Long id) {
        return roleRepository.findByIdWithAuthorities(id).orElseThrow(() -> new NoSuchElementException("There is no roles with such id!"));
    }
}
