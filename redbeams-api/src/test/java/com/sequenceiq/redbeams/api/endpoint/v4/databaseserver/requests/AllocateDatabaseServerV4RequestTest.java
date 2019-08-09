package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDBStackV4Parameters;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4StackRequest;

public class AllocateDatabaseServerV4RequestTest {

    private AllocateDatabaseServerV4Request request;

    @Before
    public void setUp() throws Exception {
        request = new AllocateDatabaseServerV4Request();
    }

    @Test
    public void testGettersAndSetters() {
        request.setName("myallocation");
        assertEquals("myallocation", request.getName());

        request.setEnvironmentCrn("myenv");
        assertEquals("myenv", request.getEnvironmentCrn());

        NetworkV4StackRequest network = new NetworkV4StackRequest();
        request.setNetwork(network);
        assertEquals(network, request.getNetwork());

        DatabaseServerV4StackRequest server = new DatabaseServerV4StackRequest();
        request.setDatabaseServer(server);
        assertEquals(server, request.getDatabaseServer());
    }

    @Test
    public void testAwsParameters() {
        assertNull(request.getAws());

        AwsDBStackV4Parameters parameters = request.createAws();
        assertNotNull(parameters);

        parameters = new AwsDBStackV4Parameters();
        request.setAws(parameters);
        assertEquals(parameters, request.createAws());
        assertEquals(parameters, request.getAws());
    }

}
