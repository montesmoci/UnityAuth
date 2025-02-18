package io.unityfoundation.auth;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Serdeable
public record HasPermissionRequest(
    @NotNull String tenantId,
    @NotNull String serviceId,
    List<String> permissions

) {}
