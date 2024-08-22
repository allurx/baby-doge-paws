package red.zyc.babydogepaws.model.response;

import java.math.BigDecimal;

/**
 * 卡片
 *
 * @param id          卡片id
 * @param upgradeCost 卡片升级所需花费
 * @author allurx
 */
public record Card(int id, BigDecimal upgradeCost) {
}
