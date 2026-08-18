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

        private void autoSetTenantId(EntityPersister persister, Object[] state) {
            // Contexte de requête HTTP : le tenant du JWT. Hors contexte
            // (jobs planifiés, initialiseurs, tâches système) : repli sur le
            // tenant par défaut créé/backfillé par V70 — sans quoi tout insert
            // échouerait sur la contrainte tenant_id NOT NULL.
            java.util.UUID tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                tenantId = TenantContext.DEFAULT_TENANT_ID;
            }
            String[] propertyNames = persister.getPropertyNames();
            int index = Arrays.asList(propertyNames).indexOf("tenantId");
            if (index >= 0 && state[index] == null) {
                state[index] = tenantId;
            }
        }
    }
}
