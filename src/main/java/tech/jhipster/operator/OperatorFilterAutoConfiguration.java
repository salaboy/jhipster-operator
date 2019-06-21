package tech.jhipster.operator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

@Configuration
@AutoConfigureBefore(GatewayAutoConfiguration.class)
public class OperatorFilterAutoConfiguration {

    private Logger logger = LoggerFactory.getLogger(OperatorFilterAutoConfiguration.class);

    @Bean
    @Order(100)
    public GlobalFilter customGlobalFilter() {
        return (exchange, chain) -> {
            Set<URI> uris = exchange.getAttributeOrDefault(GATEWAY_ORIGINAL_REQUEST_URL_ATTR, Collections.emptySet());
            String originalUri = (uris.isEmpty()) ? "Unknown" : uris.iterator().next().toString();
            Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            URI routeUri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
            logger.error("> Incoming request " + originalUri + " is routed to id: " + route.getId()
                    + ", uri:" + routeUri);
            return chain.filter(exchange);
        };
    }
}
