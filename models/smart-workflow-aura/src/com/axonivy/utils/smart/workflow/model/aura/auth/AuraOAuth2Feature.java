package com.axonivy.utils.smart.workflow.model.aura.auth;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import ch.ivyteam.ivy.bpm.error.BpmPublicErrorBuilder;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.rest.client.feature.FeatureConfig;
import ch.ivyteam.ivy.rest.client.oauth2.OAuth2BearerFilter;
import ch.ivyteam.ivy.rest.client.oauth2.OAuth2RedirectErrorBuilder;
import ch.ivyteam.ivy.rest.client.oauth2.OAuth2TokenRequester.AuthContext;
import ch.ivyteam.ivy.rest.client.oauth2.uri.OAuth2CallbackUriBuilder;
import ch.ivyteam.ivy.rest.client.oauth2.uri.OAuth2UriProperty;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

/**
 * DocuWare Aura OAuth2 flow implementation.
 *
 * <ul>
 * <li>Requires a registered application in DocuWare Aura</li>
 * <li>Resolves access tokens for the Aura inference API endpoint</li>
 * <li>Supports OpenID Connect authorization code flow with PKCE</li>
 * </ul>
 *
 * @since 1.0
 */
public class AuraOAuth2Feature implements Feature {

  private static final String CODE_VERIFIER_SESSION_KEY = "aura.pkce.code_verifier";
  private static final int CODE_VERIFIER_BYTE_LENGTH = 96;
  private static final String ORG_ID_CONFIG_PROPERTY = "PATH.orgId";
  private static final String ORG_ID_URI_TEMPLATE = "{orgId}";

  public static interface Default {
    String APP_SCOPE = "docuware.platform openid dwprofile offline_access";
  }

  public static interface Property {
    String APP_ID = "AUTH.appId";
    String CLIENT_SECRET = "AUTH.secretKey";
    String SCOPE = "AUTH.scope";
    String AUTH_BASE_URI = "AUTH.baseUri";
  }

  @Override
  public boolean configure(FeatureContext context) {
    var config = FeatureConfig.of(context.getConfiguration(), AuraOAuth2Feature.class);

    var authBaseUri = resolveAuthBaseUri(config);
    var uriConfig = FeatureConfig.of(
        property -> Property.AUTH_BASE_URI.equals(property) ? authBaseUri : config.read(property).orElse(null),
        AuraOAuth2Feature.class);
    var auraUri = new OAuth2UriProperty(uriConfig, Property.AUTH_BASE_URI);
    Ivy.log().debug("Created Aura OAuth2 base URI: " + auraUri.getUri(null));

    var oauth2 = new OAuth2BearerFilter(
        ctxt -> requestToken(ctxt, auraUri),
        auraUri);
    oauth2.tokenScopeProperty(Property.SCOPE);
    oauth2.tokenSuffix(() -> "authorization_code");
    context.register(oauth2, Priorities.AUTHORIZATION);
    return true;
  }

  static String resolveAuthBaseUri(FeatureConfig config) {
    var authBaseUri = config.read(Property.AUTH_BASE_URI).orElseThrow();
    if (!authBaseUri.contains(ORG_ID_URI_TEMPLATE)) {
      return authBaseUri;
    }

    var orgId = config.readMandatory(ORG_ID_CONFIG_PROPERTY);
    return authBaseUri.replace(ORG_ID_URI_TEMPLATE, orgId);
  }

  private static Response requestToken(AuthContext ctxt, OAuth2UriProperty uriFactory) {
    FeatureConfig config = ctxt.config;
    var authCode = ctxt.authCode();
    var refreshToken = ctxt.refreshToken();

    Ivy.log().debug("refreshToken");

    if (authCode.isEmpty() && refreshToken.isEmpty()) {
      authError(config, uriFactory)
          .withMessage("Missing permission from user to access Aura API.")
          .throwError();
    }

    Form form = createTokenPayload(config, authCode, refreshToken);
    var response = ctxt.target.request()
        .accept(MediaType.WILDCARD)
        .post(Entity.form(form));
    return response;
  }

  static Form createTokenPayload(FeatureConfig config, Optional<String> authCode, Optional<String> refreshToken) {
    Form form = new Form();
    form.param("client_id", config.readMandatory(Property.APP_ID));

    var clientSecret = config.read(Property.CLIENT_SECRET);
    if (clientSecret.isPresent()) {
      form.param("client_secret", clientSecret.get());
    }

    if (refreshToken.isPresent()) {
      form.param("grant_type", "refresh_token");
      form.param("refresh_token", refreshToken.get());
      form.param("redirect_uri", ivyCallbackUri().toASCIIString());
    } else {
      form.param("grant_type", "authorization_code");
      form.param("redirect_uri", ivyCallbackUri().toASCIIString());
      authCode.ifPresent(code -> form.param("code", code));

      var codeVerifier = Ivy.session().getAttribute(CODE_VERIFIER_SESSION_KEY);
      if (codeVerifier == null) {
        throw new IllegalStateException("PKCE code verifier is missing for the authorization-code exchange.");
      }
      form.param("code_verifier", String.valueOf(codeVerifier));
      Ivy.session().removeAttribute(CODE_VERIFIER_SESSION_KEY);
    }

    return form;
  }

  private static BpmPublicErrorBuilder authError(FeatureConfig config, OAuth2UriProperty uriFactory) {
    var uri = createAuraAuthCodeUri(config, uriFactory);
    return OAuth2RedirectErrorBuilder.create(uri)
        .withMessage("Missing permission from user to access Aura API.");
  }

  private static URI createAuraAuthCodeUri(FeatureConfig config, OAuth2UriProperty uriFactory) {
    var codeVerifier = generateCodeVerifier();
    var codeChallenge = generateCodeChallenge(codeVerifier);

    Ivy.session().setAttribute(CODE_VERIFIER_SESSION_KEY, codeVerifier);

    var authUri = UriBuilder.fromUri(uriFactory.getUri("authorize"))
        .queryParam("client_id", config.readMandatory(Property.APP_ID))
        .queryParam("scope", getScope(config))
        .queryParam("redirect_uri", ivyCallbackUri())
        .queryParam("response_type", "code")
        .queryParam("response_mode", "query")
        .queryParam("code_challenge", codeChallenge)
        .queryParam("code_challenge_method", "S256")
        .build();
    Ivy.log().debug("DW AUTH " + authUri);
    return authUri;
  }

  private static String generateCodeVerifier() {
    var random = new SecureRandom();
    byte[] bytes = new byte[CODE_VERIFIER_BYTE_LENGTH];
    random.nextBytes(bytes);

    var encoder = Base64.getUrlEncoder().withoutPadding();
    return encoder.encodeToString(bytes);
  }

  private static String generateCodeChallenge(String codeVerifier) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
      var encoder = Base64.getUrlEncoder().withoutPadding();
      return encoder.encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }

  private static URI ivyCallbackUri() {
    URI url = OAuth2CallbackUriBuilder.create().toUrl();
    Ivy.log().debug("Creating Ivy callback URI: " + url);
    return url;
  }

  private static String getScope(FeatureConfig config) {
    return config.read(Property.SCOPE).orElse(Default.APP_SCOPE);
  }
}
