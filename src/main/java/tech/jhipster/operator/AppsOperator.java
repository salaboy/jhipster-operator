package tech.jhipster.operator;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import tech.jhipster.operator.app.AppService;
import tech.jhipster.operator.core.K8SCoreRuntime;
import tech.jhipster.operator.crds.app.Application;
import tech.jhipster.operator.crds.app.ApplicationList;
import tech.jhipster.operator.crds.app.DoneableApplication;
import tech.jhipster.operator.crds.gateway.DoneableGateway;
import tech.jhipster.operator.crds.gateway.Gateway;
import tech.jhipster.operator.crds.gateway.GatewayList;
import tech.jhipster.operator.crds.module.DoneableModule;
import tech.jhipster.operator.crds.module.Module;
import tech.jhipster.operator.crds.module.ModuleList;
import tech.jhipster.operator.crds.registry.DoneableRegistry;
import tech.jhipster.operator.crds.registry.Registry;
import tech.jhipster.operator.crds.registry.RegistryList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.jhipster.operator.app.AppCRDs;

import java.util.List;

@Service
public class AppsOperator {

    private Logger logger = LoggerFactory.getLogger(AppsOperator.class);
    private CustomResourceDefinition moduleCRD = null;
    private CustomResourceDefinition gatewayCRD = null;
    private CustomResourceDefinition registryCRD = null;
    private CustomResourceDefinition applicationCRD = null;
    private boolean moduleWatchRegistered = false;
    private boolean gatewayWatchRegistered = false;
    private boolean registryWatchRegistered = false;
    private boolean applicationWatchRegistered = false;

    private String appsResourceVersion;
    private String modulesResourceVersion;
    private String registriesResourceVersion;
    private String gatewaysResourceVersion;

    private NonNamespaceOperation<Application, ApplicationList, DoneableApplication, Resource<Application, DoneableApplication>> appCRDClient;
    private NonNamespaceOperation<Module, ModuleList, DoneableModule, Resource<Module, DoneableModule>> modulesCRDClient;
    private NonNamespaceOperation<Gateway, GatewayList, DoneableGateway, Resource<Gateway, DoneableGateway>> gatewaysCRDClient;
    private NonNamespaceOperation<Registry, RegistryList, DoneableRegistry, Resource<Registry, DoneableRegistry>> registriesCRDClient;


    @Autowired
    private AppService appService;

    @Autowired
    private K8SCoreRuntime k8SCoreRuntime;

