package com.accountingapp.auth;

import com.accountingapp.auth.dto.AuthResponse;
import com.accountingapp.auth.dto.LoginRequest;
import com.accountingapp.auth.dto.MeResponse;
import com.accountingapp.auth.dto.MembershipDto;
import com.accountingapp.auth.dto.RegisterRequest;
import com.accountingapp.common.exception.BusinessRuleException;
import com.accountingapp.common.exception.ConflictException;
import com.accountingapp.common.exception.ResourceNotFoundException;
import com.accountingapp.user.OrganizationMemberRepository;
import com.accountingapp.user.User;
import com.accountingapp.user.UserMapper;
import com.accountingapp.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("EMAIL_IN_USE", "An account with this email already exists.");
        }
        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .active(true)
                .build();
        User saved = userRepository.save(user);
        return toAuthResponse(issueTokens(saved));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BusinessRuleException("INVALID_CREDENTIALS", "Invalid email or password."));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new BusinessRuleException("ACCOUNT_LOCKED",
                    "This account is temporarily locked due to repeated failed sign-in attempts. Try again later.");
        }
        if (!user.isActive()) {
            throw new BusinessRuleException("ACCOUNT_INACTIVE", "This account is inactive.");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw new BusinessRuleException("INVALID_CREDENTIALS", "Invalid email or password.");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        return toAuthResponse(issueTokens(user));
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .filter(t -> t.getRevokedAt() == null && t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new BusinessRuleException(
                        "INVALID_REFRESH_TOKEN", "Refresh token is invalid or has expired."));
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);
        return toAuthResponse(issueTokens(stored.getUser()));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    @Transactional(readOnly = true)
    public MeResponse me(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return new MeResponse(userMapper.toDto(user), membershipDtos(userId));
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCKOUT_DURATION));
        }
        userRepository.save(user);
    }

    private AuthResult issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String rawRefreshToken = generateOpaqueToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawRefreshToken))
                .expiresAt(Instant.now().plus(jwtProperties.refreshTokenTtlDays(), ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshToken);
        return new AuthResult(accessToken, rawRefreshToken, user);
    }

    private AuthResponse toAuthResponse(AuthResult result) {
        return new AuthResponse(result.accessToken(), result.refreshToken(), jwtService.accessTokenTtlSeconds(),
                userMapper.toDto(result.user()), membershipDtos(result.user().getId()));
    }

    private List<MembershipDto> membershipDtos(UUID userId) {
        return organizationMemberRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(member -> new MembershipDto(
                        member.getOrganization().getId(), member.getOrganization().getName(), member.getRole()))
                .toList();
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private record AuthResult(String accessToken, String refreshToken, User user) {
    }
}
