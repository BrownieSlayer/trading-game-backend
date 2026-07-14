package app.services;

import java.util.List;
import app.dto.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Service de gestion des utilisateurs")
public interface UserService {

    List<UserDto> getAll();
    
    UserDto getById(long id);

    UserDto create(UserRequest dto);

    UserDto update(long id, UserRequest dto);

    void delete(long id);

    void toggle(String username, boolean enabled);
}