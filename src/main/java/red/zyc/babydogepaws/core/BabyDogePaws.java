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
import red.zyc.babydogepaws.model.BabyDogePawsAccount;
import red.zyc.babydogepaws.model.persistent.GameLoginInfo;
import red.zyc.babydogepaws.model.request.BabyDogePawsGameRequestParam;
import red.zyc.babydogepaws.selenium.ElementPosition;
import red.zyc.babydogepaws.selenium.Javascript;
import red.zyc.selenium.browser.Chrome;
import red.zyc.selenium.browser.Mode;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

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

    private final UserMapper userMapper;
    private final GameLoginInfoMapper gameLoginInfoMapper;
    private final BabyDogePawsTask babyDogePawsTask;
    private final Predicate<BabyDogePawsAccount> bootStrapAccountPredicate = account -> account.name.equals("19962006575") || account.name.equals("8502950634");

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
        userMapper.listBabyDogeUsers()
                .stream()
                .map(user -> new BabyDogePawsAccount(user, chromeRootDataDir))
                .filter(bootStrapAccountPredicate)
                .forEach(account -> bootstrap(account, 0));
    }

    private void bootstrap(BabyDogePawsAccount account, int failNum) {
        BOOTSTRAP_SERVICE.execute(() -> {
            if (Commons.isEmpty(account.user.authParam)) {
                playBabyDogePaws(account, failNum);
            } else {
                babyDogePawsTask.schedule(new BabyDogePawsGameRequestParam(account));
            }
        });
    }

    public void playBabyDogePaws(BabyDogePawsAccount account, int failNum) {
        try (Chrome chrome = Chrome.builder()
                .mode(Mode.ATTACH)
                .addArgs("--user-data-dir=" + account.chromeDataDir, "--headless=new")
                .build()) {

            LocalDateTime startTime = LocalDateTime.now();
            WebDriver webDriver = chrome.webDriver();
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
                    .throwWhenMiss(() -> new BabyDogePawsException("telegram页面加载失败"));

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

            int duration = (int) startTime.until(LocalDateTime.now(), ChronoUnit.SECONDS);
            account.user.authParam = authParam;

            // 启动所有定时任务
            babyDogePawsTask.schedule(new BabyDogePawsGameRequestParam(account));

            // 保存登录信息
            gameLoginInfoMapper.saveOrUpdateGameLoginInfo(new GameLoginInfo(account.user.id, account.user.authParam, duration));

            LOGGER.info("[游戏登录成功]-{}:{}", account.name, authParam);

        } catch (Throwable t) {

            int currentFailNum = ++failNum;
            LOGGER.error(String.format("[游戏登录失败]-%s-%s:", account.name, currentFailNum), t);

            // 游戏登录失败6次放弃
            if (currentFailNum == 6) {
                Mails.sendTextMail(account.name + "游戏登录失败", Commons.convertThrowableToString(t));
            } else {

                // 继续丢到线程池的任务队列中排队执行，默认的就是FIFO队列
                bootstrap(account, currentFailNum);
            }
        }
    }

}
