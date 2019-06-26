package tech.jhipster.operator.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.jhipster.operator.core.K8SCoreRuntime;
import tech.jhipster.operator.crds.app.Application;
import tech.jhipster.operator.crds.app.ApplicationSpec;
import tech.jhipster.operator.crds.app.CustomService;
import tech.jhipster.operator.crds.app.MicroServiceDescr;
import tech.jhipster.operator.crds.gateway.Gateway;
import tech.jhipster.operator.crds.registry.Registry;
import tech.jhipster.operator.jdl.JDLParser;
import tech.jhipster.operator.jdl.JHipsterApplicationDefinition;
import tech.jhipster.operator.jdl.JHipsterModuleDefinition;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AppService {
    private Logger logger = LoggerFactory.getLogger(AppService.class);
    private Map<String, Application> apps = new ConcurrentHashMap<>();
    private Map<String, String> appsUrls = new HashMap<>();

    @Autowired
    private K8SCoreRuntime k8SCoreRuntime;

    /*
     * Add the logic to define what are the rules for your application to be UP or DOWN
     * 1) We need to get the microservices from the app spec
     * 2) we need to make sure that the microservice in the app def is in the app spec
     *   2.1) we need to check that the microservice resource exist
     *   2.2) for each microservice I need to check with k8sCoreRuntime that the service is available
     * 3) if microservice type gateway, check for gateway in the spec
     * 4) if microservice type registry, check for registry in the spec
     */
    public boolean isAppHealthy(Application app) {
        // We compare the desired state -> AppDefinition to JHipster K8s Native CRDs
        JHipsterApplicationDefinition appDefinition = app.getSpec().getAppDefinition();
        boolean isGatewayAvailable = false;
        boolean isRegistryAvailable = false;
        boolean microServicesAvailable[] = new boolean[app.getSpec().getMicroservices().size()];
        int microServicesCount = 0;
        //
        Set<MicroServiceDescr> microservices = app.getSpec().getMicroservices();
        for (JHipsterModuleDefinition mdd : appDefinition.getModules()) {
            if (JDLParser.fromJDLServiceToKind(mdd.getType()).equals("Gateway")) {
                String gateway = app.getSpec().getGateway();
                if (gateway != null && !gateway.isEmpty()) {
                    isGatewayAvailable = k8SCoreRuntime.isServiceAvailable(gateway);
                }
            }
            if (JDLParser.fromJDLServiceToKind(mdd.getType()).equals("MicroService")) {

                for (MicroServiceDescr md : microservices) {
                    // 1) check that the CRD Kind MicroService exist
                    if (md.getName().equals(mdd.getName()) && md.getKind().equals("MicroService")) {
                        // 2) check that the service referenced from the CRD exist
                        microServicesAvailable[microServicesCount] = k8SCoreRuntime.isServiceAvailable(md.getServiceName());
                        microServicesCount++;
                    }
                }

            }

        }

        // The registry is not a microservice in the app def so I need to check separately
        String registry = app.getSpec().getRegistry();
        if (registry != null && !registry.isEmpty()) {
            isRegistryAvailable = k8SCoreRuntime.isServiceAvailable(registry);
        }

        boolean areMicroServicesAvailable = checkMicroServicesAvailability(microservices.size(), microServicesAvailable);
        logger.info("MicroServices Available?: " + areMicroServicesAvailable);
        logger.info("Gateway Available?: " + isGatewayAvailable);
        logger.info("Registry Available?: " + isRegistryAvailable);
        if (areMicroServicesAvailable && isGatewayAvailable && isRegistryAvailable) {
            return true;
        }

        return false;
    }

    private boolean checkMicroServicesAvailability(int size, boolean[] microServicesAvailable) {
        if (microServicesAvailable.length == size && microServicesAvailable.length > 0) {
            for (boolean a : microServicesAvailable) {
                if (!a) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void addGatewayToApp(Gateway gateway) {
        String appName = gateway.getMetadata().getLabels().get("app");
        if (appName != null && !appName.isEmpty()) {
            Application application = apps.get(appName);
            if (application != null) {
                ApplicationSpec spec = application.getSpec();
                //If the APP already have the gateway then ignore, to avoid one API call
                if (spec.getGateway() != null && !spec.getGateway().isEmpty() && spec.getGateway().equals(gateway.getSpec().getServiceName())) {
                    return;
                }
                if (k8SCoreRuntime.isServiceAvailable(gateway.getSpec().getServiceName())) {
                    spec.setGateway(gateway.getSpec().getServiceName());
                    application.setSpec(spec);
                    apps.put(application.getMetadata().getName(), application);
                    logger.info("> Application: " + appName + " updated with Gateway " + gateway.getMetadata().getName());
                } else {
                    logger.error("Registry: " + gateway.getSpec().getServiceName() + " doesn't exist. ");
                }
            }
        } else {
            logger.error("> Orphan Service: " + gateway.getMetadata().getName());
        }
    }


    public void addRegistryToApp(Registry registry) {
        String appName = registry.getMetadata().getLabels().get("app");
        if (appName != null && !appName.isEmpty()) {
            Application application = apps.get(appName);
            if (application != null) {
                ApplicationSpec spec = application.getSpec();
                //If the APP already have the registry then ignore, to avoid one API call
                if (spec.getRegistry() != null && !spec.getRegistry().isEmpty() && spec.getRegistry().equals(registry.getSpec().getServiceName())) {
                    return;
                }
                if (k8SCoreRuntime.isServiceAvailable(registry.getSpec().getServiceName())) {
                    spec.setRegistry(registry.getSpec().getServiceName());
                    application.setSpec(spec);
                    apps.put(application.getMetadata().getName(), application);
                    logger.info("> Application: " + appName + " updated with Registry " + registry.getMetadata().getName());
                } else {
                    logger.error("Registry: " + registry.getSpec().getServiceName() + " doesn't exist. ");
                }
            }
        } else {
            logger.error("> Orphan Service: " + registry.getMetadata().getName());
        }
    }

    public void addMicroServiceToApp(CustomService microService) {
        String appName = microService.getMetadata().getLabels().get("app");
        if (appName != null && !appName.isEmpty()) {
            Application application = apps.get(appName);
            if (application != null) {
                ApplicationSpec spec = application.getSpec();
                Set<MicroServiceDescr> microservices = spec.getMicroservices();
                if (microservices == null) {
                    microservices = new HashSet<>();
                }
                if (k8SCoreRuntime.isServiceAvailable(microService.getSpec().getServiceName())) {
                    microservices.add(new MicroServiceDescr(microService.getMetadata().getName(), microService.getKind(), microService.getSpec().getServiceName()));
                    spec.setMicroservices(microservices);
                    application.setSpec(spec);
                    apps.put(application.getMetadata().getName(), application);
                    logger.info("> Application: " + appName + " updated with Service " + microService.getMetadata().getName());
                } else {
                    logger.error("Service: " + microService.getSpec().getServiceName() + " doesn't exist. ");
                }
            }
        } else {
            logger.error("> Orphan Service: " + microService.getMetadata().getName());
        }
    }

    public void removeGatewayFromApp(Gateway gateway) {
        String appName = gateway.getMetadata().getLabels().get("app");
        if (appName != null && !appName.isEmpty()) {
            Application application = apps.get(appName);
            if (application != null) {
                ApplicationSpec spec = application.getSpec();
                spec.setGateway("");
                application.setSpec(spec);
                apps.put(application.getMetadata().getName(), application);
                logger.info(">> Gateway removed " + gateway.getMetadata().getName() + " from app " + appName);
            }
        }
    }


    public void removeRegistryFromApp(Registry service) {
        String appName = service.getMetadata().getLabels().get("app");
        if (appName != null && !appName.isEmpty()) {
            Application application = apps.get(appName);
            if (application != null) {
                ApplicationSpec spec = application.getSpec();
                spec.setRegistry("");
                application.setSpec(spec);
                apps.put(application.getMetadata().getName(), application);
                logger.info(">> Registry removed " + service.getMetadata().getName() + " from app " + appName);
            }
        }
    }

    public void removeMicroServiceFromApp(CustomService service) {
        String appName = service.getMetadata().getLabels().get("app");
        if (appName != null && !appName.isEmpty()) {
            Application application = apps.get(appName);
            if (application != null) {
                ApplicationSpec spec = application.getSpec();
                Set<MicroServiceDescr> microservices = spec.getMicroservices();
                if (microservices == null) {
                    microservices = new HashSet<>();
                }
                microservices.removeIf(m -> m.getKind().equals(service.getKind()) && m.getName().equals(service.getMetadata().getName()));
                spec.setMicroservices(microservices);
                application.setSpec(spec);
                apps.put(application.getMetadata().getName(), application);
                logger.info(">> Deleted MicroService " + service.getMetadata().getName() + " from app " + appName);
            }
        }
    }


    public List<String> getApps() {
        return apps.values().stream()
                .filter(app -> isAppHealthy(app))
                .map(a -> a.getMetadata().getName())
                .collect(Collectors.toList());
    }

    public void addApp(String appName, Application app) {
        apps.put(appName, app);
    }

    public Application removeApp(String appName) {
        return apps.remove(appName);
    }

    public Application getApp(String appName) {
        return apps.get(appName);
    }

    public String getAppUrl(String appName) {
        return appsUrls.get(appName);
    }

    public void addAppUrl(String appName, String url) {
        appsUrls.put(appName, url);
    }

    public Map<String, Application> getAppsMap() {
        return apps;
    }

    public Map<String, String> getAppsUrls() {
        return appsUrls;
    }
}
