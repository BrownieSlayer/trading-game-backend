package app.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import app.models.User;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Recherche un utilisateur par son username.
     *
     * @param username le nom d'utilisateur
     * @return Optional contenant l'utilisateur si trouvé
     */
    Optional<User> findByUsername(String username);
}