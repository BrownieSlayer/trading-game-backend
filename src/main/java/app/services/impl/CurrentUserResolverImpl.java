package app.services.impl;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import app.models.User;
import app.repositories.UserRepository;
import app.services.CurrentUserResolver;
import app.services.DailyCycleService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentUserResolverImpl implements CurrentUserResolver {

    private final UserRepository userRepository;
    private final DailyCycleService dailyCycleService;

    @Override
    public User resolve(UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        dailyCycleService.resolveNewDay(user);
        return user;
    }
}
