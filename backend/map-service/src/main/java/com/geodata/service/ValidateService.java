package com.geodata.service;

import com.geodata.exceptions.*;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ValidateService {

    public static void validateTitle(@NonNull String title) {
        if (title.isBlank() || !title.matches("^[a-zA-Z0-9\\s']{1,50}$"))
            throw new InvalidTitleException("Invalid title: " + title);
    }

    public static void validateName(@NonNull String name) {
        if (name.isBlank() || !name.matches("^[a-zA-Z][a-zA-Z\\s'.]{0,50}[a-zA-Z]$"))
            throw new InvalidNameException("Invalid name: " + name);
    }

    public static void validateEmail(@NonNull String email) {
        if (email.isBlank() || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
            throw new InvalidEmailException("Invalid email: " + email);
    }

    public static void isYearValid(int year) {
        if (year == 0)
            throw new InvalidItemYearException("Invalid year assigned to item");
        if (LocalDate.now().getYear() < year)
            throw new InvalidItemYearException("Year of publication cannot be in the future.");
    }
}