package com.quicktable.userservice.repository;

import com.quicktable.userservice.entity.PasswordResetToken;
import com.quicktable.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteAllByUser(User user);
}
