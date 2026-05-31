package com.jullyscraft.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jullyscraft.config.ShippingConfig;
import com.jullyscraft.dto.request.ShipOrderRequest;
import com.jullyscraft.dto.request.ShippingRateRequest;
import com.jullyscraft.dto.response.ShippingRateResponse;
import com.jullyscraft.dto.response.TrackingResponse;
import com.jullyscraft.entity.Order;
import com.jullyscraft.entity.Shipment;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.service.ShippingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DelhiveryProvider implements ShippingProvider {

    private final ShippingConfig config;
    private final RestTemplate   restTemplate;
    private final ObjectMapper   objectMapper;

    @Override
    public Shipment.ShippingProvider getProviderType() {
        return Shipment.ShippingProvider.DELHIVERY;
    }

    @Override
    public boolean isEnabled() {
        return config.getDelhiveryToken() != null
                && !config.getDelhiveryToken().isBlank();
    }

    @Override
    public ShippingRateResponse getRates(ShippingRateRequest req) {
        try {
            String url = config.getDelhiveryBaseUrl()
                    + "/api/kinko/v1.0/invoice/charges/.json"
                    + "?md=S"                    // surface mode
                    + "&ss=Delivered"
                    + "&d_pin=" + req.getDeliveryPincode()
                    + "&o_pin=" + req.getPickupPincode()
                    + "&cgm=" + req.getWeightGrams()
                    + "&pt=Pre-paid"
                    + "&cod=0";

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()), String.class);

            JsonNode root  = objectMapper.readTree(resp.getBody());
            double   total = root.path("total_amount").asDouble(
                    config.getDefaultShippingFee().doubleValue());

            return ShippingRateResponse.builder()
                    .pickupPincode(req.getPickupPincode())
                    .deliveryPincode(req.getDeliveryPincode())
                    .options(List.of(
                            ShippingRateResponse.CourierOption.builder()
                                    .provider(Shipment.ShippingProvider.DELHIVERY)
                                    .courierName("Delhivery")
                                    .rate(BigDecimal.valueOf(total))
                                    .estimatedDays(4)
                                    .codAvailable(true)
                                    .recommended(true)
                                    .build()))
                    .recommendedRate(BigDecimal.valueOf(total))
                    .build();

        } catch (Exception e) {
            log.error("Delhivery getRates failed: {}", e.getMessage());
            return ShippingRateResponse.builder()
                    .pickupPincode(req.getPickupPincode())
                    .deliveryPincode(req.getDeliveryPincode())
                    .options(List.of())
                    .recommendedRate(config.getDefaultShippingFee())
                    .build();
        }
    }

    @Override
    public Shipment book(Order order, ShipOrderRequest req) {
        if (!isEnabled()) {
            throw new BadRequestException(
                    "Delhivery is not configured — check API token");
        }

        try {
            // Delhivery uses form-encoded shipment data
            String shipmentData = buildShipmentPayload(order, req);

            HttpHeaders headers = buildHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "format=json&data=" +
                    java.net.URLEncoder.encode(shipmentData,
                            java.nio.charset.StandardCharsets.UTF_8);

            ResponseEntity<String> resp = restTemplate.exchange(
                    config.getDelhiveryBaseUrl() + "/api/cmu/create.json",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class);

            JsonNode root    = objectMapper.readTree(resp.getBody());
            JsonNode pkg     = root.path("packages").get(0);
            String   waybill = pkg.path("waybill").asText();
            String   status  = pkg.path("status").asText();

            log.info("Delhivery shipment booked: AWB={}, order={}",
                    waybill, order.getOrderNumber());

            return Shipment.builder()
                    .order(order)
                    .provider(Shipment.ShippingProvider.DELHIVERY)
                    .providerShipmentId(waybill)
                    .awbNumber(waybill)
                    .courierName("Delhivery")
                    .trackingUrl("https://www.delhivery.com/track/package/"
                            + waybill)
                    .status(Shipment.ShipmentStatus.BOOKED)
                    .pickupLocation(config.getDelhiveryWarehouseName())
                    .deliveryAddress(order.getShippingAddressLine1())
                    .deliveryPincode(order.getShippingPincode())
                    .weightGrams(req.getWeightGrams() != null
                            ? req.getWeightGrams() : config.getDefaultWeightGrams())
                    .length(config.getDefaultLength())
                    .breadth(config.getDefaultBreadth())
                    .height(config.getDefaultHeight())
                    .declaredValue(order.getTotalAmount())
                    .shippedAt(LocalDateTime.now())
                    .estimatedDelivery(LocalDateTime.now().plusDays(4))
                    .build();

        } catch (Exception e) {
            log.error("Delhivery booking failed: {}", e.getMessage());
            throw new BadRequestException("Shipment booking failed: " + e.getMessage());
        }
    }

    @Override
    public TrackingResponse track(String awbNumber) {
        try {
            String url = config.getDelhiveryBaseUrl()
                    + "/api/v1/packages/json/?waybill=" + awbNumber
                    + "&verbose=1";

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()), String.class);

            JsonNode root    = objectMapper.readTree(resp.getBody());
            JsonNode pkg     = root.path("ShipmentData").get(0)
                    .path("Shipment");

            String   status  = pkg.path("Status").path("Status").asText();
            JsonNode scans   = pkg.path("Scans");

            List<TrackingResponse.TrackingEvent> events = new ArrayList<>();
            for (JsonNode scan : scans) {
                JsonNode scanDetail = scan.path("ScanDetail");
                events.add(TrackingResponse.TrackingEvent.builder()
                        .description(scanDetail.path("Instructions").asText())
                        .location(scanDetail.path("ScannedLocation").asText())
                        .status(scanDetail.path("Scan").asText())
                        .timestamp(LocalDateTime.now())
                        .build());
            }

            return TrackingResponse.builder()
                    .awbNumber(awbNumber)
                    .provider(Shipment.ShippingProvider.DELHIVERY)
                    .status(mapDelhiveryStatus(status))
                    .events(events)
                    .build();

        } catch (Exception e) {
            log.error("Delhivery tracking failed: {}", e.getMessage());
            return TrackingResponse.builder()
                    .awbNumber(awbNumber)
                    .provider(Shipment.ShippingProvider.DELHIVERY)
                    .status(Shipment.ShipmentStatus.IN_TRANSIT)
                    .events(List.of())
                    .build();
        }
    }

    @Override
    public boolean cancel(String awbNumber) {
        try {
            String url  = config.getDelhiveryBaseUrl()
                    + "/api/p/edit?waybill=" + awbNumber + "&cancellation=true";
            restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()), String.class);
            return true;
        } catch (Exception e) {
            log.error("Delhivery cancel failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String generateLabel(String providerShipmentId) {
        return config.getDelhiveryBaseUrl()
                + "/api/p/packing_slip?wbns=" + providerShipmentId
                + "&token=" + config.getDelhiveryToken();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private HttpHeaders buildHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Token " + config.getDelhiveryToken());
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private String buildShipmentPayload(Order order, ShipOrderRequest req)
            throws Exception {
        Map<String, Object> shipment = new LinkedHashMap<>();
        shipment.put("name",         order.getShippingFullName());
        shipment.put("add",          order.getShippingAddressLine1()
                + (order.getShippingAddressLine2() != null
                ? " " + order.getShippingAddressLine2() : ""));
        shipment.put("pin",          order.getShippingPincode());
        shipment.put("city",         order.getShippingCity());
        shipment.put("state",        order.getShippingState());
        shipment.put("country",      "India");
        shipment.put("phone",        order.getShippingPhone());
        shipment.put("order",        order.getOrderNumber());
        shipment.put("payment_mode",
                order.getPaymentMethod() ==
                        com.jullyscraft.entity.Order.PaymentMethod.COD
                        ? "COD" : "Pre-paid");
        shipment.put("total_amount", order.getTotalAmount());
        shipment.put("cod_amount",
                order.getPaymentMethod() ==
                        com.jullyscraft.entity.Order.PaymentMethod.COD
                        ? order.getTotalAmount() : 0);
        shipment.put("weight",
                (req.getWeightGrams() != null
                        ? req.getWeightGrams()
                        : config.getDefaultWeightGrams()) / 1000.0);
        shipment.put("seller_name",    "Jully's Craft");
        shipment.put("seller_add",     "Warehouse Address");
        shipment.put("seller_pin",     "110001");
        shipment.put("seller_city",    "Delhi");
        shipment.put("seller_state",   "Delhi");
        shipment.put("seller_country", "India");
        shipment.put("quantity",       "1");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("shipments",     List.of(shipment));
        payload.put("pickup_location",
                Map.of("name", config.getDelhiveryWarehouseName()));

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(payload);
    }

    private Shipment.ShipmentStatus mapDelhiveryStatus(String status) {
        return switch (status.toUpperCase()) {
            case "DELIVERED"             -> Shipment.ShipmentStatus.DELIVERED;
            case "IN TRANSIT"            -> Shipment.ShipmentStatus.IN_TRANSIT;
            case "OUT FOR DELIVERY"      -> Shipment.ShipmentStatus.OUT_FOR_DELIVERY;
            case "PICKUP SCHEDULED",
                 "MANIFESTED"           -> Shipment.ShipmentStatus.BOOKED;
            case "PICKED UP"             -> Shipment.ShipmentStatus.PICKED_UP;
            case "RTO"                   -> Shipment.ShipmentStatus.RTO;
            case "CANCELLED"             -> Shipment.ShipmentStatus.CANCELLED;
            default                      -> Shipment.ShipmentStatus.IN_TRANSIT;
        };
    }
}