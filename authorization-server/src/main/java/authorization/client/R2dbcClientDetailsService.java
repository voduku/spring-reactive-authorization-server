package authorization.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientAlreadyExistsException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.NoSuchClientException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author VuDo
 * @since 2/11/2021
 */
public class R2dbcClientDetailsService implements ReactiveClientDetailsService, ReactiveClientRegistrationService {

  private static final Log logger = LogFactory.getLog(R2dbcClientDetailsService.class);
  private static final String CLIENT_FIELDS_FOR_UPDATE = "resource_ids, scope, "
      + "authorized_grant_types, web_server_redirect_uri, authorities, access_token_validity, "
      + "refresh_token_validity, additional_information, autoapprove";

  private static final String CLIENT_FIELDS = "client_secret, " + CLIENT_FIELDS_FOR_UPDATE;
  private static final String BASE_FIND_STATEMENT = "select client_id, " + CLIENT_FIELDS + " from oauth_client_details";
  private static final String DEFAULT_FIND_STATEMENT = BASE_FIND_STATEMENT + " order by client_id";
  private static final String DEFAULT_SELECT_STATEMENT = BASE_FIND_STATEMENT + " where client_id = :clientId";
  private static final String DEFAULT_INSERT_STATEMENT =
      "insert into oauth_client_details (" + CLIENT_FIELDS + ", client_id) values (?,?,?,?,?,?,?,?,?,?,?)";
  private static final String DEFAULT_UPDATE_STATEMENT = "update oauth_client_details " + "set "
      + CLIENT_FIELDS_FOR_UPDATE.replaceAll(", ", "=?, ") + "=? where client_id = ?";
  private static final String DEFAULT_UPDATE_SECRET_STATEMENT =
      "update oauth_client_details " + "set client_secret = :clientSecret where client_id = :clientId";
  private static final String DEFAULT_DELETE_STATEMENT = "delete from oauth_client_details where client_id = :clientId";
  private final R2dbcEntityTemplate template;
  private final ObjectMapper mapper = new ObjectMapper();
  @Setter
  private PasswordEncoder passwordEncoder = NoOpPasswordEncoder.getInstance();

  public R2dbcClientDetailsService(ConnectionFactory connectionFactory) {
    Assert.notNull(connectionFactory, "Connection Factory required");
    template = new R2dbcEntityTemplate(connectionFactory);
  }

  @Override
  public Mono<ClientDetails> loadClientByClientId(String clientId) throws ClientRegistrationException {
    return selectClientDetailsSql(clientId)
        .doOnError(EmptyResultDataAccessException.class, e -> {
          throw new NoSuchClientException("No client with requested id: " + clientId);
        });
  }


  @Override
  public Mono<Void> addClientDetails(ClientDetails clientDetails) throws ClientAlreadyExistsException {
    return insertClientDetailsSql(clientDetails)
        .doOnError(DuplicateKeyException.class, e -> {
          throw new ClientAlreadyExistsException("Client already exists: " + clientDetails.getClientId(), e);
        });
  }

  @Override
  public Mono<Void> updateClientDetails(ClientDetails clientDetails) throws NoSuchClientException {
    return updateClientDetailsSql(clientDetails);
  }

  @Override
  public Mono<Void> updateClientSecret(String clientId, String secret) throws NoSuchClientException {
    return updateClientSecretSql(clientId, secret);
  }

  @Override
  public Mono<Void> removeClientDetails(String clientId) throws NoSuchClientException {
    return deleteClientDetailsSql(clientId);
  }

  @Override
  public Flux<ClientDetails> listClientDetails() {
    return findClientDetailsSql();
  }

  private Flux<ClientDetails> findClientDetailsSql() {
    return template.getDatabaseClient()
        .sql(DEFAULT_FIND_STATEMENT)
        .map(this::mapRow)
        .all();
  }

  private Mono<ClientDetails> selectClientDetailsSql(String clientId) {
    return template.getDatabaseClient()
        .sql(DEFAULT_SELECT_STATEMENT)
        .bind("clientId", clientId)
        .map(this::mapRow)
        .one();
  }

  private Mono<Void> insertClientDetailsSql(ClientDetails clientDetails) {
    GenericExecuteSpec sql = template.getDatabaseClient().sql(DEFAULT_INSERT_STATEMENT);
    Object[] fields = getFields(clientDetails);
    for (int i = 0; i < fields.length; i++) {
      sql = sql.bind(i, fields[i]);
    }
    return sql.then();
  }

