package com.doc.entity.project;

import com.doc.entity.document.Document;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(
        name = "project_directory",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_project_directory_name",
                        columnNames = {"project_id", "directory_name"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProjectDirectory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "directory_name", nullable = false, length = 150)
    private String directoryName;

    /*
     * Multiple documents can be stored inside one directory.
     * No change is required in the Document entity.
     */
    @OneToMany(
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JoinTable(
            name = "project_directory_documents",
            joinColumns = @JoinColumn(name = "directory_id"),
            inverseJoinColumns = @JoinColumn(name = "document_id"),
            uniqueConstraints = {
                    @UniqueConstraint(
                            name = "uk_project_directory_document",
                            columnNames = "document_id"
                    )
            }
    )
    private List<Document> documents = new ArrayList<>();

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (createdDate == null) {
            createdDate = new Date();
        }
    }
}