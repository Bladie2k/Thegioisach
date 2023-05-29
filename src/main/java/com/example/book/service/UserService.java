package com.example.book.service;

import com.example.book.domain.UserRegister;
import com.example.book.entity.Role;
import com.example.book.entity.User;
import com.example.book.repository.RoleRepository;
import com.example.book.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final ModelMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public Set<SimpleGrantedAuthority> getRole(User user) {
        Role role = user.getRole();
        return Collections.singleton(new SimpleGrantedAuthority(role.getName()));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = Optional.of(userRepository.findUserByEmail(username)
                .orElseThrow(()-> new UsernameNotFoundException("Username " + username + " not found!")));
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(user.get().getEmail(),
                user.get().getPassword(), getRole(user.get()));
        return userDetails;
    }

    public void addUserAttributesToModel(Model model){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findUserByEmail(auth.getName()).get();
        String role = user.getRole().getName();
        model.addAttribute("user_detail", user);
        model.addAttribute("role", role);
    }

    public void saveRegisteredUser(UserRegister userRegister){
        User user = mapper.map(userRegister, User.class);
        user.setId(UUID.randomUUID());
        user.setPassword(passwordEncoder.encode(userRegister.getPassword()));
        user.setRole(roleRepository.findRoleByName("user"));
        user.setLast_updated(LocalDateTime.now());
        userRepository.save(user);
    }
}
