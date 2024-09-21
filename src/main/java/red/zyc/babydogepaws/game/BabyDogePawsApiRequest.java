package red.zyc.babydogepaws.game;

import red.zyc.babydogepaws.model.request.BabyDogePawsGameRequestParam;
import red.zyc.babydogepaws.model.request.Mine;
import red.zyc.babydogepaws.model.request.ResolveChannel;
import red.zyc.babydogepaws.model.request.UpgradeCard;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Map;
import java.util.Optional;

import static red.zyc.kit.json.JsonOperator.JACKSON_OPERATOR;

/**
 * @author allurx
 */
public final class BabyDogePawsApiRequest {

    private BabyDogePawsApiRequest() {
    }

    public enum Request {

        AUTHORIZE {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return builder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/authorize"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(Optional.ofNullable(param.user.authParam).orElse("")))
                        .build();
            }
        },

        GET_ME {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return builder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/getMe"))
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .GET()
                        .build();
            }
        },

        PICK_DAILY_BONUS {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return builder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/pickDailyBonus"))
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .POST(HttpRequest.BodyPublishers.ofString(""))
                        .build();
            }
        },

        GET_DAILY_BONUSES {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return builder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/getDailyBonuses"))
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .GET()
                        .build();
            }
        },

        LIST_CARDS {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return builder()
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
                return builder()
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
                Mine mine = (Mine) param;
                return builder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/mine"))
                        .header("content-type", "application/json")
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .POST(HttpRequest.BodyPublishers.ofString(JACKSON_OPERATOR.toJsonString(Map.of("count", mine.count))))
                        .build();
            }
        },

        LIST_CHANNEL {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return builder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/channels"))
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .GET()
                        .build();
            }
        },

        RESOLVE_CHANNEL {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                ResolveChannel resolveChannel = (ResolveChannel) param;
                return builder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/channels-resolve"))
                        .header("content-type", "application/json")
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .POST(HttpRequest.BodyPublishers.ofString(JACKSON_OPERATOR.toJsonString(Map.of("channel_id", resolveChannel.channel.id()))))
                        .build();
            }
        },

        PICK_CHANNEL {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                ResolveChannel resolveChannel = (ResolveChannel) param;
                return builder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/channels"))
                        .header("content-type", "application/json")
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .POST(HttpRequest.BodyPublishers.ofString(JACKSON_OPERATOR.toJsonString(Map.of("channel_id", resolveChannel.channel.id()))))
                        .build();
            }
        },

        PICK_PROMO {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return builder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/promo"))
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .POST(HttpRequest.BodyPublishers.ofString(""))
                        .build();
            }
        },

        GET_PROMO {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return builder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/promo"))
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .GET()
                        .build();
            }
        },

        LIST_FRIENDS {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return builder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/friends"))
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .GET()
                        .build();
            }
        },

        GET_BOOSTS {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return builder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/boosts"))
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .GET()
                        .build();
            }
        },

        USE_FULL_ENERGY_BOOSTS {
            @Override
            public HttpRequest build(BabyDogePawsGameRequestParam param) {
                return builder()
                        .uri(URI.create("https://backend.babydogepawsbot.com/boosts"))
                        .header("content-type", "application/json")
                        .header(X_API_KEY, Optional.ofNullable(param.user.xApiKey).orElse(""))
                        .POST(HttpRequest.BodyPublishers.ofString(JACKSON_OPERATOR.toJsonString(Map.of("boost", "full_energy"))))
                        .build();
            }
        },
        ;

        private static final String X_API_KEY = "x-api-key";

        public abstract HttpRequest build(BabyDogePawsGameRequestParam param);

        public HttpRequest.Builder builder() {
            return HttpRequest.newBuilder()
                    .header("Accept", "application/json, text/plain, */*")
                    .header("sec-fetch-site", "cross-site")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("sec-fetch-mode", "cors")
                    .header("Origin", "https://babydogeclikerbot.com")
                    .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_6_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148")
                    .header("Referer", "https://babydogeclikerbot.com/")
                    .header("sec-fetch-dest", "empty");
        }

    }
}
