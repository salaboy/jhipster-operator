package tech.jhipster.operator.crds.module;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneableModule extends CustomResourceDoneable<Module> {

    public DoneableModule(Module resource, Function<Module, Module> function) {
        super(resource, function);
    }
}
