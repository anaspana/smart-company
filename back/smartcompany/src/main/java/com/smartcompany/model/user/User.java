package com.smartcompany.model.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "app_user",
    indexes = {
        @Index(name = "idx_user_type", columnList = "type"),
        @Index(name = "idx_user_type_username", columnList = "type, username")
    }
)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class User {

    @Id
    private String username;
    @Column
    private String password;
    @Column
    private UserType type;
    @Column
    private boolean isActive;
    @Column
    private boolean mustChangePassword;
    @Column
    private String email;
    @Column
    private String image;
    @Column
    private String activationToken;

}