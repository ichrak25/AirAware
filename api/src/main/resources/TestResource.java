package tn.airaware.api.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.airaware.api.mqtt.MqttConnection;
import tn.airaware.api.mqtt.MqttListenerService;
import tn.airaware.api.mqtt.MqttPublisherService;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TestResource {
    
    private static final Logger logger = Logger.getLogger(TestResource.class.getName());
    
    @Inject
    private MqttConnection mqttConnection;
    
    @Inject
    private MqttListenerService mqttListenerService;
    
    @Inject
    private MqttPublisherService mqttPublisherService;
    
    @GET
    @Path("/status")
    public Response getStatus() {
        logger.info("📊 Test endpoint called - checking MQTT status");
        
        Map<String, Object> status = new HashMap<>();
        
        try {
            boolean isConnected = mqttConnection != null && mqttConnection.isConnected();
            status.put("mqttConnectionInjected", mqttConnection != null);
            status.put("mqttConnected", isConnected);
            status.put("listenerServiceInjected", mqttListenerService != null);
            status.put("publisherServiceInjected", mqttPublisherService != null);
            
            logger.info("✅ Status check completed: " + status);
            
            return Response.ok(status).build();
            
        } catch (Exception e) {
            logger.severe("❌ Error checking status: " + e.getMessage());
            e.printStackTrace();
            status.put("error", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(status).build();
        }
    }
    
    @POST
    @Path("/publish")
    public Response publishTestMessage(@QueryParam("message") String message) {
        logger.info("📤 Publishing test message: " + message);
        
        try {
            if (mqttPublisherService == null) {
                logger.severe("❌ MqttPublisherService is NULL!");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "MqttPublisherService not injected"))
                    .build();
            }
            
            String testMsg = message != null ? message : "{\"test\":\"message\"}";
            mqttPublisherService.publishMessage(testMsg);
            
            logger.info("✅ Test message published successfully");
            return Response.ok(Map.of("status", "published", "message", testMsg)).build();
            
        } catch (Exception e) {
            logger.severe("❌ Error publishing test message: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    @POST
    @Path("/reconnect")
    public Response reconnect() {
        logger.info("🔄 Manual reconnect requested");
        
        try {
            mqttConnection.disconnect();
            Thread.sleep(1000);
            mqttConnection.connect();
            
            logger.info("✅ Reconnection completed");
            return Response.ok(Map.of("status", "reconnected")).build();
            
        } catch (Exception e) {
            logger.severe("❌ Reconnection failed: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
}