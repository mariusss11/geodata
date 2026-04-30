package com.geodata.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "borrows")
public class Borrows {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "map_id")
    private int mapId;

    @Column(name = "borrower_name")
    private String borrowerName;

    private LocalDateTime borrowDate;

    private LocalDate returnDate;

    @Column(name = "actual_return_date")
    private LocalDateTime actualReturnDate;

    private String status;
}
