package app.models.converter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stocke une liste de {@link BigDecimal} de taille fixe (ex. la marche
 * aléatoire du trading rapide, 25 valeurs) dans une seule colonne texte,
 * séparées par des virgules — évite d'introduire une dépendance JSON ou une
 * table enfant pour un tableau de taille connue et modeste.
 */
@Converter
public class BigDecimalListConverter implements AttributeConverter<List<BigDecimal>, String> {

    @Override
    public String convertToDatabaseColumn(List<BigDecimal> attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.stream().map(BigDecimal::toPlainString).collect(Collectors.joining(","));
    }

    @Override
    public List<BigDecimal> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        return Arrays.stream(dbData.split(",")).map(BigDecimal::new).toList();
    }
}
