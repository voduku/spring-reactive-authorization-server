package com.example.uaa.authorization.token;

import io.r2dbc.spi.ConnectionFactory;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.DefaultAuthenticationKeyGenerator;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author VuDo
 * @since 2/12/2021
 */
public class R2dbcTokenStore implements ReactiveTokenStore {

  private static final Log LOG = LogFactory.getLog(R2dbcTokenStore.class);

  private static final String DEFAULT_ACCESS_TOKEN_INSERT_STATEMENT = "insert into oauth_access_token (token_id, token, authentication_id, user_name, client_id, authentication, refresh_token) values (:tokenId, :token, :authenticationId, :username, :clientId, :authentication, :refreshToken)";

  private static final String DEFAULT_ACCESS_TOKEN_SELECT_STATEMENT = "select token_id, token from oauth_access_token where token_id = :tokenId";

  private static final String DEFAULT_ACCESS_TOKEN_AUTHENTICATION_SELECT_STATEMENT = "select token_id, authentication from oauth_access_token where token_id = :tokenId";

  private static final String DEFAULT_ACCESS_TOKEN_FROM_AUTHENTICATION_SELECT_STATEMENT = "select token_id, token from oauth_access_token where authentication_id = :authenticationId";

  private static final String DEFAULT_ACCESS_TOKENS_FROM_USERNAME_AND_CLIENT_SELECT_STATEMENT = "select token_id, token from oauth_access_token where user_name = :username and client_id = :clientId";

  private static final String DEFAULT_ACCESS_TOKENS_FROM_CLIENTID_SELECT_STATEMENT = "select token_id, token from oauth_access_token where client_id = :clientId";

  private static final String DEFAULT_ACCESS_TOKEN_DELETE_STATEMENT = "delete from oauth_access_token where token_id = :tokenId";

  private static final String DEFAULT_ACCESS_TOKEN_DELETE_FROM_REFRESH_TOKEN_STATEMENT = "delete from oauth_access_token where refresh_token = :refreshToken";

  private static final String DEFAULT_REFRESH_TOKEN_INSERT_STATEMENT = "insert into oauth_refresh_token (token_id, token, authentication) values (:tokenId, :token, :authentication)";

  private static final String DEFAULT_REFRESH_TOKEN_SELECT_STATEMENT = "select token_id, token from oauth_refresh_token where token_id = :tokenId";

  private static final String DEFAULT_REFRESH_TOKEN_AUTHENTICATION_SELECT_STATEMENT = "select token_id, authentication from oauth_refresh_token where token_id = :tokenId";

  private static final String DEFAULT_REFRESH_TOKEN_DELETE_STATEMENT = "delete from oauth_refresh_token where token_id = :tokenId";

  private final R2dbcEntityTemplate template;

  private final AuthenticationKeyGenerator authenticationKeyGenerator = new DefaultAuthenticationKeyGenerator();

  public R2dbcTokenStore(ConnectionFactory connectionFactory) {
    Assert.notNull(connectionFactory, "Connection Factory required");
    template = new R2dbcEntityTemplate(connectionFactory);
  }

  @Override
  public Mono<OAuth2Authentication> readAuthentication(OAuth2AccessToken token) {
    return readAuthentication(token.getValue());
  }

  @Override
  public Mono<OAuth2Authentication> readAuthentication(String token) {
    return selectAccessTokenAuthenticationSql(token)
        .doOnError(EmptyResultDataAccessException.class, e -> {
          if (LOG.isInfoEnabled()) {
            LOG.info("Failed to find access token");
          }
        })
        .doOnError(IllegalArgumentException.class, e -> {
          LOG.warn("Failed to deserialize authentication", e);
          removeAccessToken(token).subscribe();
        });
  }

