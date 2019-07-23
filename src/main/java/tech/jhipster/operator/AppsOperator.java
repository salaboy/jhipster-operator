package tech.jhipster.operator;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.jhipster.operator.app.AppCRDs;
import tech.jhipster.operator.app.AppService;
import tech.jhipster.operator.core.K8SCoreRuntime;
import tech.jhipster.operator.crds.app.*;
import tech.jhipster.operator.crds.gateway.DoneableGateway;
import tech.jhipster.operator.crds.gateway.Gateway;
import tech.jhipster.operator.crds.gateway.GatewayList;
import tech.jhipster.operator.crds.microservice.DoneableMicroService;
import tech.jhipster.operator.crds.microservice.MicroService;
import tech.jhipster.operator.crds.microservice.MicroServiceList;
import tech.jhipster.operator.crds.registry.DoneableRegistry;
import tech.jhipster.operator.crds.registry.Registry;
import tech.jhipster.operator.crds.registry.RegistryList;

import java.util.List;
import java.util.Set;

@Service
public class AppsOperator {

    // Is the service On?
    private boolean on = true;
    private boolean initDone = false;
    private boolean crdsFound = false;

    private Logger logger = LoggerFactory.getLogger(AppsOperator.class);
    private CustomResourceDefinition microServiceCRD = null;
    private CustomResourceDefinition gatewayCRD = null;
    private CustomResourceDefinition registryCRD = null;
    private CustomResourceDefinition applicationCRD = null;
    private boolean applicationWatchRegistered = false;
    private String appsResourceVersion;
    private String microServicesResourceVersion;
    private String registriesResourceVersion;
    private String gatewaysResourceVersion;

    private NonNamespaceOperation<Application, ApplicationList, DoneableApplication, Resource<Application, DoneableApplication>> appCRDClient;
    private NonNamespaceOperation<MicroService, MicroServiceList, DoneableMicroService, Resource<MicroService, DoneableMicroService>> microServicesCRDClient;
    private NonNamespaceOperation<Gateway, GatewayList, DoneableGateway, Resource<Gateway, DoneableGateway>> gatewaysCRDClient;
    private NonNamespaceOperation<Registry, RegistryList, DoneableRegistry, Resource<Registry, DoneableRegistry>> registriesCRDClient;


    @Autowired
    private AppService appService;

    @Autowired
    private JHipsterOperatorConfiguration config;

    @Autowired
    private K8SCoreRuntime k8SCoreRuntime;

    public void bootstrap() {
        crdsFound = areRequiredCRDsPresent();
        if (crdsFound) {
            initDone = init();
        }
    }

