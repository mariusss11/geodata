package com.geodata.repository;

import com.geodata.model.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;


public interface MapRepository extends JpaRepository<Map, Integer> {

    Optional<Map> findByName(String name);

    Optional<Map> findByNameAndYear(String name, int year);


    List<Map> findAllByIsEnabledTrue();

    Page<Map> findByIsEnabledTrueAndNameContainingIgnoreCase(String query, Pageable pageable);

}
