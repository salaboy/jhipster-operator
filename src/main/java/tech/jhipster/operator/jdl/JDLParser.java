package tech.jhipster.operator.jdl;

/*
 * This is not a serious parser, this is just to demonstrate what can be done if we have a JDL Java Class Model available
 * in other words.. this sucks.. please don't use it :)
 */
public class JDLParser {

    public static JHipsterApplicationDefinition parse(String name, String version, String jdl) {
        JHipsterApplicationDefinition jHipsterApplicationDefinition = new JHipsterApplicationDefinition(name, version);
        jHipsterApplicationDefinition.setJDLContent(jdl);
        String[] applications = jdl.split("application \\{"); // Extract Application/MicroService
        for (String app : applications) {
            if (!app.trim().isEmpty()) {
                String[] configs = app.trim().split("config \\{");
                JHipsterModuleDefinition jHipsterModuleDefinition = new JHipsterModuleDefinition();
                for (String config : configs) {
                    if (!config.trim().isEmpty()) {
                        String[] lines = config.trim().split(",");
                        for (String line : lines) {
                            if (line.contains("baseName")) {
                                jHipsterModuleDefinition.setName(line.trim().split(" ")[1]);
                            }
                            if (line.contains("applicationType")) {
                                jHipsterModuleDefinition.setType(line.trim().split(" ")[1]);
                            }
                            if(line.contains("serverPort")){
                                jHipsterModuleDefinition.setPort(line.trim().split(" ")[1]);
                            }

                        }
                    }

                }
                jHipsterApplicationDefinition.addModule(jHipsterModuleDefinition);
            }

        }
        return jHipsterApplicationDefinition;
    }

    public static String fromJDLServiceToKind(String type) {
        switch (type) {
            case "gateway":
                return "Gateway";
            case "microservice":
                return "MicroService";
            case "registry":
                return "Registry";
            default:
                return "N/A";
        }
    }
}