  @Override
  public Mono<Void> storeAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
    String refreshToken = token.getRefreshToken() != null ? token.getRefreshToken().getValue() : null;
    return Mono.just(token)
        .flatMap(this::removeAccessTokenIfExist)
        .flatMap(tk -> insertAccessTokenSql(tk, authentication, refreshToken));
  }

  public Mono<OAuth2AccessToken> removeAccessTokenIfExist(OAuth2AccessToken token) {
    return Mono.just(token)
        .map(OAuth2AccessToken::getValue)
        .flatMap(this::readAccessToken)
        .flatMap(this::removeAccessToken)
        .map(ignored-> token)
        .switchIfEmpty(Mono.just(token));
  }

  @Override
  public Mono<OAuth2AccessToken> readAccessToken(String tokenValue) {
    return selectAccessTokenSql(tokenValue)
        .doOnError(EmptyResultDataAccessException.class, e -> {
          if (LOG.isInfoEnabled()) {
            LOG.info("Failed to find access token");
          }
        })
        .doOnError(IllegalArgumentException.class, e -> {
          LOG.warn("Failed to deserialize access token", e);
          removeAccessToken(tokenValue).subscribe();
        });
  }

  @Override
  public Mono<Void> removeAccessToken(OAuth2AccessToken token) {
    return removeAccessToken(token.getValue());
  }

  public Mono<Void> removeAccessToken(String tokenValue) {
    return Mono.just(tokenValue)
        .map(this::extractTokenKey)
        .flatMap(this::deleteAccessTokenSql);
  }

  @Override
  public Mono<Void> storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
    return insertRefreshTokenSql(refreshToken, authentication);
  }

  @Override
  public Mono<OAuth2RefreshToken> readRefreshToken(String tokenValue) {
    return selectRefreshTokenSql(tokenValue)
        .doOnError(EmptyResultDataAccessException.class, e -> {
          if (LOG.isInfoEnabled()) {
            LOG.info("Failed to find refresh token");
          }
        })
        .doOnError(IllegalArgumentException.class, e -> {
          LOG.warn("Failed to deserialize refresh token", e);
          removeRefreshToken(tokenValue).subscribe();
        });
  }

  @Override
  public Mono<OAuth2Authentication> readAuthenticationForRefreshToken(OAuth2RefreshToken token) {
    return readAuthenticationForRefreshToken(token.getValue());
  }

  public Mono<OAuth2Authentication> readAuthenticationForRefreshToken(String token) {
    return selectRefreshTokenAuthenticationSql(token)
        .doOnError(EmptyResultDataAccessException.class, e -> {
          if (LOG.isInfoEnabled()) {
            LOG.info("Failed to find refresh token");
          }
        })
        .doOnError(IllegalArgumentException.class, e -> {
          LOG.warn("Failed to deserialize refresh token", e);
          removeRefreshToken(token).subscribe();
        });
  }

  @Override
  public Mono<Void> removeRefreshToken(OAuth2RefreshToken token) {
    return removeRefreshToken(token.getValue());
  }

  public Mono<Void> removeRefreshToken(String token) {
    return deleteRefreshTokenSql(token);
  }

  @Override
  public Mono<Void> removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
    return removeAccessTokenUsingRefreshToken(refreshToken.getValue());
  }

  public Mono<Void> removeAccessTokenUsingRefreshToken(String refreshToken) {
    return deleteAccessTokenFromRefreshTokenSql(refreshToken);
  }

  @Override
  public Mono<OAuth2AccessToken> getAccessToken(OAuth2Authentication authentication) {
    String key = authenticationKeyGenerator.extractKey(authentication);
    return Mono.just(key)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Can't extract token key")))
        .flatMap(this::selectAccessTokenFromAuthenticationSql)
        .doOnError(EmptyResultDataAccessException.class, e -> {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Failed to find access token for authentication " + authentication);
          }
        })
        .doOnError(IllegalArgumentException.class, e -> LOG.error("Could not extract access token for authentication " + authentication, e))
        .doOnNext(accessToken -> reloadToken(accessToken, authentication, key));
  }

  @Override
  public Flux<OAuth2AccessToken> findTokensByClientIdAndUserName(String clientId, String username) {
    return selectAccessTokensFromUserNameAndClientIdSql(username, clientId)
        .doOnError(EmptyResultDataAccessException.class, e -> {
          if (LOG.isInfoEnabled()) {
            LOG.info("Failed to find access token for clientId " + clientId + " and userName " + username);
          }
        });
  }

  @Override
  public Flux<OAuth2AccessToken> findTokensByClientId(String clientId) {
    return selectAccessTokensFromClientIdSql(clientId)
        .doOnError(EmptyResultDataAccessException.class, e -> {
          if (LOG.isInfoEnabled()) {
            LOG.info("Failed to find access token for clientId " + clientId);
          }
        });
  }


  private void reloadToken(OAuth2AccessToken accessToken, OAuth2Authentication authentication, String key) {
    Mono.just(accessToken)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Can't find access token")))
        .map(OAuth2AccessToken::getValue)
        .flatMap(this::readAuthentication)
        .map(authenticationKeyGenerator::extractKey)
        .filter(key::equals)
        .doOnNext(k -> removeAccessToken(k).subscribe())
        .doOnNext(k -> storeAccessToken(accessToken, authentication).subscribe())
        .subscribe();
  }

  private Flux<OAuth2AccessToken> selectAccessTokensFromUserNameAndClientIdSql(String username, String clientId) {
    return template.getDatabaseClient()
        .sql(DEFAULT_ACCESS_TOKENS_FROM_USERNAME_AND_CLIENT_SELECT_STATEMENT)
        .bind("username", username)
        .bind("clientId", clientId)
        .map((row, metadata) -> {
          try {
            return deserializeAccessToken(row.get(1));
          } catch (IllegalArgumentException e) {
            deleteAccessTokenSql(row.get(0, String.class));
            return null;
          }
        })
        .all()
        .filter(Objects::nonNull);
  }

  private Flux<OAuth2AccessToken> selectAccessTokensFromClientIdSql(String key) {
    return template.getDatabaseClient()
        .sql(DEFAULT_ACCESS_TOKENS_FROM_CLIENTID_SELECT_STATEMENT)
        .bind("clientId", key)
        .map((row, metadata) -> {
          try {
            return deserializeAccessToken(row.get(1));
          } catch (IllegalArgumentException e) {
            deleteAccessTokenSql(row.get(0, String.class));
            return null;
          }
        })
        .all()
        .filter(Objects::nonNull);
  }

  private Mono<OAuth2AccessToken> selectAccessTokenFromAuthenticationSql(String key) {
    return template.getDatabaseClient()
        .sql(DEFAULT_ACCESS_TOKEN_FROM_AUTHENTICATION_SELECT_STATEMENT)
        .bind("authenticationId", key)
        .map((row, metadata) -> deserializeAccessToken(row.get(1)))
        .one();
  }

  private Mono<OAuth2Authentication> selectAccessTokenAuthenticationSql(String key) {
    return template.getDatabaseClient()
        .sql(DEFAULT_ACCESS_TOKEN_AUTHENTICATION_SELECT_STATEMENT)
        .bind("tokenId", extractTokenKey(key))
        .map((row, metadata) -> deserializeAuthentication(row.get(1)))
        .one();
  }

  private Mono<OAuth2RefreshToken> selectRefreshTokenSql(String token) {
    return template.getDatabaseClient()
        .sql(DEFAULT_REFRESH_TOKEN_SELECT_STATEMENT)
        .bind("tokenId", extractTokenKey(token))
        .map((row, metadata) -> deserializeRefreshToken(row.get(1)))
        .one();
  }

  private Mono<OAuth2Authentication> selectRefreshTokenAuthenticationSql(String token) {
    return template.getDatabaseClient()
        .sql(DEFAULT_REFRESH_TOKEN_AUTHENTICATION_SELECT_STATEMENT)
        .bind("tokenId", extractTokenKey(token))
        .map((row, metadata) -> deserializeAuthentication(row.get(1)))
        .one();
  }

  private Mono<OAuth2AccessToken> selectAccessTokenSql(String token) {
    return template.getDatabaseClient()
        .sql(DEFAULT_ACCESS_TOKEN_SELECT_STATEMENT)
        .bind("tokenId", extractTokenKey(token))
        .map((row, metadata) -> deserializeAccessToken(row.get(1)))
        .one();
  }

  private Mono<Void> deleteAccessTokenSql(String key) {
    return template.getDatabaseClient()
        .sql(DEFAULT_ACCESS_TOKEN_DELETE_STATEMENT)
        .bind("tokenId", key)
        .then();
  }

  private Mono<Void> insertRefreshTokenSql(OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
    return template.getDatabaseClient()
        .sql(DEFAULT_REFRESH_TOKEN_INSERT_STATEMENT)
        .bind("tokenId", extractTokenKey(refreshToken.getValue()))
        .bind("token", serializeRefreshToken(refreshToken))
        .bind("authentication", serializeAuthentication(authentication))
        .then();
  }

  private Mono<Void> insertAccessTokenSql(OAuth2AccessToken token, OAuth2Authentication authentication, String refreshToken) {
    GenericExecuteSpec sql = template.getDatabaseClient()
        .sql(DEFAULT_ACCESS_TOKEN_INSERT_STATEMENT)
        .bind("tokenId", extractTokenKey(token.getValue()))
        .bind("token", serializeAccessToken(token))
        .bind("authenticationId", authenticationKeyGenerator.extractKey(authentication))
        .bind("clientId", authentication.getOAuth2Request().getClientId())
        .bind("authentication", serializeAuthentication(authentication))
        .bind("refreshToken", extractTokenKey(refreshToken));
    if (authentication.isClientOnly()) {
      sql = sql.bindNull("username", String.class);
    } else {
      sql = sql.bind("username", authentication.getName());
    }
    return sql.then();
  }

  private Mono<Void> deleteRefreshTokenSql(String token) {
    return template.getDatabaseClient()
        .sql(DEFAULT_REFRESH_TOKEN_DELETE_STATEMENT)
        .bind("tokenId", extractTokenKey(token))
        .then();
  }

  private Mono<Void> deleteAccessTokenFromRefreshTokenSql(String token) {
    return template.getDatabaseClient()
        .sql(DEFAULT_ACCESS_TOKEN_DELETE_FROM_REFRESH_TOKEN_STATEMENT)
        .bind("refreshToken", extractTokenKey(token))
        .then();
  }

  protected String extractTokenKey(String value) {
    if (value == null) {
      return null;
    }
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("MD5 algorithm not available.  Fatal (should be in the JDK).");
    }

    byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
    return String.format("%032x", new BigInteger(1, bytes));
  }

  protected byte[] serializeAccessToken(OAuth2AccessToken token) {
    return SerializationUtils.serialize(token);
  }

  protected OAuth2AccessToken deserializeAccessToken(Object token) {
    ByteBuffer buffer = (ByteBuffer) token;
    byte[] bytea = new byte[buffer.remaining()];
    buffer.get(bytea);
    return SerializationUtils.deserialize(bytea);
  }

  protected OAuth2RefreshToken deserializeRefreshToken(Object token) {
    ByteBuffer buffer = (ByteBuffer) token;
    byte[] bytea = new byte[buffer.remaining()];
    buffer.get(bytea);
    return SerializationUtils.deserialize(bytea);
  }

  protected OAuth2Authentication deserializeAuthentication(Object authentication) {
    ByteBuffer buffer = (ByteBuffer) authentication;
    byte[] bytea = new byte[buffer.remaining()];
    buffer.get(bytea);
    return SerializationUtils.deserialize(bytea);
  }

  protected byte[] serializeAuthentication(OAuth2Authentication authentication) {
    return SerializationUtils.serialize(authentication);
  }

  protected byte[] serializeRefreshToken(OAuth2RefreshToken token) {
    return SerializationUtils.serialize(token);
  }
}
