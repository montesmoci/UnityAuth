package io.unityfoundation.auth.entities;


import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface TenantRepo extends CrudRepository<Tenant, Long> {


  Tenant findByName(String tenantName);
}
