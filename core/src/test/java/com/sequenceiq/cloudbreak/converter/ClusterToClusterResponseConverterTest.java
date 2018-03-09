package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.RDSConfigJson;
import com.sequenceiq.cloudbreak.api.model.RDSConfigRequest;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.blueprint.validation.StackServiceComponentDescriptor;
import com.sequenceiq.cloudbreak.blueprint.validation.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.converter.mapper.ProxyConfigMapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariViewProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.util.StackUtil;

public class ClusterToClusterResponseConverterTest extends AbstractEntityConverterTest<Cluster> {

    @InjectMocks
    private ClusterToClusterResponseConverter underTest;

    @Mock
    private BlueprintValidator blueprintValidator;

    @Mock
    private StackServiceComponentDescriptors stackServiceComponentDescs;

    @Mock
    private ConversionService conversionService;

    @Mock
    private JsonNode jsonNode;

    @Mock
    private JsonNode nameJsonNode;

    @Mock
    private Iterator<JsonNode> mockIterator;

    @Mock
    private Map<String, HostGroup> hostGroupMap;

    @Mock
    private HostGroup hostGroup;

    @Mock
    private InstanceGroup instanceGroup;

    @Mock
    private InstanceMetaData instanceMetaData;

    @Mock
    private Iterator<JsonNode> mockComponentIterator;

    @Mock
    private AmbariViewProvider ambariViewProvider;

    @Mock
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private ClusterComponentConfigProvider componentConfigProvider;

    @Mock
    private ProxyConfigMapper proxyConfigMapper;

    private StackServiceComponentDescriptor stackServiceComponentDescriptor;

    @Before
    public void setUp() throws CloudbreakException {
        underTest = new ClusterToClusterResponseConverter();
        MockitoAnnotations.initMocks(this);
        given(orchestratorTypeResolver.resolveType(any(Orchestrator.class))).willReturn(OrchestratorType.HOST);
        given(rdsConfigService.findByClusterId(anyString(), anyString(), anyLong())).willReturn(new HashSet<>());
        stackServiceComponentDescriptor = createStackServiceComponentDescriptor();
    }

    @Test
    public void testConvert() throws IOException {
        // GIVEN
        mockAll();
        getSource().setConfigStrategy(ConfigStrategy.NEVER_APPLY);
        given(stackUtil.extractAmbariIp(any(Stack.class))).willReturn("10.0.0.1");
        // WHEN
        ClusterResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(1L, (long) result.getId());
        assertAllFieldsNotNull(result, Lists.newArrayList("cluster", "ambariStackDetails", "rdsConfigId", "blueprintCustomProperties",
                "blueprint", "rdsConfigs", "ldapConfig", "exposedKnoxServices", "customContainers",
                "ambariRepoDetailsJson", "ambariDatabaseDetails", "creationFinished", "kerberosResponse"));
    }

    @Test
    public void testConvertWithoutUpSinceField() throws IOException {
        // GIVEN
        mockAll();
        getSource().setUpSince(null);
        // WHEN
        ClusterResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(0L, (long) result.getMinutesUp());
    }

    @Test
    public void testConvertWithoutMasterComponent() throws IOException {
        // GIVEN
        mockAll();
        given(stackServiceComponentDescs.get(anyString())).willReturn(new StackServiceComponentDescriptor("dummy", "dummy", 1, 1));
        // WHEN
        ClusterResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(1L, (long) result.getId());
    }

    @Test
    public void testConvertWhenValidatorThrowException() throws IOException {
        // GIVEN
        given(blueprintValidator.getHostGroupNode(any(Blueprint.class))).willThrow(new IOException("error"));
        // WHEN
        underTest.convert(getSource());
        // THEN
        verify(blueprintValidator, times(0)).createHostGroupMap(anySet());

    }

    @Override
    public Cluster createSource() {
        Stack stack = TestUtil.stack();
        Blueprint blueprint = TestUtil.blueprint();
        Cluster cluster = TestUtil.cluster(blueprint, stack, 1L);
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setName("test");
        cluster.setProxyConfig(proxyConfig);
        stack.setCluster(cluster);
        return cluster;
    }

    private void mockAll() throws IOException {
        when(ambariViewProvider.provideViewInformation(any(AmbariClient.class), any(Cluster.class))).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[1];
        });
        given(blueprintValidator.getHostGroupNode(any(Blueprint.class))).willReturn(jsonNode);
        given(jsonNode.iterator()).willReturn(mockIterator);
        given(mockIterator.hasNext()).willReturn(true).willReturn(false);
        given(mockIterator.next()).willReturn(jsonNode);
        given(conversionService.convert(any(RDSConfig.class), eq(RDSConfigJson.class))).willReturn(new RDSConfigRequest());
        given(blueprintValidator.getHostGroupName(jsonNode)).willReturn("slave_1");
        given(blueprintValidator.createHostGroupMap(any(Set.class))).willReturn(hostGroupMap);
        given(hostGroupMap.get("slave_1")).willReturn(hostGroup);
        given(instanceGroup.getInstanceMetaData()).willReturn(Sets.newHashSet(instanceMetaData));
        given(blueprintValidator.getComponentsNode(jsonNode)).willReturn(nameJsonNode);
        given(nameJsonNode.iterator()).willReturn(mockComponentIterator);
        given(mockComponentIterator.hasNext()).willReturn(true).willReturn(false);
        given(mockComponentIterator.next()).willReturn(nameJsonNode);
        given(nameJsonNode.get(anyString())).willReturn(nameJsonNode);
        given(nameJsonNode.asText()).willReturn("dummyName");
        given(componentConfigProvider.getAmbariRepo(any(Set.class))).willReturn(null);
        given(stackServiceComponentDescs.get(anyString())).willReturn(stackServiceComponentDescriptor);
        ProxyConfigResponse proxyConfigResponse = new ProxyConfigResponse();
        proxyConfigResponse.setId(1L);
        given(proxyConfigMapper.mapEntityToResponse(any(ProxyConfig.class))).willReturn(proxyConfigResponse);

    }

    private StackServiceComponentDescriptor createStackServiceComponentDescriptor() {
        return new StackServiceComponentDescriptor("ELASTIC_SEARCH", "MASTER", 1, 1);
    }
}
