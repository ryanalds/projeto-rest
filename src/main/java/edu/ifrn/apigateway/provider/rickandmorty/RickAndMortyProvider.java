package edu.ifrn.apigateway.provider.rickandmorty;

import edu.ifrn.apigateway.catalog.CatalogResource;
import edu.ifrn.apigateway.catalog.ResourceType;
import edu.ifrn.apigateway.catalog.Source;
import edu.ifrn.apigateway.common.page.PageResult;
import edu.ifrn.apigateway.provider.AbstractRestCatalogProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RickAndMortyProvider extends AbstractRestCatalogProvider {
    public RickAndMortyProvider(@Qualifier("rickAndMortyClient") RestClient client) { super(client); }

    @Override
    protected Source providerSource() { return Source.RICKANDMORTY; }

    @Override
    protected CatalogResource map(ResourceType type, JsonNode json) {
        int id = integer(json, "id", 0);
        String name = text(json, "name");
        String image = type == ResourceType.CHARACTER ? text(json, "image") : null;
        Map<String, Object> attributes = new LinkedHashMap<>();
        Map<String, List<Integer>> relations = new LinkedHashMap<>();

        switch (type) {
            case CHARACTER -> {
                copy(json, attributes, "status", "species", "type", "gender");
                copyNestedName(json, attributes, "origin", "origem");
                copyNestedName(json, attributes, "location", "localizacao");
                addOne(relations, "origin", idFromUrl(text(json.path("origin"), "url")));
                addOne(relations, "location", idFromUrl(text(json.path("location"), "url")));
                addMany(relations, "episodes", idsFromUrls(json.path("episode")));
            }
            case EPISODE -> {
                copy(json, attributes, "air_date", "episode");
                addMany(relations, "characters", idsFromUrls(json.path("characters")));
            }
            case LOCATION -> {
                copy(json, attributes, "type", "dimension");
                addMany(relations, "characters", idsFromUrls(json.path("residents")));
            }
        }
        return new CatalogResource(id, name, image, source().path(), attributes, relations);
    }

    @Override
    protected PageResult<CatalogResource> mapPage(ResourceType type, int requestedPage, JsonNode json) {
        JsonNode results = json.path("results");
        List<CatalogResource> items = new ArrayList<>();
        if (results.isArray()) results.forEach(item -> items.add(map(type, item)));
        JsonNode info = json.path("info");
        int pages = integer(info, "pages", 0);
        long count = info.path("count").asLong(items.size());
        return new PageResult<>(items, requestedPage, 20, count, pages);
    }

    private void copy(JsonNode source, Map<String, Object> target, String... fields) {
        for (String field : fields) {
            String value = text(source, field);
            if (value != null && !value.isBlank()) target.put(field, value);
        }
    }

    private void copyNestedName(JsonNode source, Map<String, Object> target, String field, String outputName) {
        String value = text(source.path(field), "name");
        if (value != null) target.put(outputName, value);
    }

    private void addOne(Map<String, List<Integer>> relations, String relation, Integer id) {
        if (id != null) relations.put(relation, List.of(id));
    }

    private void addMany(Map<String, List<Integer>> relations, String relation, List<Integer> ids) {
        if (!ids.isEmpty()) relations.put(relation, ids);
    }
}
