package red.zyc.babydogepaws.model;

import red.zyc.babydogepaws.model.response.Card;

import java.math.BigDecimal;

/**
 * 升级卡片所需信息
 *
 * @param balance 用户当前余额
 * @param card    {@link Card}
 * @author allurx
 */
public record UpgradeCard(BigDecimal balance, Card card) {
}
