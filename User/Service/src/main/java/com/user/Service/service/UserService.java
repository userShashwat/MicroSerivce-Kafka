package com.user.Service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user.Service.DTO.AuthResponse;
import com.user.Service.DTO.LoginRequest;
import com.user.Service.DTO.RegisterRequest;
import com.user.Service.Enitity.Role;
import com.user.Service.Enitity.User;
import com.user.Service.Repository.UserRepository;
import com.user.Service.Security.JwtAuthFilter;
import com.user.Service.Security.JwtUtils;
import com.user.Service.event.UserRegisteredEvent;
import com.user.Service.kafka.UserEventProducer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    private final UserEventProducer userEventProducer;
    private final ModelMapper modelMapper;
    @Transactional
    public AuthResponse register(RegisterRequest request){
        if(userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("email already exist"+request.getEmail());
        }
        User user=User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        User saved=userRepository.save(user);
        log.info("User registered: {}", saved.getEmail());
        userEventProducer.publishUserRegistered(
                new UserRegisteredEvent(saved.getId(), saved.getEmail(), saved.getName())
        );
        String token =jwtUtils.generateToken(saved.getEmail(),saved.getRole().name());
        AuthResponse response = modelMapper.map(saved, AuthResponse.class);
        response.setToken(token);
        return response;
    }
    public AuthResponse login(LoginRequest request){
          User user=userRepository.findByEmail(request.getEmail()).orElseThrow(
                  ()->new RuntimeException("NO USER FOUND"+request.getEmail())
          );
          if(!passwordEncoder.matches(request.getPassword(),user.getPassword())){
              throw new RuntimeException("Invalid Password");
          }
          log.info("User logged in: {}", user.getEmail());
          String token=jwtUtils.generateToken(user.getEmail(),user.getRole().name());
          return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }
}
