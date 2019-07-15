package tech.jhipster.operator.app;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
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
import tech.jhipster.operator.crds.microservice.MicroService;
import tech.jhipster.operator.crds.registry.Registry;

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
     * 4) if registry is not in the app definition so we need to check separately
     */
    public boolean isAppHealthy(Application app, boolean log) {
        // We compare the desired state -> AppDefinition to JHipster K8s Native CRDs

        boolean isGatewayAvailable = true; //@TODO: need to implement the validation of structure
        boolean isRegistryAvailable = true; //@TODO: need to implement the validation of structure
        boolean microServicesAvailable[] = new boolean[app.getSpec().getMicroservices().size()];
        int microServicesCount = 0;
        //
        Set<MicroServiceDescr> microservices = app.getSpec().getMicroservices();


        // The registry is not a microservice in the app def so I need to check separately
        String registry = app.getSpec().getRegistry();
        if (registry != null && !registry.isEmpty()) {
            isRegistryAvailable = k8SCoreRuntime.isServiceAvailable(registry);
        }

        boolean areMicroServicesAvailable = checkMicroServicesAvailability(microservices.size(), microServicesAvailable);
        if (log) {
            logger.info("\t> Checking app health: " + app.getMetadata().getName());
            logger.info("\t\t> MicroServices Available?: " + areMicroServicesAvailable);
            logger.info("\t\t> Gateway Available?: " + isGatewayAvailable);
            logger.info("\t\t> Registry Available?: " + isRegistryAvailable);
        }
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

    /*
     * Add Gateway to Application and set up the owner references
     * @return the modified Gateway resource
     */
    public Gateway addGatewayToApp(Gateway gateway) {
        String appName = gateway.getMetadata().getLabels().get("app");
        if (appName != null && !appName.isEmpty()) {
            Application application = apps.get(appName);
            if (application != null) {
                //Set OwnerReferences: the Application Owns the Gateway
                List<OwnerReference> ownerReferencesFromApp = createOwnerReferencesFromApp(application);
                ObjectMeta objectMetaRegistry = new ObjectMeta();
                objectMetaRegistry.setOwnerReferences(ownerReferencesFromApp);
                gateway.setMetadata(objectMetaRegistry);

                ApplicationSpec spec = application.getSpec();
                //If the APP already have the gateway then ignore, to avoid one API call
                if (spec.getGateway() != null && !spec.getGateway().isEmpty() && spec.getGateway().equals(gateway.getSpec().getServiceName())) {
                    return gateway;
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
            logger.error("> Orphan Service: " + gateway.getMetadata().getName() + ", it reference this app that doesn't exist: " + gateway.getMetadata().getLabels().get("app"));
        }
        return gateway;
    }

    /*
     * Add Registry to Application and set up the owner references
     * @return the modified Registry resource
     */
    public Registry addRegistryToApp(Registry registry) {
        String appName = registry.getMetadata().getLabels().get("app");
        if (appName != null && !appName.isEmpty()) {
            Application application = apps.get(appName);
            if (application != null) {
                //Set OwnerReferences: the Application Owns the Registry
                List<OwnerReference> ownerReferencesFromApp = createOwnerReferencesFromApp(application);
                ObjectMeta objectMetaRegistry = new ObjectMeta();
                objectMetaRegistry.setOwnerReferences(ownerReferencesFromApp);
                registry.setMetadata(objectMetaRegistry);

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
            logger.error("> Orphan Service: " + registry.getMetadata().getName() + ", it reference this app that doesn't exist: " + registry.getMetadata().getLabels().get("app"));
        }
        return registry;
    }

    /*
     * Add MicroService to Application and set up the owner references
     * @return the modified MicroService resource
     */
    public MicroService addMicroServiceToApp(MicroService microService) {
        String appName = microService.getMetadata().getLabels().get("app");
        if (appName != null && !appName.isEmpty()) {
            Application application = apps.get(appName);
            if (application != null) {
                //Set OwnerReferences: the Application Owns the MicroService
                List<OwnerReference> ownerReferencesFromApp = createOwnerReferencesFromApp(application);
                ObjectMeta objectMetaMicroService = new ObjectMeta();
                objectMetaMicroService.setOwnerReferences(ownerReferencesFromApp);
                microService.setMetadata(objectMetaMicroService);

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
            logger.error("> Orphan Service: " + microService.getMetadata().getName() + ", it reference this app that doesn't exist: " + microService.getMetadata().getLabels().get("app"));
        }
        return microService;
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


    /*
     * Create owner references for modules of an application
     */
    private List<OwnerReference> createOwnerReferencesFromApp(Application app) {
        if (app.getMetadata().getUid() == null || app.getMetadata().getUid().isEmpty()) {
            throw new IllegalStateException("The app needs to be saved first, the UUID needs to be present.");
        }
        OwnerReference ownerReference = new OwnerReference();
        ownerReference.setUid(app.getMetadata().getUid());
        ownerReference.setName(app.getMetadata().getName());
        ownerReference.setKind(app.getKind());
        ownerReference.setController(true);
        ownerReference.setBlockOwnerDeletion(true);
        ownerReference.setApiVersion(app.getApiVersion());

        return Arrays.asList(ownerReference);

    }

    public List<String> getApps() {
        return apps.values().stream()
                .filter(app -> isAppHealthy(app, false))
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
