package tech.jhipster.operator.crds.microservice;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneableMicroService extends CustomResourceDoneable<MicroService> {

    public DoneableMicroService(MicroService resource, Function<MicroService, MicroService> function) {
        super(resource, function);
    }
}
