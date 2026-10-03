package edu.ifrn.apigateway.catalog;

import edu.ifrn.apigateway.common.error.ResourceNotFoundException;
import edu.ifrn.apigateway.common.page.PageResult;
import edu.ifrn.apigateway.discovery.SourceController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Min;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequestMapping("/api/{fonte}")
@Validated
@SecurityRequirement(name = "bearerAuth")
public class CatalogController {
    private final CatalogService service;

    public CatalogController(CatalogService service) { this.service = service; }

    @GetMapping("/{recurso}")
    @Operation(summary = "Lista uma coleção paginada da fonte selecionada")
    public CatalogPageModel list(
            @PathVariable String fonte,
            @PathVariable String recurso,
            @Parameter(description = "Número da página, começando em 1")
            @RequestParam(defaultValue = "1") @Min(1) int page) {
        Source source = source(fonte);
        ResourceType type = type(recurso);
        PageResult<CatalogResource> result = service.list(source, type, page);
        List<EntityModel<CatalogResource>> content = result.items().stream()
                .map(item -> item(item, source, type)).toList();
        CatalogPageModel model = new CatalogPageModel(type.path(), content,
                new org.springframework.hateoas.PagedModel.PageMetadata(
                        result.size(), page - 1L, result.totalElements(), result.totalPages()));
        model.add(pageLink(source, type, page, "self"));
        model.add(pageLink(source, type, 1, "first"));
        if (page > 1) model.add(pageLink(source, type, page - 1, "prev"));
        if (page < result.totalPages()) model.add(pageLink(source, type, page + 1, "next"));
        if (result.totalPages() > 0) model.add(pageLink(source, type, result.totalPages(), "last"));
        model.add(sourceLink(source));
        return model;
    }

    @GetMapping("/{recurso}/{id}")
    @Operation(summary = "Busca um recurso pelo id")
    public EntityModel<CatalogResource> getItem(@PathVariable String fonte,
                                                 @PathVariable String recurso,
                                                 @PathVariable @Min(1) int id) {
        Source source = source(fonte);
        ResourceType type = type(recurso);
        CatalogResource resource = service.find(source, type, id);
        return item(resource, source, type);
    }

    private EntityModel<CatalogResource> item(CatalogResource resource, Source source, ResourceType type) {
        var model = EntityModel.of(resource);
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(CatalogController.class)
                .getItem(source.path(), type.path(), resource.id())).withSelfRel());
        model.add(collectionLink(source, type));
        model.add(sourceLink(source));
        resource.relacoes().forEach((relation, ids) -> {
            ResourceType relatedType = relatedType(relation);
            ids.forEach(id -> model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(CatalogController.class)
                    .getItem(source.path(), relatedType.path(), id)).withRel(relation)));
        });
        return model;
    }

    private ResourceType relatedType(String relation) {
        return switch (relation) {
            case "episodes" -> ResourceType.EPISODE;
            case "characters" -> ResourceType.CHARACTER;
            case "locations", "location", "origin" -> ResourceType.LOCATION;
            default -> throw new IllegalArgumentException("Relação não suportada: " + relation);
        };
    }

    private Link collectionLink(Source source, ResourceType type) {
        return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(CatalogController.class)
                .list(source.path(), type.path(), 1)).withRel("collection");
    }

    private Link sourceLink(Source source) {
        return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SourceController.class)
                .menu(source.path())).withRel("source");
    }

    private Link pageLink(Source source, ResourceType type, int page, String relation) {
        String href = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/{source}/{resource}")
                .queryParam("page", page)
                .buildAndExpand(source.path(), type.path()).toUriString();
        return Link.of(href).withRel(relation);
    }

    private Source source(String value) {
        try { return Source.fromPath(value); }
        catch (IllegalArgumentException exception) { throw new ResourceNotFoundException(exception.getMessage()); }
    }

    private ResourceType type(String value) {
        try { return ResourceType.fromPath(value); }
        catch (IllegalArgumentException exception) { throw new ResourceNotFoundException(exception.getMessage()); }
    }
}
