package edu.ifrn.apigateway.auth;

import edu.ifrn.apigateway.discovery.SourceController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class RootController {

    @GetMapping
    public RepresentationModel<?> root() {
        RepresentationModel<?> model = new RepresentationModel<>();
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(RootController.class).root()).withSelfRel());
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SourceController.class)
                .menu("rickandmorty")).withRel("rickandmorty").withTitle("Rick and Morty"));
        model.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SourceController.class)
                .menu("simpsons")).withRel("simpsons").withTitle("Os Simpsons"));
        return model;
    }
}
