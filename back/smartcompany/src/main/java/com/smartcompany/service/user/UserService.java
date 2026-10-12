package com.smartcompany.service.user;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smartcompany.dto.cache.CachedPage;
import com.smartcompany.dto.user.ChangePasswordDto;
import com.smartcompany.dto.user.CredentialsDto;
import com.smartcompany.dto.user.SignUpDto;
import com.smartcompany.dto.user.UserDto;
import com.smartcompany.exception.AppException;
import com.smartcompany.model.user.User;
import com.smartcompany.model.user.UserType;
import com.smartcompany.repository.user.UserRepository;
import com.smartcompany.specification.UserSpecification;

@Service
public class UserService {

    @Autowired
    private EmailService emailService;
    @Autowired
    private ActivationTokenService tokenService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CacheManager cacheManager;

    public List<UserDto> getAll() {
        return toUserDtos(userRepository.findAll());
    }

    public Page<UserDto> getManagers(String search, Pageable pageable) {
        Cache cache = cacheManager.getCache("managers");
        String key = search + "-" + pageable.getPageNumber() + "-" + pageable.getPageSize();

        if (cache != null) {
            CachedPage<UserDto> cached = cache.get(key, CachedPage.class);
            if (cached != null) {
                return cached.toPage(pageable);
            }
        }

        Page<UserDto> result = userRepository
                .findAll(UserSpecification.searchManagers(search), pageable)
                .map(this::toUserDto);

        if (cache != null) {
            cache.put(key, CachedPage.from(result));
        }

        return result;
    }

    public UserDto login(CredentialsDto credentialsDto) {
        User user = userRepository.findByUsername(credentialsDto.username())
                .orElseThrow(() -> new AppException("Unknown user", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(CharBuffer.wrap(credentialsDto.password()), user.getPassword())) {
            throw new AppException("Incorrect password", HttpStatus.BAD_REQUEST);
        }

        // isActive=false za menadžere/superadmin znači blokiran
        // isActive=false za kupce znači neaktiviran nalog
        if (!user.isActive() && user.getType() != UserType.Customer) {
            throw new AppException("Account is blocked", HttpStatus.FORBIDDEN);
        }

        if (!user.isActive() && user.getType() == UserType.Customer) {
            throw new AppException("Account is not activated. Please check your email.", HttpStatus.FORBIDDEN);
        }

        return toUserDto(user);
    }

    public UserDto registerCustomer(SignUpDto userDto) {
        if (userRepository.findByUsername(userDto.username()).isPresent()) {
            throw new AppException("Username already taken", HttpStatus.BAD_REQUEST);
        }

        User user = signUpToUser(userDto);
        user.setType(UserType.Customer);
        user.setActive(false);
        user.setMustChangePassword(false);
        user.setPassword(passwordEncoder.encode(CharBuffer.wrap(userDto.password())));

        String token = tokenService.generateActivationToken();
        user.setActivationToken(token);
        String activationLink = "http://127.0.0.1/activate?username=" + user.getUsername() + "&token=" + token;
        emailService.sendActivationEmail(user.getEmail(), activationLink);

        return toUserDto(userRepository.save(user));
    }

    public UserDto registerManager(SignUpDto userDto) {
        if (userRepository.findByUsername(userDto.username()).isPresent()) {
            throw new AppException("Username already taken", HttpStatus.BAD_REQUEST);
        }

        User user = signUpToUser(userDto);
        user.setType(UserType.Manager);
        user.setActive(true);              // odmah aktivan
        user.setMustChangePassword(true);  // mora promeniti lozinku
        user.setPassword(passwordEncoder.encode(CharBuffer.wrap(userDto.password())));

        return toUserDto(userRepository.save(user));
    }

    public UserDto registerSuperAdmin(SignUpDto userDto) {
        if (userRepository.findByUsername(userDto.username()).isPresent()) {
            throw new AppException("Username already taken", HttpStatus.BAD_REQUEST);
        }

        User user = signUpToUser(userDto);
        user.setType(UserType.SuperAdmin);
        user.setActive(true);              // odmah aktivan
        user.setMustChangePassword(true);  // mora promeniti lozinku
        user.setPassword(passwordEncoder.encode(CharBuffer.wrap(userDto.password())));

        return toUserDto(userRepository.save(user));
    }

    @Caching(evict = {
        @CacheEvict(value = "users", key = "#username"),
        @CacheEvict(value = "managers", allEntries = true)
    })
    public UserDto blockManager(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Unknown user", HttpStatus.NOT_FOUND));
        if (user.getType() == UserType.SuperAdmin) {
            throw new AppException("You cannot block a super admin", HttpStatus.FORBIDDEN);
        }
        user.setActive(false);
        return toUserDto(userRepository.save(user));
    }
    
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#username"),
        @CacheEvict(value = "managers", allEntries = true)
    })
    public UserDto unblockManager(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Unknown user", HttpStatus.NOT_FOUND));
        user.setActive(true);
        return toUserDto(userRepository.save(user));
    }

    @CacheEvict(value = "users", key = "#username")
    public UserDto changePasswordAndReturn(ChangePasswordDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDto loggedUser = (UserDto) authentication.getPrincipal();
        User user = userRepository.findByUsername(loggedUser.getUsername())
                .orElseThrow(() -> new AppException("Unknown user", HttpStatus.NOT_FOUND));

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        user.setMustChangePassword(false);

        return toUserDto(userRepository.save(user));
    }

    @CacheEvict(value = "users", key = "#username")
    public UserDto update(SignUpDto signUpDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Unknown user", HttpStatus.NOT_FOUND));

        User updated = signUpToUser(signUpDto);
        updated.setUsername(username);

        if (signUpDto.password() == null || signUpDto.password().length == 0) {
            updated.setPassword(user.getPassword());
        } else {
            updated.setPassword(passwordEncoder.encode(CharBuffer.wrap(signUpDto.password())));
        }

        userRepository.deleteById(username);
        return toUserDto(userRepository.save(updated));
    }

    public void delete(String username) {
        userRepository.deleteById(username);
    }

    @Cacheable(value = "users", key = "#username")
    public UserDto findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Unknown user", HttpStatus.NOT_FOUND));
        return toUserDto(user);
}

    public boolean isUsernameTaken(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public boolean validateActivationToken(String username, String token) {
        return userRepository.findByUsername(username)
                .map(u -> token.equals(u.getActivationToken()) && !u.isActive())
                .orElse(false);
    }

    public void activateUser(String username) {
        userRepository.findByUsername(username).ifPresent(u -> {
            u.setActive(true);
            u.setActivationToken(null);
            userRepository.save(u);
        });
    }

    public UserDto toUserDto(User user) {
        if (user == null) return null;
        return new UserDto(
                user.getUsername(), null, user.getType(),
                user.isActive(), user.isMustChangePassword(),
                user.getEmail(), user.getImage(), user.getActivationToken()
        );
    }

    public List<UserDto> toUserDtos(List<User> users) {
        if (users == null) return null;
        List<UserDto> list = new ArrayList<>(users.size());
        for (User u : users) list.add(toUserDto(u));
        return list;
    }

    public User signUpToUser(SignUpDto dto) {
        if (dto == null) return null;
        User user = new User();
        user.setUsername(dto.username());
        user.setType(dto.type());
        user.setActive(dto.isActive());
        user.setMustChangePassword(dto.mustChangePassword());
        user.setEmail(dto.email());
        user.setImage(dto.image());
        user.setActivationToken(dto.activationToken());
        return user;
    }
}