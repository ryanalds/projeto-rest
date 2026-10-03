package edu.ifrn.apigateway.catalog;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class CatalogPageModel extends RepresentationModel<CatalogPageModel> {
    private final Map<String, List<EntityModel<CatalogResource>>> embedded;
    private final PagedModel.PageMetadata page;

    public CatalogPageModel(String collectionRel, List<EntityModel<CatalogResource>> items,
                            PagedModel.PageMetadata page) {
        this.embedded = Map.of(collectionRel, items);
        this.page = page;
    }

    @JsonProperty("_embedded")
    public Map<String, List<EntityModel<CatalogResource>>> getEmbedded() { return embedded; }

    public PagedModel.PageMetadata getPage() { return page; }
}
