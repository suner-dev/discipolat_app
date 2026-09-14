package com.discipolat.common.multitenancy;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import java.util.Arrays;

public class TenantFilterIntegrator implements Integrator {

    @Override
    public void integrate(Metadata metadata, BootstrapContext bootstrapContext,
                          SessionFactoryImplementor sessionFactory) {
        EventListenerRegistry registry = sessionFactory
                .getServiceRegistry()
                .getService(EventListenerRegistry.class);
        registry.appendListeners(EventType.PRE_INSERT, new TenantAutoSetListener());
        registry.appendListeners(EventType.PRE_UPDATE, new TenantAutoSetListener());
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory,
                             SessionFactoryServiceRegistry serviceRegistry) {
    }

    static class TenantAutoSetListener implements org.hibernate.event.spi.PreInsertEventListener,
            org.hibernate.event.spi.PreUpdateEventListener {

        @Override
        public boolean onPreInsert(org.hibernate.event.spi.PreInsertEvent event) {
            autoSetTenantId(event.getPersister(), event.getState());
            return false;
        }

        @Override
        public boolean onPreUpdate(org.hibernate.event.spi.PreUpdateEvent event) {
            autoSetTenantId(event.getPersister(), event.getState());
            return false;
        }

        private static final java.util.UUID DEFAULT_TENANT_ID =
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");

        private void autoSetTenantId(EntityPersister persister, Object[] state) {
            String[] propertyNames = persister.getPropertyNames();
            int index = java.util.Arrays.asList(propertyNames).indexOf("tenantId");
            if (index < 0 || state[index] != null) {
                // Aucune colonne tenant_id, ou tenantId déjà fixé explicitement par
                // le service : on ne touche à rien.
                return;
            }
            java.util.UUID tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                // Contexte de requête HTTP : le tenant du JWT.
                state[index] = tenantId;
                return;
            }
            // Hors contexte (jobs planifiés, initialiseurs, seed) : seuls les
            // tenants REQUIS (colonnes NOT NULL) doivent être remplis, avec repli
            // sur le tenant par défaut créé par la migration V70. Les lignes
            // systèmes volontairement NULL (roles, permissions) restent NULL.
            boolean[] nullability = persister.getPropertyNullability();
            boolean required = nullability == null || index >= nullability.length || !nullability[index];
            if (required) {
                state[index] = DEFAULT_TENANT_ID;
            }
        }
    }
}
