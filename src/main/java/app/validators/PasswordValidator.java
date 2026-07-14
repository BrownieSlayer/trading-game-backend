package app.validators;

import java.util.regex.Pattern;

public class PasswordValidator {
    
    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");
    
    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caractères");
        }
        if (!UPPERCASE.matcher(password).find()) {
            throw new IllegalArgumentException("Le mot de passe doit contenir une majuscule");
        }
        if (!LOWERCASE.matcher(password).find()) {
            throw new IllegalArgumentException("Le mot de passe doit contenir une minuscule");
        }
        if (!DIGIT.matcher(password).find()) {
            throw new IllegalArgumentException("Le mot de passe doit contenir un chiffre");
        }
        if (!SPECIAL.matcher(password).find()) {
            throw new IllegalArgumentException("Le mot de passe doit contenir un caractère spécial");
        }
    }
}