package tech.jhipster.operator;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import reactor.core.publisher.Flux;
import tech.jhipster.operator.app.AppService;
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

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RefreshScope
public class OperatorRoutesLocator implements RouteDefinitionLocator {

    public static final String MICROSERVICES_PATH = "services";
    private Logger logger = LoggerFactory.getLogger(OperatorRoutesLocator.class);


    private AppsOperator appsOperator;

    private AppService appService;

    private KubernetesClient kubernetesClient;


    public OperatorRoutesLocator(AppsOperator appsOperator,
                                 AppService appService,
                                 KubernetesClient kubernetesClient) {
        this.appsOperator = appsOperator;
        this.appService = appService;
        this.kubernetesClient = kubernetesClient;

    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        try {
            List<RouteDefinition> allRouteDefinitions = new ArrayList<RouteDefinition>();
            if (appsOperator.getApplicationCRD() != null) {
                List<Application> applications = kubernetesClient.customResources(appsOperator.getApplicationCRD(), Application.class,
                        ApplicationList.class, DoneableApplication.class).list().getItems();

                applications.forEach(app -> {
                    //@TODO: read from virtual services from istio

                    List<RouteDefinition> appRouteDefinitions = new ArrayList<RouteDefinition>();
                    List<RouteDefinition> microServicesForApp = getMicroServicesRoutesForApplication(app);
                    appRouteDefinitions.addAll(microServicesForApp);
                    List<RouteDefinition> gatewayRoute = getGatewayRouteForApplication(app);
                    appRouteDefinitions.addAll(gatewayRoute);
                    List<RouteDefinition> registryRoute = getRegistryRouteForApplication(app);
                    appRouteDefinitions.addAll(registryRoute);

                    if (areApplicationRoutesReady(app, appRouteDefinitions)) { // if all the routes for the app are available add to main routes
                        allRouteDefinitions.addAll(appRouteDefinitions);
                    }
                });
            }

            //@TODO: i need to remove routes old routes ?????
            return Flux.fromIterable(allRouteDefinitions);
        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
    }


    //@TODO: improve routes and modules validation
    private boolean areApplicationRoutesReady(Application app, List<RouteDefinition> appRouteDefinitions) {
        final AtomicInteger validated = new AtomicInteger();
        if (appService.isAppHealthy(app)) { // ALL the required modules are present
            logger.info("> App: " + app.getMetadata().getName() + " validation!");
            Set<MicroServiceDescr> microservices = app.getSpec().getMicroservices();
            if (microservices != null) {
                microservices.forEach(md -> {
                    appRouteDefinitions.forEach(rd -> {
                        if (rd.getId().equals(app.getMetadata().getName() + ":" + md.getName())) {
                            validated.incrementAndGet();
                        }
                    });
                });
                logger.info("> MicroServices size: " + microservices.size() + " and validated: " + validated);
                if (validated.get() == microservices.size()) {
                    return true;
                }
            }
        }

        return false;

    }

    private List<RouteDefinition> createRouteForServices(Application app, List<CustomService> resources, String path) {
        List<RouteDefinition> routeDefinitions = new ArrayList<RouteDefinition>();
        resources.forEach(service -> {
            RouteDefinition routeDefinition = new RouteDefinition();
            routeDefinition.setId(app.getMetadata().getName() + ":" + service.getMetadata().getName());
            String port = "";
            if (service.getSpec().getServicePort() != null && !service.getSpec().getServicePort().isEmpty()) {
                port = ":" + service.getSpec().getServicePort();
            }
            String URLPath = "http://" + service.getSpec().getServiceName() + port;
            logger.info("URLPath for service (" + service.getMetadata().getName() + "): " + URLPath);
            routeDefinition.setUri(URI.create(URLPath));

            //@TODO: It will be nice to add into the HEADERS the application where the service belongs

            PredicateDefinition predicateDefinition = new PredicateDefinition();
            predicateDefinition.setName("Path");
            String pattern = "";
            //@TODO: refactor this instance of
            if(service instanceof Gateway){ // Gateway should go to the route and not use a special path
                pattern = "/apps/" + app.getMetadata().getName() + "/" + app.getSpec().getVersion() + "/";
            }else if(service instanceof Registry){ // Registry should go to the route and not use a special path
                pattern = "/apps/" + app.getMetadata().getName() + "/" + app.getSpec().getVersion() + "/registry/";
            }else {
                pattern = "/apps/" + app.getMetadata().getName() + "/" + app.getSpec().getVersion() + "/" + path + "/" + service.getMetadata().getName() + "/";
            }
            predicateDefinition.addArg("pattern", pattern + "**");
            routeDefinition.getPredicates().add(predicateDefinition);
            FilterDefinition filter = new FilterDefinition("RewritePath");
            filter.setArgs(ImmutableMap.of("regexp", pattern + "(?<remaining>.*)",
                    "replacement", "/${remaining}"));

            routeDefinition.setFilters(Arrays.asList( filter ));
            routeDefinitions.add(routeDefinition);
            logger.info("Route (id=" + app.getMetadata().getName() + ":" + service.getMetadata().getName() + ") added: " + pattern);
        });
        return routeDefinitions;
    }

    private List<RouteDefinition> getMicroServicesRoutesForApplication(Application app) {
        if (appsOperator.getMicroServiceCRD() != null) {
            List<MicroService> microServiceList = kubernetesClient.customResources(appsOperator.getMicroServiceCRD(), MicroService.class,
                    MicroServiceList.class, DoneableMicroService.class).list().getItems();
            return createRouteForServices(app, new ArrayList<>(microServiceList), MICROSERVICES_PATH);
        }
        return Collections.EMPTY_LIST;
    }

    private List<RouteDefinition> getGatewayRouteForApplication(Application app) {

        List<RouteDefinition> routeDefinitions = new ArrayList<RouteDefinition>();
        if (appsOperator.getGatewayCRD() != null) {
            List<Gateway> gatewayList = kubernetesClient.customResources(appsOperator.getGatewayCRD(), Gateway.class,
                    GatewayList.class, DoneableGateway.class).list().getItems();
            //@TODO: check and adapt: There should be just one Gateway per app
            return createRouteForServices(app, new ArrayList<>(gatewayList), "");
        }
        return Collections.EMPTY_LIST;
    }

    private List<RouteDefinition> getRegistryRouteForApplication(Application app) {

        List<RouteDefinition> routeDefinitions = new ArrayList<RouteDefinition>();
        if (appsOperator.getRegistryCRD() != null) {
            List<Registry> registryList = kubernetesClient.customResources(appsOperator.getRegistryCRD(), Registry.class,
                    RegistryList.class, DoneableRegistry.class).list().getItems();
            return createRouteForServices(app, new ArrayList<>(registryList), "");
        }
        return Collections.EMPTY_LIST;
    }


}
