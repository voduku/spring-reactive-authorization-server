package authorization.endpoint;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.result.condition.NameValueExpression;
import org.springframework.web.reactive.result.condition.ParamsRequestCondition;
import org.springframework.web.reactive.result.condition.PatternsRequestCondition;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.view.UrlBasedViewResolver;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * @author VuDo
 * @since 2/14/2021
 */
public class FrameworkEndpointReactiveHandlerMapping extends RequestMappingHandlerMapping {

  private static final String REDIRECT = UrlBasedViewResolver.REDIRECT_URL_PREFIX;

  private final PathPatternParser parser = new PathPatternParser();
  private final Set<String> paths = new HashSet<>();
  private Map<String, String> mappings = new HashMap<>();
  private String approvalParameter = OAuth2Utils.USER_OAUTH_APPROVAL;
  private String prefix;

  public FrameworkEndpointReactiveHandlerMapping() {
    // Make sure user-supplied mappings take precedence by default (except the resource mapping)
    setOrder(Ordered.LOWEST_PRECEDENCE - 2);
  }

  /**
   * @param prefix the prefix to set
   */
  public void setPrefix(String prefix) {
    if (!StringUtils.hasText(prefix)) {
      prefix = "";
    } else {
      while (prefix.endsWith("/")) {
        prefix = prefix.substring(0, prefix.lastIndexOf("/"));
      }
    }
    this.prefix = prefix;
  }

  /**
   * Custom mappings for framework endpoint paths. The keys in the map are the default framework endpoint path, e.g. "/oauth/authorize", and the values are the
   * desired runtime paths.
   *
   * @param patternMap the mappings to set
   */
  public void setMappings(Map<String, String> patternMap) {
    this.mappings = new HashMap<>(patternMap);
    for (String key : mappings.keySet()) {
      String result = mappings.get(key);
      if (result.startsWith(REDIRECT)) {
        result = result.substring(REDIRECT.length());
      }
      mappings.put(key, result);
    }
  }

  /**
   * @return the mapping from default endpoint paths to custom ones (or the default if no customization is known)
   */
  public String getServerPath(String defaultPath) {
    return (prefix == null ? "" : prefix) + getPath(defaultPath);
  }

  /**
   * @return the mapping from default endpoint paths to custom ones (or the default if no customization is known)
   */
  public PathPattern getPath(String defaultPath) {
    String result = defaultPath;
    if (mappings.containsKey(defaultPath)) {
      result = mappings.get(defaultPath);
    }
    return parser.parse(result);
  }

  public Set<String> getPaths() {
    return paths;
  }

  /**
   * The name of the request parameter that distinguishes a call to approve an authorization. Default is {@link OAuth2Utils#USER_OAUTH_APPROVAL}.
   *
   * @param approvalParameter the approvalParameter to set
   */
  public void setApprovalParameter(String approvalParameter) {
    this.approvalParameter = approvalParameter;
  }

  /**
   * Detects @ReactiveFrameworkEndpoint annotations in handler beans.
   *
   * @see RequestMappingHandlerMapping#isHandler(java.lang.Class)
   */
  @Override
  protected boolean isHandler(Class<?> beanType) {
    return AnnotationUtils.findAnnotation(beanType, ReactiveFrameworkEndpoint.class) != null;
  }

  @Override
  protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {

    RequestMappingInfo defaultMapping = super.getMappingForMethod(method, handlerType);
    if (defaultMapping == null) {
      return null;
    }

    Set<PathPattern> defaultPatterns = defaultMapping.getPatternsCondition().getPatterns();
    PathPattern[] patterns = new PathPattern[defaultPatterns.size()];

    int i = 0;
    for (PathPattern pattern : defaultPatterns) {
      patterns[i] = getPath(pattern.getPatternString());
      paths.add(pattern.getPatternString());
      i++;
    }
    PatternsRequestCondition patternsInfo = new PatternsRequestCondition(patterns);

    ParamsRequestCondition paramsInfo = defaultMapping.getParamsCondition();
    if (!approvalParameter.equals(OAuth2Utils.USER_OAUTH_APPROVAL) && defaultPatterns.contains(parser.parse("/oauth/authorize"))) {
      String[] params = new String[paramsInfo.getExpressions().size()];
      Set<NameValueExpression<String>> expressions = paramsInfo.getExpressions();
      i = 0;
      for (NameValueExpression<String> expression : expressions) {
        String param = expression.toString();
        if (OAuth2Utils.USER_OAUTH_APPROVAL.equals(param)) {
          params[i] = approvalParameter;
        } else {
          params[i] = param;
        }
        i++;
      }
      paramsInfo = new ParamsRequestCondition(params);
    }

    RequestMappingInfo mapping = new RequestMappingInfo(patternsInfo, defaultMapping.getMethodsCondition(),
        paramsInfo, defaultMapping.getHeadersCondition(), defaultMapping.getConsumesCondition(),
        defaultMapping.getProducesCondition(), defaultMapping.getCustomCondition());
    return mapping;

  }
}
