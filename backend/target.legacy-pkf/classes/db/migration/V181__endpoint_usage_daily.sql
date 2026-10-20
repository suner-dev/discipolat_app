-- V181 : Mesure d'usage des endpoints HTTP (identification du code mort)
-- Agrégats quotidiens alimentés par EndpointUsageInterceptor (in-memory -> flush planifié).

CREATE TABLE IF NOT EXISTS endpoint_usage_daily (
    day        date         NOT NULL,
    method     varchar(10)  NOT NULL,
    route      varchar(255) NOT NULL,
    calls      bigint       NOT NULL DEFAULT 0,
    errors     bigint       NOT NULL DEFAULT 0,
    last_seen  timestamptz,
    PRIMARY KEY (day, method, route)
);

CREATE INDEX IF NOT EXISTS idx_endpoint_usage_day ON endpoint_usage_daily (day);
