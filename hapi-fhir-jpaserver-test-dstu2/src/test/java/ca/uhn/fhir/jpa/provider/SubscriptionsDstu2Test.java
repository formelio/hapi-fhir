package ca.uhn.fhir.jpa.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.jpa.util.SubscriptionsRequireManualActivationInterceptorDstu2;
import ca.uhn.fhir.model.dstu2.resource.Subscription;
import ca.uhn.fhir.model.dstu2.valueset.SubscriptionChannelTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.SubscriptionStatusEnum;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class SubscriptionsDstu2Test extends BaseResourceProviderDstu2Test {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(SubscriptionsDstu2Test.class);
	private SubscriptionsRequireManualActivationInterceptorDstu2 myInterceptor;

	@BeforeEach
	public void beforeCreateInterceptor() {
		myInterceptor = new SubscriptionsRequireManualActivationInterceptorDstu2();
		myInterceptor.setDao(mySubscriptionDao);
		myInterceptorRegistry.registerInterceptor(myInterceptor);
	}

	@AfterEach
	public void afterDestroyInterceptor() {
		myInterceptorRegistry.unregisterInterceptor(myInterceptor);
	}

	@BeforeEach
	public void beforeDisableResultReuse() {
		myStorageSettings.setReuseCachedSearchResultsForMillis(null);
	}

	@BeforeEach
	public void beforeEnableScheduling() {
		myStorageSettings.setSchedulingDisabled(false);
	}


	@Test
	public void testCreateInvalidNoStatus() {
		Subscription subs = new Subscription();
		subs.getChannel().setType(SubscriptionChannelTypeEnum.REST_HOOK);
		subs.setCriteria("Observation?identifier=123");
		subs.getChannel().setEndpoint("http://localhost");
		try {
			myClient.create().resource(subs).execute();
			fail("");
		} catch (UnprocessableEntityException e) {
			assertThat(e.getMessage()).contains("Subscription.status must be populated on this server");
		}

		subs.setId("ABC");
		try {
			myClient.update().resource(subs).execute();
			fail("");
		} catch (UnprocessableEntityException e) {
			assertThat(e.getMessage()).contains("Subscription.status must be populated on this server");
		}

		subs.setStatus(SubscriptionStatusEnum.REQUESTED);
		myClient.update().resource(subs).execute();
	}

	@Test
	public void testCreateInvalidWrongStatus() {
		Subscription subs = new Subscription();
		subs.getChannel().setType(SubscriptionChannelTypeEnum.REST_HOOK);
		subs.getChannel().setPayload("application/fhir+json");
		subs.getChannel().setEndpoint("http://foo");
		subs.setStatus(SubscriptionStatusEnum.ACTIVE);
		subs.setCriteria("Observation?identifier=123");
		try {
			myClient.create().resource(subs).execute();
			fail("");
		} catch (UnprocessableEntityException e) {
			assertEquals("HTTP 422 Unprocessable Entity: " + Msg.code(804) + "Subscription.status must be 'off' or 'requested' on a newly created subscription", e.getMessage());
		}

		subs.setId("ABC");
		try {
			myClient.update().resource(subs).execute();
			fail("");
		} catch (UnprocessableEntityException e) {
			assertEquals("HTTP 422 Unprocessable Entity: " + Msg.code(804) + "Subscription.status must be 'off' or 'requested' on a newly created subscription", e.getMessage());
		}
	}

	@Test
	public void testCreateWithPopulatedButInvalidStatue() {
		Subscription subs = new Subscription();
		subs.getChannel().setType(SubscriptionChannelTypeEnum.WEBSOCKET);
		subs.getChannel().setEndpoint("http://localhost");
		subs.setCriteria("Observation?identifier=123");
		subs.getStatusElement().setValue("aaaaa");

		try {
			myClient.create().resource(subs).execute();
			fail("");
		} catch (UnprocessableEntityException e) {
			assertThat(e.getMessage()).containsAnyOf("invalid value aaaaa", "Unknown SubscriptionStatus code 'aaaaa'");
		}
	}


	@Test
	public void testUpdateFails() {
		Subscription subs = new Subscription();
		subs.getChannel().setType(SubscriptionChannelTypeEnum.REST_HOOK);
		subs.setStatus(SubscriptionStatusEnum.REQUESTED);
		subs.setCriteria("Observation?identifier=123");
		subs.getChannel().setEndpoint("http://example.com");
		IIdType id = myClient.create().resource(subs).execute().getId().toUnqualifiedVersionless();

		subs.setId(id);

		try {
			subs.setStatus(SubscriptionStatusEnum.ACTIVE);
			myClient.update().resource(subs).execute();
			fail("");
		} catch (UnprocessableEntityException e) {
			assertEquals("HTTP 422 Unprocessable Entity: " + Msg.code(802) + "Subscription.status can not be changed from 'requested' to 'active'", e.getMessage());
		}

		try {
			subs.setStatus((SubscriptionStatusEnum) null);
			myClient.update().resource(subs).execute();
			fail("");
		} catch (UnprocessableEntityException e) {
			assertThat(e.getMessage()).contains("Subscription.status must be populated on this server");
		}

		subs.setStatus(SubscriptionStatusEnum.OFF);
	}

	@Test
	public void testUpdateToInvalidStatus() {
		Subscription subs = new Subscription();
		subs.getChannel().setType(SubscriptionChannelTypeEnum.REST_HOOK);
		subs.setCriteria("Observation?identifier=123");
		subs.getChannel().setEndpoint("http://localhost");
		subs.setStatus(SubscriptionStatusEnum.REQUESTED);
		IIdType id = myClient.create().resource(subs).execute().getId();
		subs.setId(id);

		try {
			subs.setStatus(SubscriptionStatusEnum.ACTIVE);
			myClient.update().resource(subs).execute();
			fail("");
		} catch (UnprocessableEntityException e) {
			assertEquals("HTTP 422 Unprocessable Entity: " + Msg.code(802) + "Subscription.status can not be changed from 'requested' to 'active'", e.getMessage());
		}

		try {
			subs.setStatus((SubscriptionStatusEnum) null);
			myClient.update().resource(subs).execute();
			fail("");
		} catch (UnprocessableEntityException e) {
			assertThat(e.getMessage()).contains("Subscription.status must be populated on this server");
		}

		subs.setStatus(SubscriptionStatusEnum.OFF);
		myClient.update().resource(subs).execute();
	}

	public class BaseSocket {
		protected String myError;
		protected boolean myGotBound;
		protected int myPingCount;
		protected String mySubsId;

	}

	/**
	 * Basic Echo Client Socket
	 */
	@WebSocket()
	public class SimpleEchoSocket extends BaseSocket {

		@SuppressWarnings("unused")
		private Session session;

		public SimpleEchoSocket(String theSubsId) {
			mySubsId = theSubsId;
		}

		@OnWebSocketOpen
		public void onConnect(Session session) {
			ourLog.info("Got connect: {}", session);
			this.session = session;
			try {
				String sending = "bind " + mySubsId;
				ourLog.info("Sending: {}", sending);
				session.sendText(sending, null);
			} catch (Throwable t) {
				ourLog.error("Failure", t);
			}
		}

		@OnWebSocketMessage
		public void onMessage(String theMsg) {
			ourLog.info("Got msg: {}", theMsg);
			if (theMsg.equals("bound " + mySubsId)) {
				myGotBound = true;
			} else if (myGotBound && theMsg.startsWith("ping " + mySubsId)) {
				myPingCount++;
			} else {
				myError = "Unexpected message: " + theMsg;
			}
		}
	}

	/**
	 * Basic Echo Client Socket
	 */
	@WebSocket()
	public class DynamicEchoSocket extends BaseSocket {

		private List<IBaseResource> myReceived = new ArrayList<IBaseResource>();
		@SuppressWarnings("unused")
		private Session session;
		private String myCriteria;
		private EncodingEnum myEncoding;

		public DynamicEchoSocket(String theCriteria, EncodingEnum theEncoding) {
			myCriteria = theCriteria;
			myEncoding = theEncoding;
		}

		@OnWebSocketOpen
		public void onConnect(Session session) {
			ourLog.info("Got connect: {}", session);
			this.session = session;
			try {
				String sending = "bind " + myCriteria;
				ourLog.info("Sending: {}", sending);
				session.sendText(sending, null);
			} catch (Throwable t) {
				ourLog.error("Failure", t);
			}
		}

		@OnWebSocketMessage
		public void onMessage(String theMsg) {
			ourLog.info("Got msg: {}", theMsg);
			if (theMsg.startsWith("bound ")) {
				myGotBound = true;
				mySubsId = (theMsg.substring("bound ".length()));
				myPingCount++;
			} else if (myGotBound && theMsg.startsWith("add " + mySubsId + "\n")) {
				String text = theMsg.substring(("add " + mySubsId + "\n").length());
				IBaseResource res = myEncoding.newParser(myFhirContext).parseResource(text);
				myReceived.add(res);
				myPingCount++;
			} else {
				myError = "Unexpected message: " + theMsg;
			}
		}
	}
}
