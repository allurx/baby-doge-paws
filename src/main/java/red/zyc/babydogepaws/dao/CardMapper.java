package red.zyc.babydogepaws.dao;

import org.apache.ibatis.annotations.Insert;
import red.zyc.babydogepaws.model.persistent.Card;
import red.zyc.babydogepaws.model.request.UpgradeCard;

/**
 * @author allurx
 */
public interface CardMapper {

    @Insert("""
            INSERT INTO card (card_id, card_name, category_id, category_name, requirement, upgrade_info)
            VALUES (#{card.cardId}, #{card.cardName}, #{card.categoryId}, #{card.categoryName}, #{card.requirement},CAST(#{upgradeInfo.insertJson} AS JSON)) 
                ON DUPLICATE KEY
                    UPDATE
                        card_name = #{card.cardName},
                        category_id=#{card.categoryId},
                        category_name=#{card.categoryName},
                        requirement=CAST(#{card.requirement} AS JSON),
                        upgrade_info=JSON_SET(
                            upgrade_info,
                            #{upgradeInfo.levelPath},
                            CAST(#{upgradeInfo.updateJson} AS JSON)
                        );
            """)
    int saveOrUpdateCard(Card card, UpgradeCard.UpgradeInfo upgradeInfo);
}
