package com.Ledjr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Ledjr.entity.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** links that haven't been used yet, so asking for a new one can kill the old ones */
    List<PasswordResetToken> findByUserIdAndUsedAtIsNull(Long userId);

}
