package app.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import app.dto.UserRequest;
import app.dto.UserDto;
import app.mappers.UserMapper;
import app.models.User;
import app.repositories.UserRepository;
import app.services.impl.UserServiceImpl;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitaires du service {@link UserServiceImpl}.
 *
 * <p>
 * Ces tests utilisent Mockito pour isoler complètement le service :
 * <ul>
 *     <li>@ExtendWith(MockitoExtension.class) Permet l'utilisation des classes de mocks de Mockito</li>
 *     <li>Les dépendances UserRepository et UserMapper sont mockées (avec @Mock)</li>
 *     <li>Les dépendances UserRepository et UserMapper sont injectées dans notre service (avec @InjectMocks)</li>
 *     <li>Cela permet de contrôler parfaitement les valeurs retournées</li>
 *     <li>Le test ne touche pas à la base de données</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper mapper;

    @InjectMocks
    private UserServiceImpl userService;

    /**
     * Vérifie que le service retourne bien la liste des utilisateurs
     */
    @Test
    void testGetAll() {
        User firstUser = mockUser(1L,"John", "johnny");
        User secondUser = mockUser(2L,"Alice", "alice");

        UserDto firstUserResponse = new UserDto("John", app.enums.SecurityRole.ROLE_USER);
        UserDto secondUserResponse = new UserDto("Alice", app.enums.SecurityRole.ROLE_USER);

        when(userRepository.findAll()).thenReturn(List.of(firstUser, secondUser));
        when(mapper.convertToUserDto(firstUser)).thenReturn(firstUserResponse);
        when(mapper.convertToUserDto(secondUser)).thenReturn(secondUserResponse);

        List<UserDto> result = userService.getAll();

        assertEquals(2, result.size());
    }

    /**
     * Vérifie que getById retourne bien un utilisateur lorsqu’il existe
     */
    @Test
    void testGetById() {
        User user = mockUser(1L,"John", "johnny");
        UserDto userResponse = new UserDto("John", app.enums.SecurityRole.ROLE_USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(mapper.convertToUserDto(user)).thenReturn(userResponse);

        UserDto result = userService.getById(1L);

        assertEquals("John", result.username());
    }

    /**
     * Vérifie que getById lance une exception si l’utilisateur n’existe pas
     */
    @Test
    void testGetByIdNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.getById(1L));
    }

    /**
     * Vérifie la création d’un utilisateur via le service
     */
    @Test
    void testCreate() {
        UserRequest userRequest = new UserRequest("John", "johnny");
        UserDto userResponse = new UserDto("John", app.enums.SecurityRole.ROLE_USER);

        User user = mockUser("John", "johnny");
        User savedUser = mockUser("John", "johnny");

        when(mapper.convertToUser(userRequest)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(savedUser);
        when(mapper.convertToUserDto(savedUser)).thenReturn(userResponse);

        UserDto result = userService.create(userRequest);

        assertEquals("John", result.username());
    }

    /**
     * Vérifie la mise à jour d’un utilisateur existant
     */
    @Test
    void testUpdate() {
        UserRequest userRequest = new UserRequest("John Updated", "john.updated@mail.com");
        UserDto userResponse = new UserDto("John Updated", app.enums.SecurityRole.ROLE_USER);

        User user = mockUser("John", "johnny");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(mapper.convertToUserDto(user)).thenReturn(userResponse);

        UserDto result = userService.update(1L, userRequest);

        assertEquals("John Updated", result.username());
    }

    /**
     * Vérifie que la suppression fonctionne pour un utilisateur existant
     */
    @Test
    void testDelete() {
        when(userRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> userService.delete(1L));
        verify(userRepository).deleteById(1L);
    }

    /**
     * Vérifie qu’une erreur est levée si l’utilisateur n’existe pas lors de la suppression
     */
    @Test
    void testDeleteNotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> userService.delete(1L));
    }

    /**
     * Méthode utilitaire permettant de créer rapidement un utilisateur factice pour les tests
     * @param username
     * @param password
     * @return
     */
    private User mockUser(String username, String password){
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        return user;
    }

    /**
     * Méthode utilitaire permettant de créer rapidement un utilisateur factice pour les tests avec un id
     * @param id
     * @param username
     * @param password
     * @return
     */
    private User mockUser(long id, String username, String password){
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        return user;
    }
}
