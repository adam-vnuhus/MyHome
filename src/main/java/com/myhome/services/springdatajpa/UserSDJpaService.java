/*
 * Copyright 2020 Prathab Murugan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.myhome.services.springdatajpa;

import com.myhome.controllers.dto.UserDto;
import com.myhome.controllers.dto.mapper.UserMapper;
import com.myhome.domain.Community;
import com.myhome.domain.CommunityAdmin;
import com.myhome.domain.User;
import com.myhome.repositories.UserRepository;
import com.myhome.services.CommunityService;
import com.myhome.services.UserService;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Implements {@link UserService} and uses Spring Data JPA repository to does its work.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserSDJpaService implements UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final CommunityService communityService;

  @Override public Optional<UserDto> createUser(UserDto request) {
    if(userRepository.findByEmail(request.getEmail()) == null) {
      generateUniqueUserId(request);
      encryptUserPassword(request);
      return Optional.of(createUserInRepository(request));
    } else {
      return Optional.empty();
    }
  }

  @Override public Set<User> listAll() {
    return listAll(200, 0);
  }

  @Override public Set<User> listAll(Integer limit, Integer start) {
    return userRepository.findAll(PageRequest.of(start, limit)).toSet();
  }

  @Override
  public Optional<UserDto> getUserDetails(String userId) {
    User user = userRepository.findByUserId(userId);

    Set<String> communityIds = communityService.listAll().stream().filter(c -> c.getAdmins()
        .stream()
        .map(CommunityAdmin::getAdminId)
        .collect(Collectors.toSet())
        .contains(userId)).map(Community::getCommunityId).collect(Collectors.toSet());

    UserDto userDto = userMapper.userToUserDto(user);
    if (userDto == null) {
      return Optional.empty();
    }
    userDto.setCommunityIds(communityIds);
    return Optional.of(userDto);
  }

  private UserDto createUserInRepository(UserDto request) {
    User user = userMapper.userDtoToUser(request);
    User savedUser = userRepository.save(user);
    log.trace("saved user with id[{}] to repository", savedUser.getId());
    return userMapper.userToUserDto(savedUser);
  }

  private void encryptUserPassword(UserDto request) {
    request.setEncryptedPassword(passwordEncoder.encode(request.getPassword()));
  }

  private void generateUniqueUserId(UserDto request) {
    request.setUserId(UUID.randomUUID().toString());
  }
}
