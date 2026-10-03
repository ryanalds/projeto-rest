package edu.ifrn.apigateway.auth;

import org.springframework.hateoas.RepresentationModel;

public class TokenResponse extends RepresentationModel<TokenResponse> {
    private final String token;
    private final String tipo = "Bearer";
    private final long expiraEmSegundos;

    public TokenResponse(String token, long expiraEmSegundos) {
        this.token = token;
        this.expiraEmSegundos = expiraEmSegundos;
    }

    public String getToken() { return token; }
    public String getTipo() { return tipo; }
    public long getExpiraEmSegundos() { return expiraEmSegundos; }
}
