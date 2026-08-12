package com.example.backend.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Specifies auto-increment behavior
    private Integer id;
    private String message;
    private String price;
    private BigInteger telegramId;
    private LocalDateTime dateTime;
    private String name;
    private String castingType;
    private Boolean status;
    @OneToOne
    private CastingUser castingUser;


    public Message(CastingUser castingUser, String message, String price, LocalDateTime dateTime, Boolean status) {
        this.castingUser = castingUser;
        this.message = message;
        this.price = price;
        this.dateTime = dateTime;
        this.status = status;
    }
}
