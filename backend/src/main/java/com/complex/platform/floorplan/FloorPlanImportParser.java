package com.complex.platform.floorplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class FloorPlanImportParser {

    private static final Pattern SVG_SHAPE_PATTERN = Pattern.compile("(?is)<(polygon|polyline)\\b([^>]*)/?\\s*>");
    private static final Pattern SVG_ATTR_PATTERN = Pattern.compile("([A-Za-z_:][-A-Za-z0-9_:.]*)\\s*=\\s*(['\"])(.*?)\\2", Pattern.DOTALL);
    private static final double ROUNDING_SCALE = 1_000_000d;

    private final ObjectMapper objectMapper;

    public FloorPlanImportParser() {
        this(new ObjectMapper());
    }

    FloorPlanImportParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    public FloorPlanImportParseResult parse(String sourceFormat, String sourcePayload) {
        String format = normalize(sourceFormat);
        if (sourcePayload == null || sourcePayload.isBlank()) {
            return new FloorPlanImportParseResult(
                    List.of(),
                    List.of(error(null, "source_payload", "PAYLOAD_REQUIRED", "source_payload is required"))
            );
        }
        return switch (format) {
            case "svg" -> parseSvg(sourcePayload);
            case "standard_json" -> parseStandardJson(sourcePayload);
            default -> new FloorPlanImportParseResult(
                    List.of(),
                    List.of(error(null, "source_format", "UNSUPPORTED_SOURCE_FORMAT", "source_format is not supported"))
            );
        };
    }

    private FloorPlanImportParseResult parseSvg(String payload) {
        List<FloorPlanImportParseResult.CandidateDraft> candidates = new ArrayList<>();
        List<FloorPlanImportParseResult.ErrorDraft> errors = new ArrayList<>();
        double[] viewBox = readViewBox(payload);
        if (viewBox == null || viewBox[2] <= 0 || viewBox[3] <= 0) {
            return new FloorPlanImportParseResult(
                    List.of(),
                    List.of(error(null, "viewBox", "VIEWBOX_REQUIRED", "svg viewBox with positive width and height is required"))
            );
        }

        Matcher matcher = SVG_SHAPE_PATTERN.matcher(payload);
        int shapeNo = 1;
        while (matcher.find()) {
            String tag = matcher.group(1).toLowerCase(Locale.ROOT);
            Map<String, String> attributes = readAttributes(matcher.group(2));
            String points = attributes.get("points");
            if (points == null || points.isBlank()) {
                errors.add(error(shapeNo, "points", "POINTS_REQUIRED", "points attribute is required"));
                shapeNo++;
                continue;
            }

            List<FloorPlanPoint> rawPoints = parseSvgPoints(points);
            if (rawPoints.size() < 3) {
                errors.add(error(shapeNo, "points", "POINTS_TOO_FEW", "polygon requires at least 3 points"));
                shapeNo++;
                continue;
            }
            if ("polyline".equals(tag) && !samePoint(rawPoints.getFirst(), rawPoints.getLast())) {
                errors.add(error(shapeNo, "points", "POLYLINE_NOT_CLOSED", "polyline must be closed before import"));
                shapeNo++;
                continue;
            }
            if (rawPoints.size() > 3 && samePoint(rawPoints.getFirst(), rawPoints.getLast())) {
                rawPoints = rawPoints.subList(0, rawPoints.size() - 1);
            }

            List<FloorPlanPoint> normalized = normalizePoints(rawPoints, viewBox);
            if (normalized.stream().anyMatch(point -> outOfRange(point.x()) || outOfRange(point.y()))) {
                errors.add(error(shapeNo, "points", "POINT_OUT_OF_RANGE", "normalized point x/y must be between 0 and 1"));
                shapeNo++;
                continue;
            }

            candidates.add(new FloorPlanImportParseResult.CandidateDraft(
                    defaultText(attributes.get("data-shape-type"), "shop"),
                    defaultText(attributes.get("data-ref-type"), "custom"),
                    blankToNull(attributes.get("data-ref-id")),
                    defaultText(firstNonBlank(attributes.get("data-label"), attributes.get("id")), "shape-" + shapeNo),
                    normalized,
                    candidates.size() + 1
            ));
            shapeNo++;
        }

        if (candidates.isEmpty() && errors.isEmpty()) {
            errors.add(error(null, "source_payload", "NO_SHAPES_FOUND", "svg does not contain polygon or polyline shapes"));
        }
        return new FloorPlanImportParseResult(candidates, errors);
    }

    private FloorPlanImportParseResult parseStandardJson(String payload) {
        List<FloorPlanImportParseResult.CandidateDraft> candidates = new ArrayList<>();
        List<FloorPlanImportParseResult.ErrorDraft> errors = new ArrayList<>();
        JsonNode shapesNode;
        try {
            JsonNode root = objectMapper.readTree(payload);
            shapesNode = root.isArray() ? root : root.path("shapes");
        } catch (Exception exception) {
            return new FloorPlanImportParseResult(
                    List.of(),
                    List.of(error(null, "source_payload", "JSON_PARSE_FAILED", "source_payload is not valid JSON"))
            );
        }
        if (!shapesNode.isArray()) {
            return new FloorPlanImportParseResult(
                    List.of(),
                    List.of(error(null, "shapes", "SHAPES_REQUIRED", "standard_json requires a shapes array"))
            );
        }

        int index = 0;
        for (JsonNode shapeNode : shapesNode) {
            index++;
            List<FloorPlanPoint> points = readJsonPoints(shapeNode.path("polygon_points"));
            String validationError = validateNormalizedPoints(points);
            if (validationError != null) {
                errors.add(error(index, "polygon_points", validationError, messageFor(validationError)));
                continue;
            }
            candidates.add(new FloorPlanImportParseResult.CandidateDraft(
                    textOrDefault(shapeNode, "shape_type", "shop"),
                    textOrDefault(shapeNode, "ref_type", "custom"),
                    textOrNull(shapeNode, "ref_id"),
                    textOrDefault(shapeNode, "label", "shape-" + index),
                    points,
                    intOrDefault(shapeNode, "z_index", index)
            ));
        }
        return new FloorPlanImportParseResult(candidates, errors);
    }

    private double[] readViewBox(String payload) {
        Map<String, String> attributes = readAttributes(payload);
        String viewBox = attributes.get("viewBox");
        if (viewBox == null) {
            viewBox = attributes.get("viewbox");
        }
        if (viewBox == null || viewBox.isBlank()) {
            return null;
        }
        String[] parts = viewBox.trim().replace(",", " ").split("\\s+");
        if (parts.length != 4) {
            return null;
        }
        try {
            return new double[] {
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3])
            };
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Map<String, String> readAttributes(String value) {
        Map<String, String> attributes = new HashMap<>();
        Matcher matcher = SVG_ATTR_PATTERN.matcher(value);
        while (matcher.find()) {
            attributes.put(matcher.group(1), matcher.group(3));
        }
        return attributes;
    }

    private List<FloorPlanPoint> parseSvgPoints(String points) {
        String[] values = points.trim().replace(",", " ").split("\\s+");
        if (values.length < 6 || values.length % 2 != 0) {
            return List.of();
        }
        List<FloorPlanPoint> parsed = new ArrayList<>();
        for (int i = 0; i < values.length; i += 2) {
            try {
                parsed.add(new FloorPlanPoint(Double.parseDouble(values[i]), Double.parseDouble(values[i + 1])));
            } catch (NumberFormatException exception) {
                return List.of();
            }
        }
        return parsed;
    }

    private List<FloorPlanPoint> normalizePoints(List<FloorPlanPoint> points, double[] viewBox) {
        List<FloorPlanPoint> normalized = new ArrayList<>();
        for (FloorPlanPoint point : points) {
            normalized.add(new FloorPlanPoint(
                    round((point.x() - viewBox[0]) / viewBox[2]),
                    round((point.y() - viewBox[1]) / viewBox[3])
            ));
        }
        return normalized;
    }

    private List<FloorPlanPoint> readJsonPoints(JsonNode pointsNode) {
        if (!pointsNode.isArray()) {
            return List.of();
        }
        List<FloorPlanPoint> points = new ArrayList<>();
        for (JsonNode pointNode : pointsNode) {
            if (!pointNode.path("x").isNumber() || !pointNode.path("y").isNumber()) {
                return List.of();
            }
            points.add(new FloorPlanPoint(round(pointNode.path("x").asDouble()), round(pointNode.path("y").asDouble())));
        }
        return points;
    }

    private String validateNormalizedPoints(List<FloorPlanPoint> points) {
        if (points.size() < 3) {
            return "POINTS_TOO_FEW";
        }
        for (FloorPlanPoint point : points) {
            if (outOfRange(point.x()) || outOfRange(point.y())) {
                return "POINT_OUT_OF_RANGE";
            }
        }
        return null;
    }

    private String messageFor(String errorCode) {
        return switch (errorCode) {
            case "POINT_OUT_OF_RANGE" -> "polygon point x/y must be between 0 and 1";
            case "POINTS_TOO_FEW" -> "polygon requires at least 3 points";
            default -> "import candidate is invalid";
        };
    }

    private FloorPlanImportParseResult.ErrorDraft error(Integer lineNo, String fieldName, String errorCode, String errorMessage) {
        return new FloorPlanImportParseResult.ErrorDraft(lineNo, fieldName, errorCode, errorMessage);
    }

    private boolean samePoint(FloorPlanPoint left, FloorPlanPoint right) {
        return Double.compare(left.x(), right.x()) == 0 && Double.compare(left.y(), right.y()) == 0;
    }

    private boolean outOfRange(double value) {
        return value < 0 || value > 1 || Double.isNaN(value) || Double.isInfinite(value);
    }

    private double round(double value) {
        return Math.round(value * ROUNDING_SCALE) / ROUNDING_SCALE;
    }

    private String normalize(String value) {
        return defaultText(value, "").toLowerCase(Locale.ROOT);
    }

    private String textOrDefault(JsonNode node, String field, String defaultValue) {
        return defaultText(textOrNull(node, field), defaultValue);
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        String text = node.get(field).asText();
        return blankToNull(text);
    }

    private int intOrDefault(JsonNode node, String field, int defaultValue) {
        return node != null && node.hasNonNull(field) && node.get(field).canConvertToInt()
                ? node.get(field).asInt()
                : defaultValue;
    }

    private String firstNonBlank(String first, String second) {
        String normalized = blankToNull(first);
        return normalized == null ? blankToNull(second) : normalized;
    }

    private String defaultText(String value, String defaultValue) {
        String normalized = blankToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
