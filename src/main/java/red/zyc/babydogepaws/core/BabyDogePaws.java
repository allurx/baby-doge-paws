package red.zyc.babydogepaws.core;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import red.zyc.babydogepaws.common.NamedThreadFactory;
import red.zyc.babydogepaws.common.Poller;
import red.zyc.babydogepaws.common.util.Commons;
import red.zyc.babydogepaws.common.util.Mails;
import red.zyc.babydogepaws.dao.GameLoginInfoMapper;
import red.zyc.babydogepaws.dao.UserMapper;
import red.zyc.babydogepaws.exception.BabyDogePawsException;
import red.zyc.babydogepaws.model.User;
import red.zyc.babydogepaws.model.persistent.GameLoginInfo;
import red.zyc.babydogepaws.selenium.BabyDogePawsContext;
import red.zyc.babydogepaws.selenium.BabyDogePawsContextItem;
import red.zyc.babydogepaws.selenium.ChromeSupport;
import red.zyc.babydogepaws.selenium.ElementPosition;
import red.zyc.babydogepaws.selenium.Javascript;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static red.zyc.toolkit.json.Json.JACKSON_OPERATOR;

/**
 * @author allurx
 */
@Service
public class BabyDogePaws {

    private static final Logger LOGGER = LoggerFactory.getLogger(BabyDogePaws.class);
    private static final String TELEGRAM_WEB_URL = "https://web.telegram.org/a/";
    private static final List<String> AUTH_PARAM_NAMES = List.of("query_id", "user", "auth_date", "hash");
    private static final String BABY_DOGE_PAWS_AUTH_PARAMS_LOCATION_PREFIX = "https://babydogeclikerbot.com/#tgWebAppData=";
    private static final ThreadPoolExecutor BOOTSTRAP_SERVICE = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("Bootstrap", false));
    private static final List<String> IGNORED_USER = List.of("86-19962006575", "1-8502950634");

    private final UserMapper userMapper;
    private final GameLoginInfoMapper gameLoginInfoMapper;
    private final BabyDogePawsTask babyDogePawsTask;

    @Value("${chrome.root-data-dir}")
    private String chromeRootDataDir;

    public BabyDogePaws(UserMapper userMapper,
                        GameLoginInfoMapper gameLoginInfoMapper,
                        BabyDogePawsTask babyDogePawsTask) {
        this.userMapper = userMapper;
        this.gameLoginInfoMapper = gameLoginInfoMapper;
        this.babyDogePawsTask = babyDogePawsTask;
    }

    public void bootstrap() {
        userMapper.listTelegramUsers()
                .stream()
                .filter(user -> user.banned() == 0)
                .filter(user -> !IGNORED_USER.contains(user.areaCode() + "-" + user.phoneNumber()))
                .forEach(user -> bootstrap(new User(user, chromeRootDataDir), 0));
    }

    private void bootstrap(User user, int failNum) {
        BOOTSTRAP_SERVICE.execute(() -> {
                    if (playBabyDogePaws(user, failNum)) {
                        babyDogePawsTask.scheduleAuthorize(user);
                        babyDogePawsTask.schedulePickDailyBonus(user);
                        babyDogePawsTask.schedulePickPromo(user);
                        babyDogePawsTask.scheduleMine(user);
                        babyDogePawsTask.scheduleUpgradeCard(user);
                    }
                }
        );
    }

    public boolean playBabyDogePaws(User user, int failNum) {
        try {
            LocalDateTime startTime = LocalDateTime.now();
            ChromeSupport.startChromeProcess(user.chromeDataDir);
            WebDriver webDriver = ChromeSupport.startChromeDriver();
            webDriver.get(TELEGRAM_WEB_URL);
            FluentWait<WebDriver> waiter = new WebDriverWait(webDriver, Duration.ofSeconds(30L), Duration.ofSeconds(1L));
            JavascriptExecutor executor = (JavascriptExecutor) webDriver;

            // 等待页面加载完毕
            Poller.<JavascriptExecutor, Boolean>builder()
                    .duration(20000L)
                    .interval(500L)
                    .timeUnit(TimeUnit.MILLISECONDS)
                    .function((e) -> (Boolean) e.executeScript(Javascript.WINDOW_LOADED))
                    .input(executor)
                    .predicate((r) -> r)
                    .build()
                    .pollWhenMiss(() -> new BabyDogePawsException("telegram页面加载失败"));

            // 点击baby-doge-paws聊天室
            waiter.until(ExpectedConditions.elementToBeClickable(ElementPosition.BABY_DAGE_PAWS_CHAT)).click();

            // 点击play按钮
            waiter.until(ExpectedConditions.elementToBeClickable(ElementPosition.BABY_DAGE_PAWS_PLAY_BUTTON)).click();

            // 第一次play会出现一个confirm按钮
            Poller.<JavascriptExecutor, WebElement>builder()
                    .duration(6000L)
                    .interval(500L)
                    .timeUnit(TimeUnit.MILLISECONDS)
                    .function((e) -> (WebElement) e.executeScript("return Array.from(document.querySelectorAll(\"button\")).find(button => button.textContent.includes(\"Confirm\"))"))
                    .input(executor)
                    .predicate(Objects::nonNull)
                    .build()
                    .poll()
                    .ifPresent(WebElement::click);

            // 切换到游戏加载后所在的iframe
            waiter.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(ElementPosition.BABY_DAGE_PAWS_WEB_APP));
            WebDriver w = webDriver.switchTo().parentFrame();

            // 从iframe的src中提取游戏所需的authParam
            String babyDogePawsWebAppUrl = w.findElement(ElementPosition.BABY_DAGE_PAWS_WEB_APP).getAttribute("src");
            String decode = URLDecoder.decode(babyDogePawsWebAppUrl.substring(BABY_DOGE_PAWS_AUTH_PARAMS_LOCATION_PREFIX.length()), StandardCharsets.UTF_8);
            String authParam = Arrays.stream(decode.split("&"))
                    .map((s) -> s.split("="))
                    .filter((arr) -> AUTH_PARAM_NAMES.contains(arr[0]))
                    .map((arr) -> arr[0] + "=" + arr[1])
                    .reduce("", (s1, s2) -> s1 + "&" + s2).substring(1)
                    .describeConstable()
                    .orElseThrow(() -> new BabyDogePawsException("BabyDogePaws授权参数为null"));

            user.authParam = authParam;
            LOGGER.info("[获取授权参数成功]-{}:{}", user.name, authParam);

            // 成功获取到authParam后调用一下游戏的授权接口获取x-api-key
            babyDogePawsTask.authorizeOnceImmediately(user);
            String userJsonData = JACKSON_OPERATOR.toJsonString(user.data);
            LOGGER.info("[游戏登录成功]-{}:{}", user.name, userJsonData);
            return gameLoginInfoMapper.saveOrUpdateGameLoginInfo(new GameLoginInfo(user.user.id(), user.authParam, (int) startTime.until(LocalDateTime.now(), ChronoUnit.SECONDS))) == 1;
        } catch (Throwable t) {

            int currentFailNum = ++failNum;
            LOGGER.error(String.format("[游戏登录失败]-%s-%s:", user.name, currentFailNum), t);

            // 游戏登录失败6次放弃
            if (currentFailNum == 6) {
                Mails.sendTextMail(user.name + "游戏登录失败", Commons.convertThrowableToString(t));
            } else {
                bootstrap(user, currentFailNum);
            }
            return false;
        } finally {
            Optional.ofNullable(BabyDogePawsContext.<WebDriver>get(BabyDogePawsContextItem.WEB_DRIVER)).ifPresent(WebDriver::quit);
            Optional.ofNullable(BabyDogePawsContext.<Process>get(BabyDogePawsContextItem.CHROME_PROCESS)).ifPresent(Process::destroy);
            BabyDogePawsContext.remove();
        }
    }

}
