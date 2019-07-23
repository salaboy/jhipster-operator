package tech.jhipster.operator.app;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.jhipster.operator.JHipsterOperatorConfiguration;
import tech.jhipster.operator.core.K8SCoreRuntime;
import tech.jhipster.operator.crds.app.Application;
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

    @Autowired
    private JHipsterOperatorConfiguration config;

    /*
     * Add the logic to define what are the rules for your application to be UP or DOWN
     */
    public boolean isAppHealthy(Application app, boolean log) {

        String gateway = app.getSpec().getGateway();
        if (!k8SCoreRuntime.isServiceAvailable(gateway)) {
            logger.error("Service: " + gateway + " doesn't exist. ");
            return false;
        }
        String registry = app.getSpec().getRegistry();
        if (!k8SCoreRuntime.isServiceAvailable(registry)) {
            logger.error("Service: " + registry + " doesn't exist. ");
            return false;
        }
        Set<MicroServiceDescr> microservices = app.getSpec().getMicroservices();
        for (MicroServiceDescr microServiceDescr : microservices) {
            if (!k8SCoreRuntime.isServiceAvailable(microServiceDescr.getServiceName())) {
                logger.error("Service: " + microServiceDescr.getServiceName() + " doesn't exist. ");
                return false;
            }
        }


        return true;
    }

    public boolean checkMicroServicesAvailability(int size, boolean[] microServicesAvailable) {
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
     * Add CustomService to Application and set up the owner references
     * @return the modified CustomService resource
     */
    public <T extends CustomService> T owns(Application application, T service) {

        if (application != null) {
            //Set OwnerReferences: the Application Owns the MicroService
            List<OwnerReference> ownerReferencesFromApp = createOwnerReferencesFromApp(application);
            ObjectMeta objectMetaMicroService = service.getMetadata();
            objectMetaMicroService.setOwnerReferences(ownerReferencesFromApp);
            service.setMetadata(objectMetaMicroService);


        }

        return service;
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

    public String createAndSetAppURL(String appName, String appVersion) {
        String externalIp = k8SCoreRuntime.findExternalIP();
        String url = "http://" + externalIp + "/apps/" + appName + "/" + appVersion + "/";
        appsUrls.put(appName, url);
        return url;
    }

    public Map<String, Application> getAppsMap() {
        return apps;
    }

    public Map<String, String> getAppsUrls() {
        return appsUrls;
    }

    public void registerCustomResourcesForRuntime() {
        k8SCoreRuntime.registerCustomKind(AppCRDs.APP_CRD_GROUP + "/v1", "MicroService", MicroService.class);
        k8SCoreRuntime.registerCustomKind(AppCRDs.APP_CRD_GROUP + "/v1", "Gateway", Gateway.class);
        k8SCoreRuntime.registerCustomKind(AppCRDs.APP_CRD_GROUP + "/v1", "Registry", Registry.class);
        k8SCoreRuntime.registerCustomKind(AppCRDs.APP_CRD_GROUP + "/v1", "Application", Application.class);
    }
}
