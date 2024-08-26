package red.zyc.babydogepaws.model.request;

import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;
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

    public UpgradeCard(BabyDogePawsUser user, BigDecimal balance, Card card) {
        super(user);
        this.balance = balance;
        this.card = card;
    }
}
