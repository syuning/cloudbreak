package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import java.util.Arrays;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig;

@Component
public class AmbariClusterStatusUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterStatusUpdater.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private AmbariClusterStatusFactory clusterStatusFactory;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    private enum Msg {
        AMBARI_CLUSTER_COULD_NOT_SYNC("ambari.cluster.could.not.sync"),
        AMBARI_CLUSTER_SYNCHRONIZED("ambari.cluster.synchronized");

        private String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }


    public void updateClusterStatus(Stack stack, Cluster cluster) throws CloudbreakSecuritySetupException {
        if (isStackOrClusterStatusInvalid(stack, cluster)) {
            String msg = cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_COULD_NOT_SYNC.code(), Arrays.asList(stack.getStatus(),
                    cluster == null ? "" : cluster.getStatus()));
            LOGGER.warn(msg);
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), stack.getStatus().name(), msg);
        } else {
            Long stackId = stack.getId();
            String blueprintName = cluster != null ? cluster.getBlueprint().getBlueprintName() : null;
            HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stackId, cluster.getAmbariIp());
            if (cluster.getAmbariIp() != null) {
                clusterService.updateClusterMetadata(stackId);
                ClusterStatus clusterStatus = clusterStatusFactory.createClusterStatus(ambariClientProvider.getAmbariClient(
                        clientConfig, stack.getGatewayPort(), cluster.getUserName(), cluster.getPassword()), blueprintName);
                updateClusterStatus(stackId, stack.getStatus(), cluster, clusterStatus);
            }
        }
    }

    private boolean isStackOrClusterStatusInvalid(Stack stack, Cluster cluster) {
        return stack.isStackInDeletionPhase()
                || stack.isStackInStopPhase()
                || stack.isModificationInProgress()
                || cluster == null
                || cluster.isModificationInProgress();
    }

    private void updateClusterStatus(Long stackId, Status stackStatus, Cluster cluster, ClusterStatus ambariClusterStatus) {
        Status statusInEvent = stackStatus;
        String statusReason = ambariClusterStatus.getStatusReason();
        if (isUpdateEnabled(ambariClusterStatus)) {
            if (updateClusterStatus(stackId, cluster, ambariClusterStatus.getClusterStatus())) {
                statusInEvent = ambariClusterStatus.getStackStatus();
                statusReason = ambariClusterStatus.getStatusReason();
            } else {
                statusReason = "The cluster's state is up to date.";
            }
        }
        cloudbreakEventService.fireCloudbreakEvent(stackId, statusInEvent.name(), cloudbreakMessagesService.getMessage(Msg.AMBARI_CLUSTER_SYNCHRONIZED.code(),
                Arrays.asList(statusReason)));
    }

    private boolean isUpdateEnabled(ClusterStatus clusterStatus) {
        return clusterStatus == ClusterStatus.STARTED || clusterStatus == ClusterStatus.INSTALLED;
    }

    private boolean updateClusterStatus(Long stackId, Cluster cluster, Status newClusterStatus) {
        boolean result = false;
        if (cluster.getStatus() != newClusterStatus) {
            LOGGER.info("Cluster {} status is updated from {} to {}", cluster.getId(), cluster.getStatus(), newClusterStatus);
            clusterService.updateClusterStatusByStackId(stackId, newClusterStatus);
            result = true;
        } else {
            LOGGER.info("Cluster {} status hasn't changed: {}", cluster.getId(), cluster.getStatus());
        }
        return result;
    }
}
