package com.discipolat.common.infrastructure.observability;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Agrégat quotidien d'appels d'un endpoint HTTP (méthode + patron de route).
 * Alimente le rapport de code mort de la plateforme (endpoints jamais consommés).
 */
@Entity
@Table(name = "endpoint_usage_daily")
@IdClass(EndpointUsageDaily.Key.class)
public class EndpointUsageDaily {

    public static class Key implements Serializable {
        private LocalDate day;
        private String method;
        private String route;

        public Key() {
        }

        public Key(LocalDate day, String method, String route) {
            this.day = day;
            this.method = method;
            this.route = route;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(day, key.day) && Objects.equals(method, key.method)
                    && Objects.equals(route, key.route);
        }

        @Override
        public int hashCode() {
            return Objects.hash(day, method, route);
        }
    }

    @Id
    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Id
    @Column(name = "method", nullable = false, length = 10)
    private String method;

    @Id
    @Column(name = "route", nullable = false, length = 255)
    private String route;

    @Column(name = "calls", nullable = false)
    private long calls;

    @Column(name = "errors", nullable = false)
    private long errors;

    @Column(name = "last_seen")
    private Instant lastSeen;

    public LocalDate getDay() { return day; }
    public void setDay(LocalDate day) { this.day = day; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public long getCalls() { return calls; }
    public void setCalls(long calls) { this.calls = calls; }
    public long getErrors() { return errors; }
    public void setErrors(long errors) { this.errors = errors; }
    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }
}
