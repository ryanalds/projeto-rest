package edu.ifrn.apigateway.discovery;

import org.springframework.hateoas.RepresentationModel;

public class SourceMenu extends RepresentationModel<SourceMenu> {
    private final String fonte;

    public SourceMenu(String fonte) { this.fonte = fonte; }

    public String getFonte() { return fonte; }
}
