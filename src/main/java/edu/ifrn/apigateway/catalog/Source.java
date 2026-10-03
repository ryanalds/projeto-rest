package edu.ifrn.apigateway.catalog;

import java.util.Arrays;

public enum Source {
    RICKANDMORTY("rickandmorty", "Rick and Morty"),
    SIMPSONS("simpsons", "Os Simpsons");

    private final String path;
    private final String title;

    Source(String path, String title) {
        this.path = path;
        this.title = title;
    }

    public String path() { return path; }
    public String title() { return title; }

    public static Source fromPath(String path) {
        return Arrays.stream(values()).filter(source -> source.path.equalsIgnoreCase(path))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Fonte inexistente: " + path));
    }
}
