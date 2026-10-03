package edu.ifrn.apigateway.catalog;

import java.util.Arrays;

public enum ResourceType {
    CHARACTER("characters", "character", "characters"),
    EPISODE("episodes", "episode", "episodes"),
    LOCATION("locations", "location", "locations");

    private final String path;
    private final String rickAndMortyPath;
    private final String simpsonsPath;

    ResourceType(String path, String rickAndMortyPath, String simpsonsPath) {
        this.path = path;
        this.rickAndMortyPath = rickAndMortyPath;
        this.simpsonsPath = simpsonsPath;
    }

    public String path() { return path; }
    public String singular() { return name().toLowerCase(); }
    public String externalPath(Source source) {
        return source == Source.SIMPSONS ? simpsonsPath : rickAndMortyPath;
    }

    public static ResourceType fromPath(String path) {
        return Arrays.stream(values()).filter(type -> type.path.equalsIgnoreCase(path))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Recurso inexistente: " + path));
    }
}
