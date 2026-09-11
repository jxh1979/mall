package com.complex.platform.floorplan;
// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FloorPlanImportParserTests {

    private final FloorPlanImportParser parser = new FloorPlanImportParser();

    @Test
    void parsesSvgPolygonIntoNormalizedCandidate() {
        FloorPlanImportParseResult result = parser.parse(
                "svg",
                """
                <svg viewBox="0 0 1000 800">
                  <polygon id="A101" data-shape-type="shop" data-ref-type="space" data-ref-id="sp_101"
                           points="100,80 300,80 300,240 100,240" />
                </svg>
                """
        );

        assertThat(result.errors()).isEmpty();
        assertThat(result.candidates()).hasSize(1);
        FloorPlanImportParseResult.CandidateDraft candidate = result.candidates().getFirst();
        assertThat(candidate.label()).isEqualTo("A101");
        assertThat(candidate.shapeType()).isEqualTo("shop");
        assertThat(candidate.refType()).isEqualTo("space");
        assertThat(candidate.refId()).isEqualTo("sp_101");
        assertThat(candidate.polygonPoints())
                .containsExactly(
                        new FloorPlanPoint(0.1, 0.1),
                        new FloorPlanPoint(0.3, 0.1),
                        new FloorPlanPoint(0.3, 0.3),
                        new FloorPlanPoint(0.1, 0.3)
                );
    }

    @Test
    void rejectsUnclosedSvgPolylineAsImportError() {
        FloorPlanImportParseResult result = parser.parse(
                "svg",
                """
                <svg viewBox="0 0 1000 800">
                  <polyline id="route1" points="100,80 300,80 300,240" />
                </svg>
                """
        );

        assertThat(result.candidates()).isEmpty();
        assertThat(result.errors())
                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
                .containsExactly("POLYLINE_NOT_CLOSED");
    }

    @Test
    void parsesStandardJsonCandidate() {
        FloorPlanImportParseResult result = parser.parse(
                "standard_json",
                """
                {
                  "shapes": [
                    {
                      "label": "Shop 102",
                      "shape_type": "shop",
                      "ref_type": "space",
                      "ref_id": "sp_102",
                      "z_index": 5,
                      "polygon_points": [
                        {"x": 0.2, "y": 0.2},
                        {"x": 0.4, "y": 0.2},
                        {"x": 0.4, "y": 0.4},
                        {"x": 0.2, "y": 0.4}
                      ]
                    }
                  ]
                }
                """
        );

        assertThat(result.errors()).isEmpty();
        assertThat(result.candidates()).hasSize(1);
        assertThat(result.candidates().getFirst().label()).isEqualTo("Shop 102");
        assertThat(result.candidates().getFirst().zIndex()).isEqualTo(5);
    }

    @Test
    void reportsJsonCandidateWithOutOfRangePoint() {
        FloorPlanImportParseResult result = parser.parse(
                "standard_json",
                """
                {
                  "shapes": [
                    {
                      "label": "Bad Shop",
                      "polygon_points": [
                        {"x": 0.2, "y": 0.2},
                        {"x": 1.4, "y": 0.2},
                        {"x": 0.4, "y": 0.4}
                      ]
                    }
                  ]
                }
                """
        );

        assertThat(result.candidates()).isEmpty();
        assertThat(result.errors())
                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
                .containsExactly("POINT_OUT_OF_RANGE");
    }

    // M017-001：解析器错误码闭集 10 个逐一有可失败用例（修复前只锁 POLYLINE_NOT_CLOSED 与 JSON 侧 POINT_OUT_OF_RANGE）
    @Test
    void reportsPayloadRequiredForBlankPayload() {
        assertThat(parser.parse("svg", "   ").errors())
                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
                .containsExactly("PAYLOAD_REQUIRED");
    }

    @Test
    void reportsUnsupportedSourceFormat() {
        assertThat(parser.parse("geojson", "{}").errors())
                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
                .containsExactly("UNSUPPORTED_SOURCE_FORMAT");
    }

    @Test
    void reportsViewBoxRequiredForSvgWithoutViewBox() {
        assertThat(parser.parse("svg", "<svg><polygon points=\"1,1 2,1 2,2\" /></svg>").errors())
                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
                .containsExactly("VIEWBOX_REQUIRED");
    }

    @Test
    void reportsPointsRequiredForShapeWithoutPoints() {
        assertThat(parser.parse("svg", "<svg viewBox=\"0 0 10 10\"><polygon id=\"a\" /></svg>").errors())
                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
                .containsExactly("POINTS_REQUIRED");
    }

    // M017-001 / M017-006：落库并展示给复核人的 error_message 为中文整句（M015-004）
    @Test
    void reportsPointsTooFewWithChineseMessage() {
        FloorPlanImportParseResult result = parser.parse("svg", "<svg viewBox=\"0 0 10 10\"><polygon points=\"1,1 2,2\" /></svg>");

        assertThat(result.candidates()).isEmpty();
        assertThat(result.errors())
                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode, FloorPlanImportParseResult.ErrorDraft::errorMessage)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("POINTS_TOO_FEW", "多边形至少需要 3 个顶点"));
    }

    @Test
    void reportsNoShapesFoundForSvgWithoutPolygons() {
        assertThat(parser.parse("svg", "<svg viewBox=\"0 0 10 10\"><rect width=\"1\" height=\"1\" /></svg>").errors())
                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
                .containsExactly("NO_SHAPES_FOUND");
    }

    @Test
    void reportsJsonParseFailedForMalformedJson() {
        assertThat(parser.parse("standard_json", "{not json").errors())
                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
                .containsExactly("JSON_PARSE_FAILED");
    }

    @Test
    void reportsShapesRequiredForJsonWithoutShapesArray() {
        assertThat(parser.parse("standard_json", "{\"foo\": 1}").errors())
                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
                .containsExactly("SHAPES_REQUIRED");
    }

    @Test
    void reportsSvgPointOutOfRangeAgainstViewBox() {
        assertThat(parser.parse("svg", "<svg viewBox=\"0 0 100 100\"><polygon points=\"0,0 150,0 150,50\" /></svg>").errors())
                .extracting(FloorPlanImportParseResult.ErrorDraft::errorCode)
                .containsExactly("POINT_OUT_OF_RANGE");
    }

    @Test
    void appliesSvgDefaultsForLabelShapeTypeAndRefType() {
        FloorPlanImportParseResult result = parser.parse("svg", "<svg viewBox=\"0 0 100 100\"><polygon points=\"10,10 20,10 20,20\" /></svg>");

        assertThat(result.errors()).isEmpty();
        FloorPlanImportParseResult.CandidateDraft candidate = result.candidates().getFirst();
        assertThat(candidate.label()).isEqualTo("shape-1");
        assertThat(candidate.shapeType()).isEqualTo("shop");
        assertThat(candidate.refType()).isEqualTo("custom");
        assertThat(candidate.refId()).isNull();
    }
}
