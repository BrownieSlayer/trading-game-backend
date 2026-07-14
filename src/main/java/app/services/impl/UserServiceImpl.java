package app.services.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import app.dto.*;
import app.mappers.UserMapper;
import app.models.User;
import app.repositories.UserRepository;
import app.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll()
                .stream()
                .map(mapper::convertToUserDto)
                .toList();
    }

    @Override
    public UserDto getById(long id) {
        return userRepository.findById(id)
                .map(mapper::convertToUserDto)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public UserDto create(UserRequest dto) {
        User user = mapper.convertToUser(dto);
        User saved = userRepository.save(user);
        return mapper.convertToUserDto(saved);
    }

    @Override
    @Transactional
    public UserDto update(long id, UserRequest dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        mapper.updateFromDTO(dto, user);

        User saved = userRepository.save(user);
        return mapper.convertToUserDto(saved);
    }

    @Override
    @Transactional
    public void delete(long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void toggle(String username, boolean enabled) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setEnabled(enabled);
        userRepository.save(user);
    }
}