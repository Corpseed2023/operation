package com.doc.dto.vendor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LegalUserWorkloadRequestDto {

    @NotEmpty(message = "At least one user is required")
    private List<@Valid LegalUserDto> users;

    @Getter
    @Setter
    public static class LegalUserDto {

        @NotNull(message = "User id is required")
        private Long id;

        private String name;
    }
}