package com.doc.entity.document;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "document_statuses", indexes = {
        @Index(name = "idx_name", columnList = "name", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class DocumentStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Document status ID")
    private Long id;

    @Column(nullable = false, unique = true)
    @Comment("Status name (e.g., PENDING, UPLOADED)")
    private String name;

    @Column(columnDefinition = "varchar(1000)")
    @Comment("Detailed description of the status")
    private String description;
}