package tech.jhipster.operator.crds.gateway;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneableGateway extends CustomResourceDoneable<Gateway> {

    public DoneableGateway(Gateway resource, Function<Gateway, Gateway> function) {
        super(resource, function);
    }
}
