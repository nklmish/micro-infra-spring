package com.ofg.infrastructure.discovery;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ofg.infrastructure.discovery.util.CollectionUtils;
import com.ofg.infrastructure.discovery.util.LoadBalancerType;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ofg.infrastructure.discovery.ServiceConfigurationProperties.PATH;

class JsonToMicroserviceConfigurationConverter {

    List<MicroserviceConfiguration.Dependency> convertJsonToDependencies(JSONObject dependenciesAsJson) {
        return new ArrayList(Collections2.transform(dependenciesAsJson.entrySet(), new Function<Map.Entry<String, JSONObject>, MicroserviceConfiguration.Dependency>() {
            @Override
            public MicroserviceConfiguration.Dependency apply(Map.Entry<String, JSONObject> input) {
                String alias = input.getKey();
                String path = input.getValue().getString(ServiceConfigurationProperties.PATH);
                boolean required = getPropertyOrDefault(input.getValue(), ServiceConfigurationProperties.REQUIRED, Boolean.FALSE);
                String loadBalancerName = getPropertyOrDefault(input.getValue(), ServiceConfigurationProperties.LOAD_BALANCER, "");
                LoadBalancerType loadBalancerType = LoadBalancerType.fromName(loadBalancerName);
                String contentTypeTemplate = getPropertyOrDefault(input.getValue(), ServiceConfigurationProperties.CONTENT_TYPE_TEMPLATE, StringUtils.EMPTY);
                String version = getPropertyOrDefault(input.getValue(), ServiceConfigurationProperties.VERSION, StringUtils.EMPTY);
                JSONObject headers = (JSONObject) input.getValue().get(ServiceConfigurationProperties.HEADERS);
                Map<String, String> headersAsMap = Maps.newHashMap();
                if (headers != null) {
                    for (Object entry : headers.entrySet()) {
                        Map.Entry<String, Object> headerEntry = (Map.Entry<String, Object>) entry;
                        headersAsMap.put(headerEntry.getKey(), String.valueOf(headerEntry.getValue()));
                    }
                }
                return new MicroserviceConfiguration.Dependency(new ServiceAlias(alias), new ServicePath(path), required, loadBalancerType, contentTypeTemplate, version, headersAsMap);
            }
        }));
    }

    private static <T> T getPropertyOrDefault(JSONObject jsonObject, String propertyName, T defaultValue) {
        return jsonObject.has(propertyName) ? (T) jsonObject.get(propertyName) : defaultValue;
    }

    static void convertFlatDependenciesToMapFormat(JSONObject serviceMetadata) {
        JSONObject dependenciesAsJson = getDependenciesAsJsonObject(serviceMetadata);
        if (dependenciesAsJson == null) {
            return;
        }
        for (Object dependency : dependenciesAsJson.entrySet()) {
            Map.Entry<String, Object> entryDependency = (Map.Entry<String, Object>) dependency;
            if (entryDependency.getValue() instanceof String) {
                dependenciesAsJson.put(entryDependency.getKey(), ImmutableMap.of(PATH, entryDependency.getValue()));
            }
        }
    }

    private static JSONObject getDependenciesAsJsonObject(JSONObject serviceMetadata) {
        Object dependencies = serviceMetadata.get(ServiceConfigurationProperties.DEPENDENCIES);
        JSONObject dependenciesAsJson = (JSONObject) dependencies;
        return dependenciesAsJson;
    }


    static void setDefaultsForMissingOptionalElements(JSONObject serviceMetadata) {
        Object dependencies = serviceMetadata.get(ServiceConfigurationProperties.DEPENDENCIES);
        if (dependencies == null) {
            serviceMetadata.put(ServiceConfigurationProperties.DEPENDENCIES, new JSONObject());
        }

    }

}
