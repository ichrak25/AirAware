package tn.airaware.api.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.context.Initialized;

@ApplicationScoped
public class StartupBean {

    @PostConstruct
    public void init() {
        System.out.println("===========================================");
        System.out.println("✅ ✅ ✅ STARTUP BEAN INITIALIZED ✅ ✅ ✅");
        System.out.println("===========================================");
    }
    
    public void onStart(@Observes @Initialized(ApplicationScoped.class) Object init) {
        System.out.println("===========================================");
        System.out.println("🎯 🎯 🎯 APPLICATION STARTED EVENT 🎯 🎯 🎯");
        System.out.println("===========================================");
    }
}