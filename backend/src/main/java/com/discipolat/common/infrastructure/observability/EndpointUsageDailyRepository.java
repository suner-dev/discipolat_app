package com.discipolat.common.infrastructure.observability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface EndpointUsageDailyRepository extends JpaRepository<EndpointUsageDaily, EndpointUsageDaily.Key> {

    /** Upsert cumulatif —PostgreSQL ON CONFLICT (PK). */
    @Modifying
    @Query(value = """
            INSERT INTO endpoint_usage_daily (day, method, route, calls, errors, last_seen)
            VALUES (CAST(:day AS date), :method, :route, :calls, :errors, CAST(:lastSeen AS timestamptz))
            ON CONFLICT (day, method, route)
            DO UPDATE SET calls = endpoint_usage_daily.calls + :calls,
                          errors = endpoint_usage_daily.errors + :errors,
                          last_seen = GREATEST(COALESCE(endpoint_usage_daily.last_seen, CAST(:lastSeen AS timestamptz)),
                                              CAST(:lastSeen AS timestamptz))
            """, nativeQuery = true)
    void upsertAdd(@Param("day") LocalDate day,
                   @Param("method") String method,
                   @Param("route") String route,
                   @Param("calls") long calls,
                   @Param("errors") long errors,
                   @Param("lastSeen") Instant lastSeen);

    List<EndpointUsageDaily> findByDayGreaterThanEqual(LocalDate from);

    @Query("SELECT e.route, e.method, SUM(e.calls) AS total FROM EndpointUsageDaily e "
            + "WHERE e.day >= :from GROUP BY e.route, e.method")
    List<Object[]> sumCallsSince(@Param("from") LocalDate from);
}
