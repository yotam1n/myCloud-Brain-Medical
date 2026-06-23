package com.cloudbrain.application.auth;

import com.cloudbrain.common.exception.ApiException;
import com.cloudbrain.domain.auth.AccountProfile;
import com.cloudbrain.dto.auth.LoginRequest;
import com.cloudbrain.dto.auth.LoginResponse;
import com.cloudbrain.dto.auth.RegisterRequest;
import com.cloudbrain.repository.IdentityRepository;
import com.cloudbrain.security.ActorContext;
import com.cloudbrain.security.ActorRole;
import com.cloudbrain.security.JwtTokenUtil;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final IdentityRepository identityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthService(IdentityRepository identityRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenUtil jwtTokenUtil) {
        this.identityRepository = identityRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public void patientRegister(RegisterRequest request) {
        if (request.age() != null && request.age() < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "age must be positive");
        }
        identityRepository.registerPatient(request, passwordEncoder.encode(request.password()));
    }

    public LoginResponse patientLogin(LoginRequest request) {
        return login(request, ActorRole.PATIENT);
    }

    public LoginResponse doctorLogin(LoginRequest request) {
        return login(request, ActorRole.DOCTOR);
    }

    public LoginResponse adminLogin(LoginRequest request) {
        return login(request, ActorRole.ADMIN);
    }

    private LoginResponse login(LoginRequest request, ActorRole expectedRole) {
        AccountProfile account = identityRepository.findByUsername(request.username())
                .filter(profile -> profile.role() == expectedRole)
                .filter(profile -> passwordEncoder.matches(request.password(), profile.passwordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED.value(), "invalid username or password"));

        ActorContext actorContext = account.toActorContext();
        String token = jwtTokenUtil.generateToken(actorContext);
        long expiresAt = Instant.now().plus(jwtTokenUtil.getTokenTtl()).toEpochMilli();
        return new LoginResponse(
                token,
                "Bearer",
                account.userId(),
                account.role().name().toLowerCase(Locale.ROOT),
                account.patientId(),
                account.doctorId(),
                account.username(),
                account.displayName(),
                expiresAt
        );
    }
}
