package app.services;

import java.util.Optional;

import app.dto.minigames.WheelSpinDto;
import app.models.User;

/** Port de {@code minigames/roue.py} : un tirage pondéré par jour civil, verrou quotidien indépendant du reste. */
public interface RoueService {

    /** Effectue le tirage du jour. Lève une exception si déjà joué aujourd'hui. */
    WheelSpinDto spin(User user);

    Optional<WheelSpinDto> getTodayResult(User user);
}
