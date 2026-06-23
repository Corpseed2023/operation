package com.doc.repository.vendor;

import com.doc.dto.vendor.RFQVendorResponseDto;
import com.doc.entity.vendor.RFQVendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RFQVendorRepository extends JpaRepository<RFQVendor, Long> {

    @Query("""
    SELECT new com.doc.dto.vendor.RFQVendorResponseDto(
        rv.id,
        v.id,
        v.name,
        v.email,
        v.mobile,
        v.gstNumber,
        v.panNumber,
        CAST(v.status AS string)
    )
    FROM RFQVendor rv
    JOIN rv.vendor v
    WHERE rv.rfq.id = :rfqId
    AND rv.isDeleted = false
    ORDER BY rv.id DESC
""")
    List<RFQVendorResponseDto> findVendorsByRfqId(@Param("rfqId") Long rfqId);

    @Query("""
    SELECT new com.doc.dto.vendor.RFQVendorResponseDto(
        rv.id,
        v.id,
        v.name,
        v.email,
        v.mobile,
        v.gstNumber,
        v.panNumber,
        CAST(v.status AS string)
    )
    FROM RFQVendor rv
    JOIN rv.vendor v
    WHERE rv.rfq.id = :rfqId
    AND v.id = :vendorId
    AND rv.isDeleted = false
""")
    Optional<RFQVendorResponseDto> findVendorByRfqIdAndVendorId(
            @Param("rfqId") Long rfqId,
            @Param("vendorId") Long vendorId
    );
}