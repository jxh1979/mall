package com.complex.platform.bar;

// 文件版本：V0.01

import static com.complex.platform.bar.BarValidation.requireText;

import com.complex.platform.common.api.ApiException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 第十八批 BL 段第 380 条 380-R1（BSM:717「酒吧要知道桌台、卡座、吧台属于哪个空间和商户。」；PGM:470「记录空间面积、用途、状态、负责人和所属商户。」）：
 * 新建台位的所属空间——只认空间登记的所属商户为该酒吧且启用中的空间；可选清单与建台位校验共用同一谓词。
 */
@Component
public class BarTableSpaceBinding {

    private static final String OWNED_ACTIVE = " FROM spaces WHERE merchant_id = ? AND status = 'active'";

    /** 可选空间一项。 */
    public record Option(String id, String spaceName) {
    }

    private final JdbcTemplate jdbc;

    BarTableSpaceBinding(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<Option> options(String owner) {
        return jdbc.query("SELECT id, space_name" + OWNED_ACTIVE + " ORDER BY space_name, id",
                (rs, n) -> new Option(rs.getString("id"), rs.getString("space_name")), owner);
    }

    /** 所选空间须为该酒吧名下启用中的空间：缺失 400 VALIDATION_FAILED，不符 409 BAR_TABLE_SPACE_NOT_OWNED；返回去空白的空间编号。 */
    String requireOwned(String owner, String spaceId) {
        String id = requireText(spaceId, "space_id");
        Integer hits = jdbc.queryForObject("SELECT count(*)" + OWNED_ACTIVE + " AND id = ?", Integer.class, owner, id);
        if (hits == null || hits == 0) {
            throw ApiException.conflict("BAR_TABLE_SPACE_NOT_OWNED", "所选空间不属于该酒吧或已停用");
        }
        return id;
    }
}
