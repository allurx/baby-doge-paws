package red.zyc.babydogepaws.model.persistent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import red.zyc.babydogepaws.model.request.UpgradeCard;

import java.time.LocalDateTime;

/**
 * @author allurx
 */
public class Card {

    public Integer id;
    @JsonProperty("id")
    public Integer cardId;
    @JsonProperty("name")
    public String cardName;
    @JsonProperty("category_id")
    public Integer categoryId;
    @JsonProperty("category_name")
    public String categoryName;
    @JsonDeserialize(using = UpgradeCard.JsonObjectToJsonStringDeserializer.class)
    public String requirement;
    public String upgradeInfo;
    public LocalDateTime createdTime;
    public LocalDateTime modifiedTime;

}
