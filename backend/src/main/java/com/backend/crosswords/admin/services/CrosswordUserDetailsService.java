package com.backend.crosswords.admin.services;

import com.backend.crosswords.admin.models.CrosswordUserDetails;
import com.backend.crosswords.admin.models.Role;
import com.backend.crosswords.admin.models.User;
import com.backend.crosswords.admin.repositories.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CrosswordUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RoleService roleService;

    public CrosswordUserDetailsService(UserRepository userRepository, RoleService roleService) {
        this.userRepository = userRepository;
        this.roleService = roleService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByUsernameOrEmail(username, username);
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }
        return new CrosswordUserDetails(this, user.get());
    }

    public List<SimpleGrantedAuthority> getAuthorities(User user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() == null) {
            return Collections.singletonList(new SimpleGrantedAuthority("NO_AUTHORITIES"));
        }
        Role roleWithAuthorities;
        try {
            roleWithAuthorities = roleService.getRoleWithAuthorities(user.getRole().getId());
        } catch (NoSuchElementException e) {
            return Collections.singletonList(new SimpleGrantedAuthority("NO_AUTHORITIES"));
        }
        for (var authority : roleWithAuthorities.getAuthorities()) {
            authorities.add(new SimpleGrantedAuthority(authority.getName()));
        }
        return authorities;
    }
}
