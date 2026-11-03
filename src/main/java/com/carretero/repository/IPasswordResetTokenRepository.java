package com.carretero.repository;

import com.carretero.model.PasswordResetToken;

import java.util.Optional;

public interface IPasswordResetTokenRepository extends IGenericRepository<PasswordResetToken, Integer> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
