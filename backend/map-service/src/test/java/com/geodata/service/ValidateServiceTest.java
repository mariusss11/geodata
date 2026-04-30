package com.geodata.service;

import com.geodata.exceptions.InvalidEmailException;
import com.geodata.exceptions.InvalidItemYearException;
import com.geodata.exceptions.InvalidNameException;
import com.geodata.exceptions.InvalidTitleException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class ValidateServiceTest {

    @Test
    void validateTitle_valid() {
        assertThatNoException().isThrownBy(() -> ValidateService.validateTitle("Topo Map 2020"));
    }

    @Test
    void validateTitle_blank_throws() {
        assertThatThrownBy(() -> ValidateService.validateTitle(""))
                .isInstanceOf(InvalidTitleException.class);
    }

    @Test
    void validateTitle_tooLong_throws() {
        assertThatThrownBy(() -> ValidateService.validateTitle("A".repeat(51)))
                .isInstanceOf(InvalidTitleException.class);
    }

    @Test
    void validateName_valid() {
        assertThatNoException().isThrownBy(() -> ValidateService.validateName("John Doe"));
    }

    @Test
    void validateName_blank_throws() {
        assertThatThrownBy(() -> ValidateService.validateName("  "))
                .isInstanceOf(InvalidNameException.class);
    }

    @Test
    void validateEmail_valid() {
        assertThatNoException().isThrownBy(() -> ValidateService.validateEmail("user@example.com"));
    }

    @Test
    void validateEmail_invalid_throws() {
        assertThatThrownBy(() -> ValidateService.validateEmail("not-an-email"))
                .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void isYearValid_zeroYear_throws() {
        assertThatThrownBy(() -> ValidateService.isYearValid(0))
                .isInstanceOf(InvalidItemYearException.class);
    }

    @Test
    void isYearValid_futureYear_throws() {
        int futureYear = LocalDate.now().getYear() + 1;
        assertThatThrownBy(() -> ValidateService.isYearValid(futureYear))
                .isInstanceOf(InvalidItemYearException.class);
    }

    @Test
    void isYearValid_currentYear_passes() {
        assertThatNoException().isThrownBy(() -> ValidateService.isYearValid(LocalDate.now().getYear()));
    }
}
