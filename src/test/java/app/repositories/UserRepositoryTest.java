package app.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import app.configuration.TestcontainersConfiguration;
import app.enums.SecurityRole;
import app.models.User;

/**
 * Tests unitaires du repository {@link UserRepository}.
 *
 * <p>
 * Cette classe utilise l'annotation {@link DataJpaTest} qui :
 * <ul>
 *     <li>Charge uniquement la partie JPA/Hibernate du contexte Spring</li>
 *     <li>Configure automatiquement une base H2 en mémoire pour les tests</li>
 *     <li>Exécute chaque test dans une transaction rollbackée</li>
 * </ul>
 * </p>
 *
 * Ces tests valident le comportement CRUD basique du repository
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    
    @Autowired
    private UserRepository userRepository;

    /**
     * Vérifie qu'un utilisateur sauvegardé peut être retrouvé via son ID
     */
    @Test
    void testGetUser() {
        User user = mockUser("johnny");
        User userSaved = userRepository.save(user);

        var userFound = userRepository.findById(userSaved.getId());

        assertTrue(userFound.isPresent());
        assertEquals("johnny", userFound.get().getUsername());
    }

    /**
     * Vérifie que la sauvegarde d'un utilisateur persiste correctement les valeurs en base
     */
    @Test
    void testSaveUser() {

        User user = mockUser("johnny");
        User userSaved = userRepository.save(user);

        assertNotNull(userSaved.getId());
        assertEquals("johnny", userSaved.getUsername());
    }

    /**
     * Vérifie qu'une mise à jour d'un utilisateur modifie bien les données existantes
     */
    @Test
    void testUdpateUser() {

        User user = mockUser("John");
        User userSaved = userRepository.save(user);

        user.setUsername("John");
        User userUpdated = userRepository.save(user);

        assertNotNull(userSaved.getId());
        assertEquals("John", userUpdated.getUsername());
    }

    /**
     * Vérifie la suppression d'un utilisateur via son id
     */
    @Test
    void testDeleteUser() {
        User user = mockUser("Bob");
        User userSaved = userRepository.save(user);

        userRepository.deleteById(userSaved.getId());

        assertFalse(userRepository.findById(userSaved.getId()).isPresent());
    }

    /**
     * Vérifie que la récupération de tous les utilisateurs renvoie bien la bonne liste
     */
    @Test
    void testFindAllUsers() {
        userRepository.save(mockUser("johnny"));
        userRepository.save(mockUser("Bob"));

        var users = userRepository.findAll();

        assertEquals(2, users.size());
    }

    /**
     * Méthode utilitaire permettant de créer rapidement un utilisateur factice pour les tests
     * @param username
     * @return
     */
    private User mockUser(String username){
        User user = new User();
        user.setUsername(username);
        user.setPassword("$2a$10$dummyhashedpassword");
        user.setRole(SecurityRole.ROLE_USER);
        user.setEnabled(true);
        return user;
    }
}