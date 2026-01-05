package com.geodata.model;

import jakarta.persistence.*;
import lombok.*;

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
    private int userId;

    @Column(name = "map_id")
    private int mapId;

    private LocalDateTime borrowDate;

    private LocalDateTime returnDate;

    private String status;

//    @Column(length = 500)
//    private String statusReason;

    public Borrows(int client, int item, String status) {
        this.userId = client;
        this.mapId = item;
        this.status = status;
    }
}
