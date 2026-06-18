package com.doc.entity.vendor;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vendor_contacts")
@Getter
@Setter
@NoArgsConstructor
public class VendorContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    private String name;
    private String designation;
    private String email;
    private String mobile;
    private boolean isPrimary = false;


}