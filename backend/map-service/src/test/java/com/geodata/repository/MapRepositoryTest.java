package com.geodata.repository;

import com.geodata.model.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(FlywayAutoConfiguration.class)
class MapRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    MapRepository mapRepository;

    private Map map1;
    private Map map2;

    @BeforeEach
    void setUp() {
        mapRepository.deleteAll();

        map1 = mapRepository.save(Map.builder()
                .name("Topographic Map").year(2020)
                .availabilityStatus("AVAILABLE").isEnabled(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        map2 = mapRepository.save(Map.builder()
                .name("Road Atlas").year(2019)
                .availabilityStatus("BORROWED").isEnabled(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
    }

    @Test
    void findByName_existingMap_returnsMap() {
        Optional<Map> result = mapRepository.findByName("Topographic Map");
        assertThat(result).isPresent();
        assertThat(result.get().getYear()).isEqualTo(2020);
    }

    @Test
    void findByName_unknownMap_returnsEmpty() {
        assertThat(mapRepository.findByName("Ghost Map")).isEmpty();
    }

    @Test
    void findByNameAndYear_exactMatch_returnsMap() {
        Optional<Map> result = mapRepository.findByNameAndYear("Topographic Map", 2020);
        assertThat(result).isPresent();
    }

    @Test
    void findByNameAndYear_wrongYear_returnsEmpty() {
        Optional<Map> result = mapRepository.findByNameAndYear("Topographic Map", 1999);
        assertThat(result).isEmpty();
    }

    @Test
    void findAllByIsEnabledTrue_returnsOnlyEnabled() {
        List<Map> result = mapRepository.findAllByIsEnabledTrue();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Topographic Map");
    }

    @Test
    void findAllByIsEnabledTrue_paginated_returnsPage() {
        Page<Map> page = mapRepository.findAllByIsEnabledTrue(PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findByIsEnabledTrueAndNameContainingIgnoreCase_matchesPartialName() {
        Page<Map> page = mapRepository
                .findByIsEnabledTrueAndNameContainingIgnoreCase("topo", PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("Topographic Map");
    }

    @Test
    void findByIsEnabledTrueAndNameContainingIgnoreCase_noMatch_returnsEmpty() {
        Page<Map> page = mapRepository
                .findByIsEnabledTrueAndNameContainingIgnoreCase("xyz", PageRequest.of(0, 10));
        assertThat(page.getContent()).isEmpty();
    }
}
