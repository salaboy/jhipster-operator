package tech.jhipster.operator;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.annotation.Order;
import tech.jhipster.operator.app.AppService;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

@Configuration
@AutoConfigureBefore(GatewayAutoConfiguration.class)
public class OperatorAutoConfiguration {

    private Logger logger = LoggerFactory.getLogger(OperatorAutoConfiguration.class);

    @Bean
    public RouteDefinitionLocator applicationsRouteDefinitionLocator(AppsOperator appsOperator,
                                                                     AppService appService,
                                                                     KubernetesClient kubernetesClient) {
        return new OperatorRoutesLocator(appsOperator, appService, kubernetesClient);
    }
    
}
