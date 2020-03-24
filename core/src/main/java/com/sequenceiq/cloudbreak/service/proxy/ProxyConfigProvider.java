package com.sequenceiq.cloudbreak.service.proxy;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

@Service
public class ProxyConfigProvider {

    public static final String PROXY_KEY = "proxy";

    public static final String PROXY_SLS_PATH = "/proxy/proxy.sls";

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    public void decoratePillarWithProxyDataIfNeeded(Map<String, SaltPillarProperties> servicePillar, Cluster cluster) {
        Optional<ProxyConfig> proxyConfig = proxyConfigDtoService.getByCrnWithEnvironmentFallback(cluster.getProxyConfigCrn(), cluster.getEnvironmentCrn());
        proxyConfig.ifPresent(pc -> {
            Map<String, Object> proxy = new HashMap<>();
            proxy.put("host", pc.getServerHost());
            proxy.put("port", pc.getServerPort());
            proxy.put("protocol", pc.getProtocol());
            if (StringUtils.isNotBlank(pc.getUserName()) && StringUtils.isNotBlank(pc.getPassword())) {
                proxy.put("user", pc.getUserName());
                proxy.put("password", pc.getPassword());
            }
            servicePillar.put(PROXY_KEY, new SaltPillarProperties(PROXY_SLS_PATH, singletonMap(PROXY_KEY, proxy)));
        });
    }
}
