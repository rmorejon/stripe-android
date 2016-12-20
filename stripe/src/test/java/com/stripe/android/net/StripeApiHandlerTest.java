package com.stripe.android.net;

import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.model.Card;
import com.stripe.android.util.StripeNetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for {@link StripeApiHandler}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class StripeApiHandlerTest {

    @Test
    public void testGetApiUrl() {
        String tokensApi = StripeApiHandler.getApiUrl();
        assertEquals("https://api.stripe.com/v1/tokens", tokensApi);
    }

    @Test
    public void testGetRequestTokenApiUrl() {
        String tokenId = "tok_sample";
        String requestApi = StripeApiHandler.getRetrieveTokenApiUrl(tokenId);
        assertEquals("https://api.stripe.com/v1/tokens/" + tokenId, requestApi);
    }

    @Test
    public void getHeaders_withAllRequestOptions_properlyMapsRequestOptions() {
        String fakePublicKey = "fake_public_key";
        String idempotencyKey = "idempotency_rules";
        String apiVersion = "2011-11-11";
        RequestOptions requestOptions = RequestOptions.builder(fakePublicKey)
                .setIdempotencyKey(idempotencyKey)
                .setApiVersion(apiVersion)
                .build();
        Map<String, String> headerMap = StripeApiHandler.getHeaders(requestOptions);

        assertNotNull(headerMap);
        assertEquals("Bearer " + fakePublicKey, headerMap.get("Authorization"));
        assertEquals(idempotencyKey, headerMap.get("Idempotency-Key"));
        assertEquals(apiVersion, headerMap.get("Stripe-Version"));
    }

    @Test
    public void getHeaders_withOnlyRequiredOptions_doesNotAddEmptyOptions() {
        RequestOptions requestOptions = RequestOptions.builder("some_key").build();
        Map<String, String> headerMap = StripeApiHandler.getHeaders(requestOptions);

        assertNotNull(headerMap);
        assertFalse(headerMap.containsKey("Idempotency-Key"));
        assertFalse(headerMap.containsKey("Stripe-Version"));
        assertTrue(headerMap.containsKey("Authorization"));
    }

    @Test
    public void getHeaders_containsPropertyMapValues() {
        RequestOptions requestOptions = RequestOptions.builder("some_key").build();
        Map<String, String> headerMap = StripeApiHandler.getHeaders(requestOptions);

        assertNotNull(headerMap);
        String userAgentRawString = headerMap.get("X-Stripe-Client-User-Agent");
        try {
            JSONObject mapObject = new JSONObject(userAgentRawString);
            assertEquals("3.5.0", mapObject.getString("bindings.version"));
            assertEquals("Java", mapObject.getString("lang"));
            assertEquals("Stripe", mapObject.getString("publisher"));
        } catch (JSONException jsonException) {
            fail("Failed to get a parsable JsonObject for the user agent.");
        }
    }

    @Test
    public void getHeaders_correctlyAddsExpectedAdditionalParameters() {
        RequestOptions requestOptions = RequestOptions.builder("some_key").build();
        Map<String, String> headerMap = StripeApiHandler.getHeaders(requestOptions);
        assertNotNull(headerMap);

        assertEquals("Stripe/v1 JavaBindings/3.5.0", headerMap.get("User-Agent"));
        assertEquals("application/json", headerMap.get("Accept"));
        assertEquals("UTF-8", headerMap.get("Accept-Charset"));
    }

    @Test
    public void createQuery_withCardData_createsProperQueryString() {
        Card card = new Card.Builder("4242424242424242", 8, 2019, "123").build();
        Map<String, Object> cardMap = StripeNetworkUtils.hashMapFromCard(card);
        String expectedValue = "card%5Bnumber%5D=4242424242424242&card%5Bcvc%5D=123&card%5" +
                "Bexp_month%5D=8&card%5Bexp_year%5D=2019";
        try {
            String query = StripeApiHandler.createQuery(cardMap);
            assertEquals(expectedValue, query);
        } catch (UnsupportedEncodingException unsupportedCodingException) {
            fail("Encoding error with card object");
        } catch (InvalidRequestException invalidRequest) {
            fail("Invalid request error when encoding card query: "
                    + invalidRequest.getLocalizedMessage());
        }
    }
}