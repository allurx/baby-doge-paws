package red.zyc.babydogepaws.model.request;

import red.zyc.babydogepaws.model.BabyDogePawsAccount;
import red.zyc.babydogepaws.model.response.Card;

import java.math.BigDecimal;

/**
 * 升级卡片所需信息
 *
 * @author allurx
 */
public class UpgradeCard extends BabyDogePawsGameRequestParam {


    /**
     * 用户当前余额
     */
    public final BigDecimal balance;

    /**
     * {@link Card}
     */
    public final Card card;

    public UpgradeCard(BabyDogePawsAccount account, BigDecimal balance, Card card) {
        super(account);
        this.balance = balance;
        this.card = card;
    }
}
