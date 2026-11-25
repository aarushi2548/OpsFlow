
package opsflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateSupplierRequest(
    @NotBlank String name,
    @NotBlank @Email String email,
    String phone,
    String address
) {}
