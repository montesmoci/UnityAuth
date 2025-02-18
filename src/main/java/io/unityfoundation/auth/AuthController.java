package io.unityfoundation.auth;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import io.unityfoundation.auth.entities.Permission.PermissionScope;
import io.unityfoundation.auth.entities.Service;
import io.unityfoundation.auth.entities.Service.ServiceStatus;
import io.unityfoundation.auth.entities.ServiceRepo;
import io.unityfoundation.auth.entities.Tenant;
import io.unityfoundation.auth.entities.TenantRepo;
import io.unityfoundation.auth.entities.User;
import io.unityfoundation.auth.entities.UserRepo;
import java.util.List;
import java.util.Optional;

@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("/api")
public class AuthController {

  private final UserRepo userRepo;
  private final ServiceRepo serviceRepo;
  private final TenantRepo tenantRepo;

  public AuthController(UserRepo userRepo, ServiceRepo serviceRepo, TenantRepo tenantRepo) {
    this.userRepo = userRepo;
    this.serviceRepo = serviceRepo;
    this.tenantRepo = tenantRepo;
  }

  @Post("/hasPermission")
  public HttpResponse<HasPermissionResponse> hasPermission(@Body HasPermissionRequest requestDTO,
      Authentication authentication) {

    User user = userRepo.findByEmail(authentication.getName()).orElse(null);
    if (checkUserStatus(user)) {
      return createHasPermissionResponse(false, "The user’s account has been disabled!");
    }

    Optional<Service> service = serviceRepo.findByName(requestDTO.serviceId());

    String serviceStatusCheckResult = checkServiceStatus(service);
    if (serviceStatusCheckResult != null) {
      return createHasPermissionResponse(false, serviceStatusCheckResult);
    }

    if (!userRepo.isServiceAvailable(user.getId(), service.get().getId())) {
      return createHasPermissionResponse(false,
          "The requested service is not enabled for the requested tenant!");
    }

    if (!checkUserPermission(user, requestDTO)) {
      return createHasPermissionResponse(false, "The user does not have permission!");
    }

    return createHasPermissionResponse(true, null);
  }

  private boolean checkUserStatus(User user) {
    return user == null || user.getStatus() != User.UserStatus.ENABLED;
  }

  private String checkServiceStatus(Optional<Service> service) {
    if (service.isEmpty()) {
      return "The service does not exists!";
    } else {
      ServiceStatus status = service.get().getStatus();
      if (ServiceStatus.DISABLED.equals(status)) {
        return "The service is disabled!";
      } else if (ServiceStatus.DOWN_FOR_MAINTENANCE.equals(status)) {
        return "The service is temporarily down for maintenance!";
      }
    }
    return null;
  }

  private boolean checkUserPermission(User user, HasPermissionRequest requestDTO) {
    Tenant tenant = tenantRepo.findByName(requestDTO.tenantId());
    List<TenantPermission> userPermissions = userRepo.getTenantPermissionsFor(user.getId()).stream()
        .filter(tenantPermission ->
            PermissionScope.SYSTEM.equals(tenantPermission.permissionScope()) ||
            ((PermissionScope.TENANT.equals(tenantPermission.permissionScope()) || PermissionScope.SUBTENANT.equals(tenantPermission.permissionScope()))
             && tenantPermission.tenantId == tenant.getId()))
        .toList();

    List<String> commonPermissions = userPermissions.stream()
        .map(TenantPermission::permissionName)
        .filter(requestDTO.permissions()::contains)
        .toList();

    return !commonPermissions.isEmpty();
  }

  private HttpResponse<HasPermissionResponse> createHasPermissionResponse(boolean hasPermission,
      String message) {
    return HttpResponse.ok(new HasPermissionResponse(hasPermission, message));
  }

  @Serdeable
  public record HasPermissionResponse(
      boolean hasPermission,
      @Nullable String errorMessage
  ) {

  }

  @Introspected
  public record TenantPermission(
      long tenantId,
      String permissionName,
      PermissionScope permissionScope

  ) {

  }

}
