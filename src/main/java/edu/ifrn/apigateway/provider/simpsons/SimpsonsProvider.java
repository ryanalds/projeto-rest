package edu.ifrn.apigateway.provider.simpsons;

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
public class SimpsonsProvider extends AbstractRestCatalogProvider {
    private static final String CDN_BASE = "https://cdn.thesimpsonsapi.com/500";

    public SimpsonsProvider(@Qualifier("simpsonsClient") RestClient client) { super(client); }

    @Override
    protected Source providerSource() { return Source.SIMPSONS; }

    @Override
    protected CatalogResource map(ResourceType type, JsonNode json) {
        int id = integer(json, "id", 0);
        String name = text(json, "name");
        Map<String, Object> attributes = new LinkedHashMap<>();
        Map<String, List<Integer>> relations = new LinkedHashMap<>();
        String imagePath = null;

        switch (type) {
            case CHARACTER -> {
                copy(json, attributes, "age", "birthdate", "gender", "occupation", "status", "description", "phrases");
                imagePath = text(json, "portrait_path");
                addOne(relations, "episodes", integerOrNull(json, "first_appearance_ep_id"));
                addOne(relations, "locations", integerOrNull(json, "first_appearance_loc_id"));
            }
            case EPISODE -> {
                copy(json, attributes, "airdate", "episode_number", "season", "description", "synopsis");
                imagePath = text(json, "image_path");
                addOne(relations, "characters", integerOrNull(json, "character_id"));
            }
            case LOCATION -> {
                copy(json, attributes, "description", "town", "use");
                imagePath = text(json, "image_path");
                addOne(relations, "episodes", integerOrNull(json, "first_appearance_ep_id"));
            }
        }
        return new CatalogResource(id, name, imageUrl(imagePath), source().path(), attributes, relations);
    }

    @Override
    protected PageResult<CatalogResource> mapPage(ResourceType type, int requestedPage, JsonNode json) {
        JsonNode results = json.path("results");
        List<CatalogResource> items = new ArrayList<>();
        if (results.isArray()) results.forEach(item -> items.add(map(type, item)));
        int totalPages = integer(json, "pages", 0);
        long count = json.path("count").asLong(items.size());
        return new PageResult<>(items, requestedPage, 20, count, totalPages);
    }

    private String imageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return null;
        if (imagePath.startsWith("https://") || imagePath.startsWith("http://")) return imagePath;
        return CDN_BASE + (imagePath.startsWith("/") ? imagePath : "/" + imagePath);
    }

    private Integer integerOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNull() || value.isMissingNode() ? null : value.asInt();
    }

    private void addOne(Map<String, List<Integer>> relations, String relation, Integer id) {
        if (id != null && id > 0) relations.put(relation, List.of(id));
    }

    private void copy(JsonNode source, Map<String, Object> target, String... fields) {
        for (String field : fields) {
            JsonNode value = source.path(field);
            if (value.isMissingNode() || value.isNull()) continue;
            if (value.isArray()) {
                List<String> strings = new ArrayList<>();
                value.forEach(item -> { if (item.isValueNode()) strings.add(item.asString()); });
                target.put(field, strings);
            } else if (value.isValueNode()) {
                target.put(field, value.isBoolean() ? value.asBoolean() : value.isNumber() ? value.asDouble() : value.asString());
            }
        }
    }
}
