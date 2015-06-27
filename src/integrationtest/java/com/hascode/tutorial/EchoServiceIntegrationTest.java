package com.hascode.tutorial;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.hascode.tutorial.junit.categories.IntegrationTest;

@Category(IntegrationTest.class)
public class EchoServiceIntegrationTest {

    private static final String TEST_MESSAGE = "Integration Test";

    @Test
    public void emptyTest() throws Exception {
        EchoService echoService = new EchoService();
        echoService.setVersion(1);
        String message = echoService.echo(TEST_MESSAGE);
        assertEquals(message, TEST_MESSAGE);
    }

}
