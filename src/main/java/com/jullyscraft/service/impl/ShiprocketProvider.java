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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShiprocketProvider implements ShippingProvider {

    private final ShippingConfig              config;
    private final RestTemplate                restTemplate;
    private final ObjectMapper                objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TOKEN_KEY     = "shiprocket:token";
    private static final String PICKUP_PINCODE = "110001";  // warehouse pincode

    // ── Provider identity ─────────────────────────────────────────────────────

    @Override
    public Shipment.ShippingProvider getProviderType() {
        return Shipment.ShippingProvider.SHIPROCKET;
    }

    @Override
    public boolean isEnabled() {
        return config.getShiprocketEmail()    != null
                && !config.getShiprocketEmail().isBlank()
                && config.getShiprocketPassword() != null
                && !config.getShiprocketPassword().isBlank();
    }

    // ── Rates ─────────────────────────────────────────────────────────────────

    @Override
    public ShippingRateResponse getRates(ShippingRateRequest req) {
        try {
            String token = getToken();
            String url   = config.getShiprocketBaseUrl()
                    + "/courier/serviceability/?pickup_postcode="
                    + req.getPickupPincode()
                    + "&delivery_postcode=" + req.getDeliveryPincode()
                    + "&weight=" + (req.getWeightGrams() / 1000.0)
                    + "&cod=0";

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(buildHeaders(token)),
                    String.class);

            return parseRatesResponse(resp.getBody(),
                    req.getPickupPincode(), req.getDeliveryPincode());

        } catch (Exception e) {
            log.error("Shiprocket getRates failed: {}", e.getMessage());
            return fallbackRate(req);
        }
    }

    // ── Book shipment ─────────────────────────────────────────────────────────

    @Override
    public Shipment book(Order order, ShipOrderRequest req) {
        if (!isEnabled()) {
            throw new BadRequestException(
                    "Shiprocket is not configured — check credentials");
        }

        try {
            String token = getToken();
            Map<String, Object> body = buildOrderPayload(order, req);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, buildHeaders(token));

            ResponseEntity<String> resp = restTemplate.exchange(
                    config.getShiprocketBaseUrl() + "/orders/create/adhoc",
                    HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(resp.getBody());

            String shipmentId = root.path("shipment_id").asText();
            String awb        = root.path("awb_code").asText();
            String courier    = root.path("courier_name").asText();
            String label      = root.path("label_url").asText();

            log.info("Shiprocket shipment booked: AWB={}, order={}",
                    awb, order.getOrderNumber());

            return Shipment.builder()
                    .order(order)
                    .provider(Shipment.ShippingProvider.SHIPROCKET)
                    .providerShipmentId(shipmentId)
                    .awbNumber(awb)
                    .courierName(courier)
                    .trackingUrl("https://shiprocket.co/tracking/" + awb)
                    .labelUrl(label)
                    .status(Shipment.ShipmentStatus.BOOKED)
                    .pickupLocation(req.getPickupLocation() != null
                            ? req.getPickupLocation() : "Primary")
                    .deliveryAddress(order.getShippingAddressLine1())
                    .deliveryPincode(order.getShippingPincode())
                    .weightGrams(req.getWeightGrams() != null
                            ? req.getWeightGrams() : config.getDefaultWeightGrams())
                    .length(config.getDefaultLength())
                    .breadth(config.getDefaultBreadth())
                    .height(config.getDefaultHeight())
                    .declaredValue(order.getTotalAmount())
                    .shippedAt(LocalDateTime.now())
                    .estimatedDelivery(LocalDateTime.now().plusDays(5))
                    .build();

        } catch (Exception e) {
            log.error("Shiprocket booking failed: {}", e.getMessage());
            throw new BadRequestException("Shipment booking failed: " + e.getMessage());
        }
    }

    // ── Track ─────────────────────────────────────────────────────────────────

    @Override
    public TrackingResponse track(String awbNumber) {
        try {
            String token = getToken();
            String url   = config.getShiprocketBaseUrl()
                    + "/courier/track/awb/" + awbNumber;

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(buildHeaders(token)), String.class);

            return parseTrackingResponse(awbNumber, resp.getBody());

        } catch (Exception e) {
            log.error("Shiprocket tracking failed for AWB {}: {}",
                    awbNumber, e.getMessage());
            return TrackingResponse.builder()
                    .awbNumber(awbNumber)
                    .provider(Shipment.ShippingProvider.SHIPROCKET)
                    .status(Shipment.ShipmentStatus.IN_TRANSIT)
                    .events(List.of())
                    .build();
        }
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Override
    public boolean cancel(String awbNumber) {
        try {
            String token = getToken();
            Map<String, Object> body = Map.of("awbs", List.of(awbNumber));

            restTemplate.exchange(
                    config.getShiprocketBaseUrl() + "/orders/cancel/shipment/awbs",
                    HttpMethod.POST,
                    new HttpEntity<>(body, buildHeaders(token)),
                    String.class);

            log.info("Shiprocket shipment cancelled: AWB={}", awbNumber);
            return true;
        } catch (Exception e) {
            log.error("Shiprocket cancel failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Label ─────────────────────────────────────────────────────────────────

    @Override
    public String generateLabel(String providerShipmentId) {
        try {
            String token = getToken();
            Map<String, Object> body =
                    Map.of("shipment_id", List.of(providerShipmentId));

            ResponseEntity<String> resp = restTemplate.exchange(
                    config.getShiprocketBaseUrl() + "/courier/generate/label",
                    HttpMethod.POST,
                    new HttpEntity<>(body, buildHeaders(token)),
                    String.class);

            JsonNode root = objectMapper.readTree(resp.getBody());
            return root.path("label_url").asText();

        } catch (Exception e) {
            log.error("Shiprocket label generation failed: {}", e.getMessage());
            return null;
        }
    }

    // ── Auth token (cached in Redis for 9 days) ───────────────────────────────

    private String getToken() {
        Object cached = redisTemplate.opsForValue().get(TOKEN_KEY);
        if (cached != null) return cached.toString();

        Map<String, String> body = Map.of(
                "email",    config.getShiprocketEmail(),
                "password", config.getShiprocketPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> resp = restTemplate.exchange(
                config.getShiprocketBaseUrl() + "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);

        try {
            JsonNode root  = objectMapper.readTree(resp.getBody());
            String   token = root.path("token").asText();
            redisTemplate.opsForValue().set(TOKEN_KEY, token, 9, TimeUnit.DAYS);
            log.info("Shiprocket token refreshed");
            return token;
        } catch (Exception e) {
            throw new BadRequestException("Shiprocket auth failed: " + e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private HttpHeaders buildHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    private Map<String, Object> buildOrderPayload(Order order,
                                                  ShipOrderRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_id",           order.getOrderNumber());
        body.put("order_date",         order.getCreatedAt().toString());
        body.put("pickup_location",    req.getPickupLocation() != null
                ? req.getPickupLocation() : "Primary");
        body.put("channel_id",         config.getShiprocketChannelId());
        body.put("billing_customer_name",  order.getShippingFullName());
        body.put("billing_address",        order.getShippingAddressLine1());
        body.put("billing_city",           order.getShippingCity());
        body.put("billing_pincode",        order.getShippingPincode());
        body.put("billing_state",          order.getShippingState());
        body.put("billing_country",        order.getShippingCountry());
        body.put("billing_phone",          order.getShippingPhone());
        body.put("shipping_is_billing",    true);
        body.put("order_items",            buildOrderItems(order));
        body.put("payment_method",
                order.getPaymentMethod() ==
                        com.jullyscraft.entity.Order.PaymentMethod.COD ? "COD" : "Prepaid");
        body.put("sub_total",              order.getSubtotal());
        body.put("length",                 config.getDefaultLength());
        body.put("breadth",                config.getDefaultBreadth());
        body.put("height",                 config.getDefaultHeight());
        body.put("weight",
                (req.getWeightGrams() != null
                        ? req.getWeightGrams()
                        : config.getDefaultWeightGrams()) / 1000.0);
        return body;
    }

    private List<Map<String, Object>> buildOrderItems(Order order) {
        return order.getItems().stream().map(item -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name",       item.getProductName());
            m.put("sku",        item.getProductSku() != null
                    ? item.getProductSku() : "SKU-" + item.getProduct().getId());
            m.put("units",      item.getQuantity());
            m.put("selling_price", item.getUnitPrice());
            return m;
        }).toList();
    }

    private ShippingRateResponse parseRatesResponse(String json,
                                                    String pickup,
                                                    String delivery) {
        List<ShippingRateResponse.CourierOption> options = new ArrayList<>();
        try {
            JsonNode root     = objectMapper.readTree(json);
            JsonNode couriers = root.path("data")
                    .path("available_courier_companies");

            for (JsonNode c : couriers) {
                options.add(ShippingRateResponse.CourierOption.builder()
                        .provider(Shipment.ShippingProvider.SHIPROCKET)
                        .courierName(c.path("courier_name").asText())
                        .courierCode(c.path("courier_company_id").asText())
                        .rate(BigDecimal.valueOf(c.path("rate").asDouble()))
                        .codCharge(BigDecimal.valueOf(
                                c.path("cod_charges").asDouble()))
                        .estimatedDays(c.path("estimated_delivery_days").asInt(5))
                        .codAvailable(c.path("cod").asInt() == 1)
                        .recommended(c.path("is_recommended").asInt() == 1)
                        .build());
            }
        } catch (Exception e) {
            log.warn("Shiprocket rate parse error: {}", e.getMessage());
        }

        BigDecimal recommended = options.stream()
                .filter(ShippingRateResponse.CourierOption::isRecommended)
                .map(ShippingRateResponse.CourierOption::getRate)
                .findFirst()
                .orElse(config.getDefaultShippingFee());

        return ShippingRateResponse.builder()
                .pickupPincode(pickup)
                .deliveryPincode(delivery)
                .options(options)
                .recommendedRate(recommended)
                .build();
    }

    private TrackingResponse parseTrackingResponse(String awb, String json) {
        List<TrackingResponse.TrackingEvent> events = new ArrayList<>();
        Shipment.ShipmentStatus status = Shipment.ShipmentStatus.IN_TRANSIT;

        try {
            JsonNode root      = objectMapper.readTree(json);
            JsonNode tracking  = root.path("tracking_data")
                    .path("shipment_track_activities");

            for (JsonNode e : tracking) {
                events.add(TrackingResponse.TrackingEvent.builder()
                        .description(e.path("activity").asText())
                        .location(e.path("location").asText())
                        .timestamp(LocalDateTime.now())  // parse from e.path("date")
                        .status(e.path("sr-status-label").asText())
                        .build());
            }

            String srStatus = root.path("tracking_data")
                    .path("shipment_status").asText("");

            status = mapShiprocketStatus(srStatus);

        } catch (Exception e) {
            log.warn("Shiprocket tracking parse error: {}", e.getMessage());
        }

        return TrackingResponse.builder()
                .awbNumber(awb)
                .provider(Shipment.ShippingProvider.SHIPROCKET)
                .status(status)
                .events(events)
                .build();
    }

    private Shipment.ShipmentStatus mapShiprocketStatus(String srStatus) {
        return switch (srStatus.toUpperCase()) {
            case "DELIVERED"           -> Shipment.ShipmentStatus.DELIVERED;
            case "OUT FOR DELIVERY"    -> Shipment.ShipmentStatus.OUT_FOR_DELIVERY;
            case "IN TRANSIT"          -> Shipment.ShipmentStatus.IN_TRANSIT;
            case "PICKUP SCHEDULED",
                 "PICKUP GENERATED"   -> Shipment.ShipmentStatus.BOOKED;
            case "PICKED UP"           -> Shipment.ShipmentStatus.PICKED_UP;
            case "RTO INITIATED",
                 "RTO DELIVERED"      -> Shipment.ShipmentStatus.RTO;
            case "CANCELLED"           -> Shipment.ShipmentStatus.CANCELLED;
            default                    -> Shipment.ShipmentStatus.IN_TRANSIT;
        };
    }

    private ShippingRateResponse fallbackRate(ShippingRateRequest req) {
        return ShippingRateResponse.builder()
                .pickupPincode(req.getPickupPincode())
                .deliveryPincode(req.getDeliveryPincode())
                .options(List.of(ShippingRateResponse.CourierOption.builder()
                        .provider(Shipment.ShippingProvider.SHIPROCKET)
                        .courierName("Standard Delivery")
                        .rate(config.getDefaultShippingFee())
                        .estimatedDays(7)
                        .codAvailable(true)
                        .recommended(true)
                        .build()))
                .recommendedRate(config.getDefaultShippingFee())
                .build();
    }
}