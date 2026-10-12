package com.smartcompany.repository.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.smartcompany.model.user.User;

public interface UserRepository extends JpaRepository<User, String>,
                                        JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    void deleteByUsername(String username);
}