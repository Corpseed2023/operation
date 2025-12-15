package com.doc.entity.document;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "documents",
        indexes = {
                @Index(name = "idx_uuid", columnList = "uuid", unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "uuid", nullable = false, unique = true, length = 36, updatable = false)
    private String uuid;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;
}