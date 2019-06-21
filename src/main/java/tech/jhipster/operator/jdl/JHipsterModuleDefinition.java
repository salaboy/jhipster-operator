package tech.jhipster.operator.jdl;

public class JHipsterModuleDefinition {
    private String name;
    private String type;
    private String port;

    public JHipsterModuleDefinition() {
    }

    public JHipsterModuleDefinition(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public JHipsterModuleDefinition(String name, String type, String port) {
        this(name, type);
        this.port = port;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "JHipsterModuleDefinition{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", port='" + port + '\'' +
                '}';
    }
}