    /*
     * Check for Required CRDs
     */
    private boolean areRequiredCRDsPresent() {
        try {
            appService.registerCustomResourcesForRuntime();

            CustomResourceDefinitionList crds = k8SCoreRuntime.getCustomResourceDefinitionList();
            for (CustomResourceDefinition crd : crds.getItems()) {
                ObjectMeta metadata = crd.getMetadata();
                if (metadata != null) {
                    String name = metadata.getName();
                    if (AppCRDs.MICROSERVICE_CRD_NAME.equals(name)) {
                        microServiceCRD = crd;
                    }
                    if (AppCRDs.GATEWAY_CRD_NAME.equals(name)) {
                        gatewayCRD = crd;
                    }
                    if (AppCRDs.REGISTRY_CRD_NAME.equals(name)) {
                        registryCRD = crd;
                    }
                    if (AppCRDs.APP_CRD_NAME.equals(name)) {
                        applicationCRD = crd;
                    }
                }
            }
            if (allCRDsFound()) {
                logger.info("\t > App CRD: " + applicationCRD.getMetadata().getName());
                logger.info("\t > MicroService CRD: " + microServiceCRD.getMetadata().getName());
                logger.info("\t > Registry CRD: " + registryCRD.getMetadata().getName());
                logger.info("\t > Gateway CRD: " + gatewayCRD.getMetadata().getName());
                return true;
            } else {
                logger.error("> Custom CRDs required to work not found please check your installation!");
                logger.error("\t > App CRD: " + ((applicationCRD == null) ? " NOT FOUND " : applicationCRD.getMetadata().getName()));
                logger.error("\t > MicroService CRD: " + ((microServiceCRD == null) ? " NOT FOUND " : microServiceCRD.getMetadata().getName()));
                logger.error("\t > Registry CRD: " + ((registryCRD == null) ? " NOT FOUND " : registryCRD.getMetadata().getName()));
                logger.error("\t > Gateway CRD: " + ((gatewayCRD == null) ? " NOT FOUND " : gatewayCRD.getMetadata().getName()));
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("> Init sequence not done");
        }
        return false;
    }

    /*
     * Init can only be called if all the required CRDs are present
     *  - It creates the CRD clients to be able to watch and execute operations
     *  - It loads the existing resources (current state in the cluster)
     *  - It register the watches for our CRDs
     */
    private boolean init() {
        logger.info("> JHipster K8s Operator is Starting!");
        // Creating CRDs Clients
        appCRDClient = k8SCoreRuntime.customResourcesClient(applicationCRD, Application.class, ApplicationList.class, DoneableApplication.class).inNamespace(k8SCoreRuntime.getNamespace());
        microServicesCRDClient = k8SCoreRuntime.customResourcesClient(microServiceCRD, MicroService.class, MicroServiceList.class, DoneableMicroService.class).inNamespace(k8SCoreRuntime.getNamespace());
        gatewaysCRDClient = k8SCoreRuntime.customResourcesClient(gatewayCRD, Gateway.class, GatewayList.class, DoneableGateway.class).inNamespace(k8SCoreRuntime.getNamespace());
        registriesCRDClient = k8SCoreRuntime.customResourcesClient(registryCRD, Registry.class, RegistryList.class, DoneableRegistry.class).inNamespace(k8SCoreRuntime.getNamespace());

        if (loadExistingResources() && watchOurCRDs()) {
            return true;
        }

        return false;

    }

    /*
     * Check that all the CRDs are found for this operator to work
     */
    private boolean allCRDsFound() {
        if (microServiceCRD == null || applicationCRD == null || gatewayCRD == null || registryCRD == null) {
            return false;
        }
        return true;
    }

    /*
     * Watch our CRDs
     *  Register watches if they were not registered yet
     */
    private boolean watchOurCRDs() {
        // Watch for our CRDs
        if (!applicationWatchRegistered) {
            registerApplicationWatch();
        }
        if (applicationWatchRegistered) {
            logger.info("> All CRDs Found, init complete");
            return true;
        } else {
            logger.error("> CRDs missing, check your installation and run init again");
            return false;
        }
    }

    /*
     * Load existing instances of our CRDs
     *  - This checks the existing resources and make sure that they are loaded correctly
     *  - This also performs the binding of a service to its app
     */
    private boolean loadExistingResources() {
        // Load Existing Applications
        List<Application> applicationList = appCRDClient.list().getItems();
        if (!applicationList.isEmpty()) {
            appsResourceVersion = applicationList.get(0).getMetadata().getResourceVersion();
            logger.info(">> Applications Resource Version: " + appsResourceVersion);
            applicationList.forEach(app -> {
                appService.addApp(app.getMetadata().getName(), app);
                logger.info("> App " + app.getMetadata().getName() + " found.");
            });

        }
        // Load Existing Service As
        List<MicroService> microServiceList = microServicesCRDClient.list().getItems();
        if (!microServiceList.isEmpty()) {
            microServicesResourceVersion = microServiceList.get(0).getMetadata().getResourceVersion();
            logger.info(">> MicroService Resource Version: " + microServicesResourceVersion);
            microServiceList.forEach(microService -> {
                // If it doesn't have owner references we need to set it up at load time
                MicroService updatedMicroService = checkAndAddOwnerReferences(microService);
                microServicesCRDClient.createOrReplace(updatedMicroService);
            });
        }
        // Load Existing Gateways
        List<Gateway> gatewayList = gatewaysCRDClient.list().getItems();
        if (!gatewayList.isEmpty()) {
            gatewaysResourceVersion = gatewayList.get(0).getMetadata().getResourceVersion();
            logger.info(">> Gateway Resource Version: " + gatewaysResourceVersion);
            gatewayList.forEach(gateway -> {
                // If it doesn't have owner references we need to set it up at load time
                Gateway updatedGateway = checkAndAddOwnerReferences(gateway);
                gatewaysCRDClient.createOrReplace(updatedGateway);

            });

        }
        // Load Existing Registries
        List<Registry> registriesList = registriesCRDClient.list().getItems();
        if (!registriesList.isEmpty()) {
            registriesResourceVersion = registriesList.get(0).getMetadata().getResourceVersion();
            logger.info(">> Registry Resource Version: " + registriesResourceVersion);
            registriesList.forEach(registry -> {
                // If it doesn't have owner references we need to set it up at load time
                Registry updatedRegistry = checkAndAddOwnerReferences(registry);
                registriesCRDClient.createOrReplace(updatedRegistry);
            });

        }
        return true;
    }


    private <T extends CustomService> T checkAndAddOwnerReferences(T service) {
        if (service.getMetadata().getOwnerReferences() != null) {
            String appName = service.getMetadata().getLabels().get("app");
            if (appName != null && !appName.isEmpty()) {
                Application application = appService.getApp(appName);
                return appService.owns(application, service);
            }
        }
        return service;
    }


    /*
     * Register an Application Resource Watch
     *  - This watch is in charge of adding and removing apps to/from the In memory desired state
     */
    private void registerApplicationWatch() {
        logger.info("> Registering Application CRD Watch");
        appCRDClient.withResourceVersion(appsResourceVersion).watch(new Watcher<Application>() {
            @Override
            public void eventReceived(Watcher.Action action, Application application) {
                if (action.equals(Action.ADDED)) {
                    logger.info(">> Adding App: " + application.getMetadata().getName());
                    appService.addApp(application.getMetadata().getName(), application);

                    linkAllApplicationResources(application);

                }
                if (action.equals(Action.DELETED)) {
                    logger.info(">> Deleting App: " + application.getMetadata().getName());
                    appService.removeApp(application.getMetadata().getName());
                }
                if (action.equals(Action.MODIFIED)) {
                    logger.info(">> Modifying App: " + application.getMetadata().getName());
                    appService.addApp(application.getMetadata().getName(), application);
                    linkAllApplicationResources(application);
                }

                if (application.getSpec() == null) {
                    logger.info("No Spec for resource " + application.getMetadata().getName());
                }
            }

            @Override
            public void onClose(KubernetesClientException cause) {
            }
        });
        applicationWatchRegistered = true;

    }

    private void linkAllApplicationResources(Application application) {
        linkMicroServicesToApp(application);

        linkRegistryToApp(application);

        linkGatewayToApp(application);
    }

    private void linkGatewayToApp(Application application) {
        String gatewayName = application.getSpec().getGateway();
        Gateway gateway = gatewaysCRDClient.withName(gatewayName).get();
        if (gateway != null) {
            if (gateway.getMetadata().getLabels().get("app") != null &&
                    gateway.getMetadata().getLabels().get("app").equals(application.getMetadata().getName())) {
                // This just set the Application as the Owner of the Gateway
                Gateway updatedGateway = appService.owns(application, gateway);
                gatewaysCRDClient.createOrReplace(updatedGateway);
            } else {
                logger.info("This gateway (" + gateway + ") belongs to a different application"
                        + gateway.getMetadata().getLabels().get("app"));
            }
        } else {
            logger.error("Gateway: " + gatewayName + " doesn't exist!");
        }
    }

    private void linkRegistryToApp(Application application) {
        String registryName = application.getSpec().getRegistry();
        Registry registry = registriesCRDClient.withName(registryName).get();
        if (registry != null) {
            if (registry.getMetadata().getLabels().get("app") != null &&
                    registry.getMetadata().getLabels().get("app").equals(application.getMetadata().getName())) {
                // This just set the Application as the Owner of the Registry
                Registry updatedRegistry = appService.owns(application, registry);
                registriesCRDClient.createOrReplace(updatedRegistry);
            } else {
                logger.info("This registry (" + registry + ") belongs to a different application"
                        + registry.getMetadata().getLabels().get("app"));
            }
        } else {
            logger.error("Registry: " + registryName + " doesn't exist!");
        }

    }

    private void linkMicroServicesToApp(Application application) {
        Set<MicroServiceDescr> microservices = application.getSpec().getMicroservices();
        if (microservices != null && !microservices.isEmpty()) {
            for (MicroServiceDescr msd : microservices) {
                MicroService microService = microServicesCRDClient.withName(msd.getName()).get();
                if (microService != null) {
                    if (microService.getMetadata().getLabels().get("app") != null &&
                            microService.getMetadata().getLabels().get("app").equals(application.getMetadata().getName())) {
                        // This just set the Application as the Owner of the MicroService
                        MicroService updatedMicroService = appService.owns(application, microService);
                        microServicesCRDClient.createOrReplace(updatedMicroService);
                    } else {
                        logger.debug("This microservice (" + microService + ") belongs to a different application"
                                + microService.getMetadata().getLabels().get("app"));
                    }
                } else {
                    logger.error("MicroService: " + msd.getName() + " doesn't exist!");
                }

            }
        }
    }


    /*
     * Reconcile contains the logic that understand how services relates to applications and the application state
     *   matches the desired state with current state in K8s
     */
    public void reconcile() {
        if (appService.getApps().isEmpty()) {
            logger.info("> No Apps found.");
        }
        // For each App Desired State
        appService.getAppsMap().keySet().forEach(appName ->
                {
                    Application app = appService.getApp(appName);
                    linkAllApplicationResources(app);
                    logger.info("> App Found: " + appName + ". Scanning ...");
                    // Is App Structure ok
                    if (isApplicationStructureOK(app)) {

                        // Is the APP Healthy??
                        boolean appHealthy = false;
                        if (config.isK8sServiceCheckEnabled()) {
                            appHealthy = appService.isAppHealthy(app, true);
                        } else {
                            // If we have K8s services disabled and the structure is ok we will set it as healthy
                            appHealthy = true;
                        }
                        if (appHealthy) {
                            // YES: Change the state and provide a URL
                            app.getSpec().getMicroservices().forEach(m -> logger.info("\t> MicroService found: " + m));
                            app.getSpec().setStatus("HEALTHY");
                            String url = appService.createAndSetAppURL(app.getMetadata().getName(), app.getSpec().getVersion());
                            app.getSpec().setUrl(url);
                            logger.info("\t> App: " + appName + ", status:  HEALTHY, URL: " + url + " \n");
                        } else {
                            // NO: Change the state and remove the URL
                            logger.error("\t > App Name: " + appName + " is down due missing services");
                            if (app.getSpec().getMicroservices() == null || app.getSpec().getMicroservices().isEmpty()) {
                                logger.info("\t>App: " + appName + ": No MicroService found. ");
                            } else {
                                app.getSpec().getMicroservices().forEach(m -> logger.info("\t> MicroService found: " + m));
                            }
                            app.getSpec().setStatus("UNHEALTHY");
                            app.getSpec().setUrl("N/A");
                            logger.info("\t> App: " + appName + ", status: UNHEALTHY. \n ");
                        }
                        // Notify K8s about the updates required
                        appCRDClient.createOrReplace(app);
                    } else {
                        logger.error("The application " + app.getMetadata().getName() + " structure is not complete please check the resources required by this application");
                    }
                }
        );

    }

    private boolean isApplicationStructureOK(Application application) {

        boolean isRegistryDefAvailable = false;
        boolean isGatewayDefAvailable = false;
        boolean areMicroServicesAvailable = false;

        Set<MicroServiceDescr> microservices = application.getSpec().getMicroservices();
        if (microservices != null && !microservices.isEmpty()) {
            boolean areMicroServicesDefAvailable[] = new boolean[microservices.size()];
            int microservicesCount = 0;
            for (MicroServiceDescr msd : microservices) {
                MicroService microService = microServicesCRDClient.withName(msd.getName()).get();
                if (microService != null) {
                    areMicroServicesDefAvailable[microservicesCount] = true;
                } else {
                    logger.info("MicroService " + msd.getName() + " not found!");
                }
                microservicesCount++;
            }
            areMicroServicesAvailable = appService.checkMicroServicesAvailability(microservices.size(), areMicroServicesDefAvailable);
        }

        String registryName = application.getSpec().getRegistry();
        Registry registry = registriesCRDClient.withName(registryName).get();
        if (registry != null) {
            isRegistryDefAvailable = true;
        } else {
            logger.info("Registry " + registryName + " not found!");
        }

        String gatewayName = application.getSpec().getGateway();
        Gateway gateway = gatewaysCRDClient.withName(gatewayName).get();
        if (gateway != null) {
            isGatewayDefAvailable = true;
        } else {
            logger.info("Gateway " + gatewayName + " not found!");
        }

        if (areMicroServicesAvailable && isGatewayDefAvailable && isRegistryDefAvailable) {
            return true;
        }


        return false;
    }


    /*
     * Delete a JHipster Application by name
     */
    public void deleteApp(String appName) {
        Application app = appService.getApp(appName);
        //@TODO: delete by API doesn't cascade yet..
        appCRDClient.delete(app);
    }

    public CustomResourceDefinition getMicroServiceCRD() {
        return microServiceCRD;
    }

    public CustomResourceDefinition getGatewayCRD() {
        return gatewayCRD;
    }

    public CustomResourceDefinition getRegistryCRD() {
        return registryCRD;
    }

    public CustomResourceDefinition getApplicationCRD() {
        return applicationCRD;
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

    public boolean isInitDone() {
        return initDone;
    }


}
