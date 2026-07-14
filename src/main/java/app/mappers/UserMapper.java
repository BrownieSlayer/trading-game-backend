package app.mappers;

import app.dto.*;
import app.models.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto convertToUserDto(User user);

    User convertToUser(UserRequest userRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDTO(UserRequest userRequest, @MappingTarget User user);
}