  private Mono<Void> updateClientDetailsSql(ClientDetails clientDetails) {
    GenericExecuteSpec sql = template.getDatabaseClient().sql(DEFAULT_UPDATE_STATEMENT);
    Object[] fields = getFieldsForUpdate(clientDetails);
    for (int i = 0; i < fields.length; i++) {
      sql = sql.bind(i, fields[i]);
    }
    return sql.then();
  }

  private Mono<Void> updateClientSecretSql(String clientId, String secret) {
    return template.getDatabaseClient().sql(DEFAULT_UPDATE_SECRET_STATEMENT)
        .bind("clientSecret", passwordEncoder.encode(secret))
        .bind("clientId", clientId)
        .then();
  }

  private Mono<Void> deleteClientDetailsSql(String clientId) {
    return template.getDatabaseClient().sql(DEFAULT_DELETE_STATEMENT)
        .bind("clientId", clientId)
        .then();
  }

  private Object[] getFields(ClientDetails clientDetails) {
    Object[] fieldsForUpdate = getFieldsForUpdate(clientDetails);
    Object[] fields = new Object[fieldsForUpdate.length + 1];
    System.arraycopy(fieldsForUpdate, 0, fields, 1, fieldsForUpdate.length);
    fields[0] = clientDetails.getClientSecret() != null ? passwordEncoder.encode(clientDetails.getClientSecret())
        : null;
    return fields;
  }

  private Object[] getFieldsForUpdate(ClientDetails clientDetails) {
    String json = null;
    try {
      json = mapper.writeValueAsString(clientDetails.getAdditionalInformation());
    } catch (Exception e) {
      logger.warn("Could not serialize additional information: " + clientDetails, e);
    }
    return new Object[]{
        clientDetails.getResourceIds() != null ? StringUtils.collectionToCommaDelimitedString(clientDetails
            .getResourceIds()) : null,
        clientDetails.getScope() != null ? StringUtils.collectionToCommaDelimitedString(clientDetails
            .getScope()) : null,
        clientDetails.getAuthorizedGrantTypes() != null ? StringUtils
            .collectionToCommaDelimitedString(clientDetails.getAuthorizedGrantTypes()) : null,
        clientDetails.getRegisteredRedirectUri() != null ? StringUtils
            .collectionToCommaDelimitedString(clientDetails.getRegisteredRedirectUri()) : null,
        clientDetails.getAuthorities() != null ? StringUtils.collectionToCommaDelimitedString(clientDetails
            .getAuthorities()) : null, clientDetails.getAccessTokenValiditySeconds(),
        clientDetails.getRefreshTokenValiditySeconds(), json, getAutoApproveScopes(clientDetails),
        clientDetails.getClientId()};
  }


  private ClientDetails mapRow(Row row) {
    BaseClientDetails details = new BaseClientDetails((String) row.get(0), (String) row.get(2), (String) row.get(3),
        (String) row.get(4), (String) row.get(6), (String) row.get(5));
    details.setClientSecret((String) row.get(1));
    if (row.get(8) != null) {
      details.setAccessTokenValiditySeconds((Integer) row.get(7));
    }
    if (row.get(9) != null) {
      details.setRefreshTokenValiditySeconds((Integer) row.get(8));
    }
    String json = (String) row.get(9);
    if (json != null) {
      try {
        Map<String, Object> additionalInformation = mapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        details.setAdditionalInformation(additionalInformation);
      } catch (Exception e) {
        logger.warn("Could not decode JSON for additional information: " + details, e);
      }
    }
    String scopes = (String) row.get(10);
    if (scopes != null) {
      details.setAutoApproveScopes(StringUtils.commaDelimitedListToSet(scopes));
    }
    return details;
  }

  private String getAutoApproveScopes(ClientDetails clientDetails) {
    if (clientDetails.isAutoApprove("true")) {
      return "true"; // all scopes autoapproved
    }
    Set<String> scopes = new HashSet<>();
    for (String scope : clientDetails.getScope()) {
      if (clientDetails.isAutoApprove(scope)) {
        scopes.add(scope);
      }
    }
    return StringUtils.collectionToCommaDelimitedString(scopes);
  }
}
