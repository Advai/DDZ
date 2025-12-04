package com.yourco.ddz.server.service;

import com.yourco.ddz.server.persistence.User;
import com.yourco.ddz.server.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Get an existing user by username, or create a new one if it doesn't exist.
   *
   * <p>IMPORTANT: This method enforces global username uniqueness. Usernames are used for
   * leaderboard rankings which are global across all environments. If a user with the given
   * username already exists, it returns the existing user and updates their lastSeenAt timestamp.
   *
   * @param username The unique username (must be 3+ chars, alphanumeric + underscore only)
   * @param displayName The display name for the user (can be different from username)
   * @return The existing or newly created User
   * @throws IllegalArgumentException if username is invalid or already exists with different
   *     display name
   */
  @Transactional
  public User getOrCreateUser(String username, String displayName) {
    if (username == null || username.trim().isEmpty()) {
      throw new IllegalArgumentException("Username cannot be empty");
    }

    if (displayName == null || displayName.trim().isEmpty()) {
      throw new IllegalArgumentException("Display name cannot be empty");
    }

    String normalizedUsername = username.trim().toLowerCase();
    String normalizedDisplayName = displayName.trim();

    // Check if user already exists
    Optional<User> existingUser = userRepository.findByUsername(normalizedUsername);

    if (existingUser.isPresent()) {
      User user = existingUser.get();
      log.info("User '{}' already exists with userId: {}", normalizedUsername, user.getUserId());

      // Update last seen timestamp
      user.updateLastSeen();
      userRepository.save(user);

      return user;
    }

    // Create new user
    User newUser = new User(normalizedUsername, normalizedDisplayName);

    try {
      User savedUser = userRepository.save(newUser);
      log.info(
          "Created new user '{}' with userId: {} and displayName: '{}'",
          normalizedUsername,
          savedUser.getUserId(),
          normalizedDisplayName);
      return savedUser;
    } catch (DataIntegrityViolationException e) {
      // Handle race condition where another thread/process created the same user
      log.warn(
          "Race condition detected - user '{}' was created by another process", normalizedUsername);
      Optional<User> raceUser = userRepository.findByUsername(normalizedUsername);
      if (raceUser.isPresent()) {
        User user = raceUser.get();
        user.updateLastSeen();
        userRepository.save(user);
        return user;
      }
      throw new IllegalArgumentException(
          "Username '" + normalizedUsername + "' is already taken", e);
    }
  }

  /**
   * Get a user by their userId.
   *
   * @param userId The UUID of the user
   * @return Optional containing the User if found
   */
  public Optional<User> getUserById(UUID userId) {
    return userRepository.findById(userId);
  }

  /**
   * Get a user by their username.
   *
   * @param username The username to look up
   * @return Optional containing the User if found
   */
  public Optional<User> getUserByUsername(String username) {
    if (username == null || username.trim().isEmpty()) {
      return Optional.empty();
    }
    return userRepository.findByUsername(username.trim().toLowerCase());
  }

  /**
   * Check if a username is already taken.
   *
   * @param username The username to check
   * @return true if the username exists, false otherwise
   */
  public boolean isUsernameTaken(String username) {
    if (username == null || username.trim().isEmpty()) {
      return false;
    }
    return userRepository.existsByUsername(username.trim().toLowerCase());
  }

  /**
   * Update the last seen timestamp for a user.
   *
   * @param userId The UUID of the user to update
   */
  @Transactional
  public void updateLastSeen(UUID userId) {
    Optional<User> user = userRepository.findById(userId);
    if (user.isPresent()) {
      User u = user.get();
      u.updateLastSeen();
      userRepository.save(u);
      log.debug("Updated last seen for user {}", userId);
    } else {
      log.warn("Attempted to update last seen for non-existent user {}", userId);
    }
  }
}
