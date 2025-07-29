package com.doc.entity.client;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class PaymentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary Key: Unique identifier for payment type")
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    @Comment("Payment Type Name: FULL, PARTIAL, MILESTONE, PO_BASED")
    private String name;

}
