package com.complex.platform.floorplan;

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
}