    /*
     * Check for Required CRDs
     */
    public boolean areRequiredCRDsPresent() {
        try {

            k8SCoreRuntime.registerCustomKind(AppCRDs.APP_CRD_GROUP + "/v1", "Module", Module.class);
            k8SCoreRuntime.registerCustomKind(AppCRDs.APP_CRD_GROUP + "/v1", "Gateway", Gateway.class);
            k8SCoreRuntime.registerCustomKind(AppCRDs.APP_CRD_GROUP + "/v1", "Registry", Registry.class);
            k8SCoreRuntime.registerCustomKind(AppCRDs.APP_CRD_GROUP + "/v1", "Application", Application.class);

            CustomResourceDefinitionList crds = k8SCoreRuntime.getCustomResourceDefinitionList();
            for (CustomResourceDefinition crd : crds.getItems()) {
                ObjectMeta metadata = crd.getMetadata();
                if (metadata != null) {
                    String name = metadata.getName();
                    if (AppCRDs.MODULE_CRD_NAME.equals(name)) {
                        moduleCRD = crd;
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
                return true;
            } else {
                logger.error("> Custom CRDs required to work not found please check your installation!");
                logger.error("\t > App CRD: " + ((applicationCRD == null) ? " NOT FOUND " : applicationCRD.getMetadata().getName()));
                logger.error("\t > Module CRD: " + ((moduleCRD == null) ? " NOT FOUND " : moduleCRD.getMetadata().getName()));
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
    public boolean init() {
        // Creating CRDs Clients
        appCRDClient = k8SCoreRuntime.customResourcesClient(applicationCRD, Application.class, ApplicationList.class, DoneableApplication.class);
        modulesCRDClient = k8SCoreRuntime.customResourcesClient(moduleCRD, Module.class, ModuleList.class, DoneableModule.class);
        gatewaysCRDClient = k8SCoreRuntime.customResourcesClient(gatewayCRD, Gateway.class, GatewayList.class, DoneableGateway.class);
        registriesCRDClient = k8SCoreRuntime.customResourcesClient(registryCRD, Registry.class, RegistryList.class, DoneableRegistry.class);

        if (loadExistingResources() && watchOurCRDs()) {
            return true;
        }

        return false;

    }

    /*
     * Check that all the CRDs are found for this operator to work
     */
    private boolean allCRDsFound() {
        if (moduleCRD == null || applicationCRD == null || gatewayCRD == null || registryCRD == null) {
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
        if (!moduleWatchRegistered) {
            registerModuleWatch();
        }
        if (!registryWatchRegistered) {
            registerRegistryWatch();
        }
        if (!gatewayWatchRegistered) {
            registerGatewayWatch();
        }
        if (!applicationWatchRegistered) {
            registerApplicationWatch();
        }
        if (areAllCRDWatchesRegistered()) {
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
        List<Module> moduleList = modulesCRDClient.list().getItems();
        if (!moduleList.isEmpty()) {
            modulesResourceVersion = moduleList.get(0).getMetadata().getResourceVersion();
            logger.info(">> Module Resource Version: " + modulesResourceVersion);
            moduleList.forEach(module -> {
                appService.addModuleToApp(module);
            });
        }
        // Load Existing Gateways
        List<Gateway> gatewayList = gatewaysCRDClient.list().getItems();
        if (!gatewayList.isEmpty()) {
            gatewaysResourceVersion = gatewayList.get(0).getMetadata().getResourceVersion();
            logger.info(">> Gateway Resource Version: " + gatewaysResourceVersion);
            gatewayList.forEach(gateway -> {
                appService.addModuleToApp(gateway);
            });

        }
        // Load Existing Registries
        List<Registry> registriesList = registriesCRDClient.list().getItems();
        if (!registriesList.isEmpty()) {
            registriesResourceVersion = registriesList.get(0).getMetadata().getResourceVersion();
            logger.info(">> Registry Resource Version: " + registriesResourceVersion);
            registriesList.forEach(registry -> {
                appService.addModuleToApp(registry);
            });

        }
        return true;
    }


    /*
     * Check that all the CRDs are being watched for changes
     */
    private boolean areAllCRDWatchesRegistered() {
        if (applicationWatchRegistered && moduleWatchRegistered && gatewayWatchRegistered && registryWatchRegistered) {
            return true;
        }
        return false;
    }

    /*
     * Register Application Watch
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
                    List<Module> moduleForAppList = modulesCRDClient.withLabel("app", application.getMetadata().getName()).list().getItems();
                    if (moduleForAppList != null && !moduleForAppList.isEmpty()) {
                        moduleForAppList.forEach(module -> {
                            appService.addModuleToApp(module);
                        });
                    }
                    List<Gateway> gatewayForAppList = gatewaysCRDClient.withLabel("app", application.getMetadata().getName()).list().getItems();
                    if (gatewayForAppList != null && !gatewayForAppList.isEmpty()) {
                        gatewayForAppList.forEach(gateway -> {
                            appService.addModuleToApp(gateway);
                        });
                    }
                    List<Registry> registryForAppList = registriesCRDClient.withLabel("app", application.getMetadata().getName()).list().getItems();
                    if (registryForAppList != null && !registryForAppList.isEmpty()) {
                        registryForAppList.forEach(registry -> {
                            appService.addModuleToApp(registry);
                        });
                    }
                }
                if (action.equals(Action.DELETED)) {
                    logger.info(">> Deleting App: " + application.getMetadata().getName());
                    appService.removeApp(application.getMetadata().getName());
                    //application.getMetadata().setOwnerReferences();
                    //@TODO: when creating services add OWNERReferences for GC
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

    /*
     * Register Registry Watch
     */

    private void registerModuleWatch() {
        logger.info("> Registering Service A CRD Watch");
        modulesCRDClient.withResourceVersion(modulesResourceVersion).watch(new Watcher<Module>() {
            @Override
            public void eventReceived(Watcher.Action action, Module module) {
                if (action.equals(Action.ADDED)) {
                    appService.addModuleToApp(module);
                }
                if (action.equals(Action.DELETED)) {
                    appService.removeServiceFromApp(module);
                }
                if (module.getSpec() == null) {
                    logger.error("No Spec for resource " + module);
                }
            }

            @Override
            public void onClose(KubernetesClientException cause) {
            }
        });
        moduleWatchRegistered = true;
    }

    /*
     * Register Service A Watch
     */

    private void registerRegistryWatch() {
        logger.info("> Registering Registry CRD Watch");
        registriesCRDClient.withResourceVersion(registriesResourceVersion).watch(new Watcher<Registry>() {
            @Override
            public void eventReceived(Watcher.Action action, Registry registry) {
                if (action.equals(Action.ADDED)) {
                    appService.addRegistryToApp(registry);

                }
                if (action.equals(Action.DELETED)) {
                    appService.removeRegistryFromApp(registry);

                }
                if (registry.getSpec() == null) {
                    logger.error("No Spec for resource " + registry);
                }
            }

            @Override
            public void onClose(KubernetesClientException cause) {
            }
        });
        registryWatchRegistered = true;
    }

    private void registerGatewayWatch() {
        logger.info("> Registering Gateway CRD Watch");
        gatewaysCRDClient.withResourceVersion(gatewaysResourceVersion).watch(new Watcher<Gateway>() {
            @Override
            public void eventReceived(Watcher.Action action, Gateway gateway) {
                if (action.equals(Action.ADDED)) {
                    appService.addGatewayToApp(gateway);

                }
                if (action.equals(Action.DELETED)) {
                    appService.removeGatewayFromApp(gateway);

                }
                if (gateway.getSpec() == null) {
                    logger.error("No Spec for resource " + gateway);
                }
            }

            @Override
            public void onClose(KubernetesClientException cause) {
            }
        });
        gatewayWatchRegistered = true;
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
                    app.getMetadata().setNamespace("jhipster");
                    logger.info("> Scanning App: " + appName + "...");
                    if (appService.isAppHealthy(app)) {
                        logger.info("> App Name: " + appName + " is up and running");
                        app.getSpec().getModules().forEach(m -> logger.info("\t> Module found: " + m));
                        app.getSpec().setStatus("HEALTHY");
                        String externalIp = k8SCoreRuntime.findGatewayExternalIP();
                        String url = "http://" + externalIp + "/apps/" + app.getMetadata().getName() + "/";
                        appService.addAppUrl(app.getMetadata().getName(), url);
                        app.getSpec().setUrl(url);
                        logger.info("> App: " + appName + ", status:  HEALTHY, URL: " + url + " \n");
                    } else {
                        logger.error("> App Name: " + appName + " is down due missing services");
                        if (app.getSpec().getModules() == null || app.getSpec().getModules().isEmpty()) {
                            logger.info("App: " + appName + ": No Modules found. ");
                        } else {
                            app.getSpec().getModules().forEach(m -> logger.info("\t> Module found: " + m));
                        }
                        app.getSpec().setStatus("UNHEALTHY");
                        app.getSpec().setUrl("N/A");
                        logger.info("> App: " + appName + ", status: UNHEALTHY. \n ");
                    }
                    logger.error(">>> App Before upsert: " + app.toString());
                    appCRDClient.createOrReplace(app);
                }
        );

    }


    public CustomResourceDefinition getModuleCRD() {
        return moduleCRD;
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
}
