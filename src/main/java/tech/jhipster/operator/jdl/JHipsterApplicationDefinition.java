package tech.jhipster.operator.jdl;

import java.util.HashSet;

import java.util.Set;

public class JHipsterApplicationDefinition {
    private String name;
    private String version;
    private Set<JHipsterModuleDefinition> modules;
    private String JDLContent;


    public JHipsterApplicationDefinition() {
    }

    public JHipsterApplicationDefinition(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public Set<JHipsterModuleDefinition> getModules() {
        return modules;
    }

    public void setModules(Set<JHipsterModuleDefinition> modules) {
        this.modules = modules;
    }

    public void addModule(JHipsterModuleDefinition module) {
        if (modules == null) {
            modules = new HashSet<>();
        }
        modules.add(module);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJDLContent() {
        return JDLContent;
    }

    public void setJDLContent(String jDLContent) {
        this.JDLContent = jDLContent;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "JHipsterApplicationDefinition{" +
                "name='" + name + '\'' +
                ", modules=" + modules +
                ", JDLContent='" + JDLContent + '\'' +
                '}';
    }
}
