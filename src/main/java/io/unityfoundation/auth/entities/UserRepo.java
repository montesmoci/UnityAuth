package io.unityfoundation.auth.entities;


import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.unityfoundation.auth.AuthController.TenantPermission;
import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface UserRepo extends CrudRepository<User, Long> {

  Optional<User> findByEmail(String email);

  @Query("""
      SELECT count(*) > 0
FROM user_role ur
         inner join user u on u.id = ur.user_id
         inner join tenant t on t.id = ur.tenant_id
         inner join tenant_service ts on ts.tenant_id = t.id
         INNER join service s on s.id = ts.service_id
where u.id = :userId
  and t.status = 'ACTIVE'
  and s.id = :serviceId
  and s.status = 'ENABLED';
""")
  Boolean isServiceAvailable(long userId, long serviceId);

  @Query("""
      SELECT id,
       password,
       email,
       status
FROM user
WHERE email = :email
""")
  Optional<User> findUserForAuthentication(String email);

  @Query("""
      select ur.tenant_id as tenant_id, p.name as permission_name, p.`scope` as permission_scope
from user_role ur
         inner join role_permission rp on rp.role_id = ur.role_id
         inner join permission p on p.id = rp.permission_id
where ur.user_id = :userId
""")
  List<TenantPermission> getTenantPermissionsFor(Long userId);


}
