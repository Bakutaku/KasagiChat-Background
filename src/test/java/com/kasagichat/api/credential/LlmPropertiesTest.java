package com.kasagichat.api.credential;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.kasagichat.api.credential.LlmProperties.DemoProperties;
import com.kasagichat.api.credential.model.enums.LlmProvider;

class LlmPropertiesTest {

    @Test
    void iamAuthRequiresModelButNotApiKey() {
        assertTrue(properties(new DemoProperties(null, null, "jp.anthropic.model", "", true)).isDemoConfigured());
        assertFalse(properties(new DemoProperties(null, null, "", "", true)).isDemoConfigured());
    }

    @Test
    void apiKeyAuthStillRequiresProviderAndKey() {
        assertFalse(properties(new DemoProperties(LlmProvider.DEMO, null, "local-model", "", false)).isDemoConfigured());
        assertTrue(properties(new DemoProperties(LlmProvider.DEMO, null, "local-model", "test-key", false)).isDemoConfigured());
    }

    private static LlmProperties properties(DemoProperties demo) {
        return new LlmProperties("", null, null, demo);
    }
}
