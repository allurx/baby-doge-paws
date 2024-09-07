package red.zyc.babydogepaws.game;

import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;
import red.zyc.babydogepaws.model.request.BabyDogePawsGameRequestParam;
import red.zyc.babydogepaws.model.request.PickChannel;
import red.zyc.babydogepaws.model.request.UpgradeCard;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Map;
import java.util.Optional;

import static red.zyc.kit.json.JsonOperator.JACKSON_OPERATOR;

/**
 * @author allurx
 */
public final class BabyDogePawsGame {

    private BabyDogePawsGame() {
    }

    public enum Request {

        AUTHORIZE {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/authorize"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(Optional.ofNullable(param.user.authParam).orElse("")))
                        .build();
            }
        },

        GET_ME {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/getMe"))
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .GET()
                        .build();
            }
        },

        PICK_DAILY_BONUS {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/pickDailyBonus"))
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .POST(HttpRequest.BodyPublishers.ofString(""))
                        .build();
            }
        },

        GET_DAILY_BONUSES {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/getDailyBonuses"))
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .GET()
                        .build();
            }
        },

        LIST_CARDS {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/cards"))
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .GET()
                        .build();
            }
        },

        UPGRADE_CARD {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                UpgradeCard upgradeCard = (UpgradeCard) param;
                return HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/cards"))
                        .header("content-type", "application/json")
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .POST(HttpRequest.BodyPublishers.ofString(JACKSON_OPERATOR.toJsonString(Map.of("id", upgradeCard.card.cardId))))
                        .build();
            }
        },

        MINE {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/mine"))
                        .header("content-type", "application/json")
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .POST(HttpRequest.BodyPublishers.ofString(JACKSON_OPERATOR.toJsonString(Map.of("count", BabyDogePawsUser.MINE_COUNT))))
                        .build();
            }
        },

        LIST_CHANNELS {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/channels"))
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .GET()
                        .build();
            }
        },

        PICK_CHANNEL {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                PickChannel pickChannel = (PickChannel) param;
                return HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/channels"))
                        .header("content-type", "application/json")
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .POST(HttpRequest.BodyPublishers.ofString(JACKSON_OPERATOR.toJsonString(Map.of("channelId", pickChannel.channel.id()))))
                        .build();
            }
        },

        PICK_PROMO {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/promo"))
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .POST(HttpRequest.BodyPublishers.ofString(""))
                        .build();
            }
        },

        GET_PROMO {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/promo"))
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .GET()
                        .build();
            }
        },

        LIST_FRIENDS {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return HttpRequest.newBuilder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/friends"))
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .GET()
                        .build();
            }
        },
        ;

        private static final String X_API_KEY = "x-api-key";

        public abstract HttpRequest build(BabyDogePawsGameRequestParam param);

    }
}
