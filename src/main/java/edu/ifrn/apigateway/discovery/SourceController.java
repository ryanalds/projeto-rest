package edu.ifrn.apigateway.discovery;

import edu.ifrn.apigateway.catalog.CatalogController;
import edu.ifrn.apigateway.catalog.ResourceType;
import edu.ifrn.apigateway.catalog.Source;
import edu.ifrn.apigateway.auth.RootController;
import edu.ifrn.apigateway.common.error.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class SourceController {
    @GetMapping("/{fonte}")
    public SourceMenu menu(@PathVariable String fonte) {
        Source source;
        try { source = Source.fromPath(fonte); }
        catch (IllegalArgumentException exception) { throw new ResourceNotFoundException(exception.getMessage()); }

        SourceMenu model = new SourceMenu(source.path());
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(RootController.class).root()).withRel("root"));
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SourceController.class)
                .menu(source.path())).withSelfRel());
        for (ResourceType type : ResourceType.values()) {
            model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(CatalogController.class)
                    .list(source.path(), type.path(), 1)).withRel(type.path()));
            String itemPath = "/api/" + source.path() + "/" + type.path() + "/{id}";
            String href = ServletUriComponentsBuilder.fromCurrentContextPath().path(itemPath).build().toUriString();
            model.add(Link.of(href, type.singular()));
        }
        return model;
    }
}
