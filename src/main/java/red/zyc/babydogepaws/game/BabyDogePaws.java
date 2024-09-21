package red.zyc.babydogepaws.game;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import red.zyc.babydogepaws.common.constant.Constants;
import red.zyc.babydogepaws.common.util.CommonUtil;
import red.zyc.babydogepaws.common.util.MailUtil;
import red.zyc.babydogepaws.dao.LoginInfoMapper;
import red.zyc.babydogepaws.dao.TelegramUserMapper;
import red.zyc.babydogepaws.dao.UserMapper;
import red.zyc.babydogepaws.exception.BabyDogePawsException;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;
import red.zyc.babydogepaws.model.request.BabyDogePawsGameRequestParam;
import red.zyc.babydogepaws.selenium.SeleniumSupport;
import red.zyc.kit.base.concurrency.CallableFunction;
import red.zyc.kit.base.concurrency.Poller;
import red.zyc.kit.selenium.Chrome;
import red.zyc.kit.selenium.Mode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static red.zyc.babydogepaws.selenium.ElementPosition.BABY_DAGE_PAWS_PLAY_BUTTON;
import static red.zyc.babydogepaws.selenium.ElementPosition.BABY_DAGE_PAWS_WEB_APP;
import static red.zyc.babydogepaws.selenium.Javascript.*;
import static red.zyc.babydogepaws.selenium.SeleniumSupport.executeScript;
import static red.zyc.kit.base.concurrency.Poller.throwingRunnable;
import static red.zyc.kit.json.JsonOperator.JACKSON_OPERATOR;

/**
 * @author allurx
 */
@Service
public class BabyDogePaws {

    private static final Logger LOGGER = LoggerFactory.getLogger(BabyDogePaws.class);
    private static final String BABY_DOGE_PAWS_URL = "https://web.telegram.org/a/#7413313712";
    private static final String TELEGRAM_URL = "https://web.telegram.org/a/";

    private final Environment environment;
    private final TelegramUserMapper telegramUserMapper;
    private final UserMapper userMapper;
    private final LoginInfoMapper loginInfoMapper;
    private final BabyDogePawsTask babyDogePawsTask;

    public BabyDogePaws(Environment environment, TelegramUserMapper telegramUserMapper, UserMapper userMapper,
                        LoginInfoMapper loginInfoMapper,
                        BabyDogePawsTask babyDogePawsTask) {
        this.environment = environment;
        this.telegramUserMapper = telegramUserMapper;
        this.userMapper = userMapper;
        this.loginInfoMapper = loginInfoMapper;
        this.babyDogePawsTask = babyDogePawsTask;
    }

    /**
     * 启动
     */
    public void bootstrap() {
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            LOGGER.info("BabyDoge Paws is launching");
            userMapper.listBabyDogeUsers().forEach(user -> babyDogePawsTask.schedule(new BabyDogePawsGameRequestParam(user)));
            LOGGER.info("BabyDoge Paws launches successfully");
        }
    }

    /**
     * 通过selenium模拟玩BabyDogePaws游戏
     *
     * @param user    {@link BabyDogePawsUser}
     * @param failNum 模拟玩的过程失败次数，提供容错性
     */
    public void playBabyDogePaws(BabyDogePawsUser user, int failNum) {
        try (var chrome = Chrome.builder()
                .mode(Mode.ATTACH)
                .addArgs("--user-data-dir=%s".formatted(user.chromeDataDir()), "--headless=new")
                .build()) {

            var webDriver = chrome.webDriver();
            var jsExecutor = (JavascriptExecutor) webDriver;

            // 加载页面
            webDriver.get(BABY_DOGE_PAWS_URL);

            // 等待页面加载完毕
            Poller.<JavascriptExecutor, Boolean>builder()
                    .timing(Duration.ofSeconds(60), Duration.ofSeconds(1))
                    .<CallableFunction<JavascriptExecutor, Boolean>>execute(jsExecutor, o -> executeScript(o, RETURN_TELEGRAM_LOGIN_SUCCESS))
                    .until(r -> r)
                    .onTimeout(throwingRunnable(() -> new BabyDogePawsException("telegram页面加载失败")))
                    .build()
                    .get();

            // 点击play按钮
            Poller.<WebDriver, Boolean>builder()
                    .timing(Duration.ofSeconds(30), Duration.ofMillis(500))
                    .<CallableFunction<WebDriver, Boolean>>execute(webDriver, o -> Optional.ofNullable(ExpectedConditions.elementToBeClickable(BABY_DAGE_PAWS_PLAY_BUTTON).apply(o))
                            .map(element -> {
                                element.click();
                                return true;
                            }).orElse(false))
                    .until(b -> b)
                    .onTimeout(throwingRunnable(() -> new BabyDogePawsException("找不到play按钮")))
                    .ignoreExceptions(Throwable.class)
                    .build()
                    .get();

            // 第一次play会出现一个confirm按钮
            Poller.<JavascriptExecutor, Boolean>builder()
                    .timing(Duration.ofSeconds(10), Duration.ofMillis(1000))
                    .<CallableFunction<JavascriptExecutor, Boolean>>execute(jsExecutor, o -> Optional.ofNullable(SeleniumSupport.<WebElement>executeScript(o, RETURN_BABY_DAGE_PAWS_WEB_APP_CONFIRM_BUTTON))
                            .map(element -> {
                                element.click();
                                return true;
                            }).orElse(false))
                    .until(b -> b)
                    .ignoreExceptions(Throwable.class)
                    .build()
                    .get();

            // 定位游戏iframe，定位成功后webdriver就会切换到这个iframe中
            Poller.<WebDriver, WebDriver>builder()
                    .timing(Duration.ofSeconds(60), Duration.ofMillis(1000))
                    .<CallableFunction<WebDriver, WebDriver>>execute(webDriver, o -> ExpectedConditions.frameToBeAvailableAndSwitchToIt(BABY_DAGE_PAWS_WEB_APP).apply(o))
                    .until(Objects::nonNull)
                    .ignoreExceptions(NoSuchElementException.class)
                    .onTimeout(throwingRunnable(() -> new BabyDogePawsException("定位游戏iframe失败")))
                    .build()
                    .get();

            // 修改sessionStorage模拟手机登录
            String key1 = "telegram-apps/launch-params";
            String key2 = "telegram-apps/mini-app";
            var items = Poller.<JavascriptExecutor, List<String>>builder()
                    .timing(Duration.ofSeconds(30), Duration.ofMillis(1000))
                    .<CallableFunction<JavascriptExecutor, List<String>>>execute(jsExecutor, o -> executeScript(jsExecutor, RETURN_TELEGRAM_APPS_SESSION_STORAGE_ITEMS, key1, key2))
                    .until(o -> o != null && o.size() == 2 && o.getFirst() != null && o.get(1) != null)
                    .build()
                    .getOptional()
                    .orElseThrow(() -> new BabyDogePawsException("获取sessionStorage失败"));

            var mockPhoneLaunchParams = items.getFirst().replaceFirst("tgWebAppPlatform=weba", "tgWebAppPlatform=ios");
            jsExecutor.executeScript(SET_TELEGRAM_APPS_SESSION_STORAGE_ITEM, key1, mockPhoneLaunchParams);

            // 保存tg用户信息
            @SuppressWarnings("unchecked")
            var initData = (Map<String, Object>) JACKSON_OPERATOR.<Map<String, Object>>fromJsonString(items.get(1), Map.class)
                    .getOrDefault("initData", new HashMap<>());
            @SuppressWarnings("unchecked")
            var tgUser = (Map<String, Object>) initData.getOrDefault("user", new HashMap<>());
            telegramUserMapper.saveOrUpdateTelegramUser(
                    user.id,
                    Long.parseLong(tgUser.getOrDefault("id", -1).toString()),
                    String.valueOf(tgUser.getOrDefault("username", ""))
            );

            // 重新加载iframe使其能够在web端显示（reload后webdriver依旧在iframe中）
            jsExecutor.executeScript(RELOAD_PAGE);
            Poller.<JavascriptExecutor, WebElement>builder()
                    .timing(Duration.ofSeconds(30), Duration.ofMillis(1000))
                    .<CallableFunction<JavascriptExecutor, WebElement>>execute(jsExecutor, o -> executeScript(o, RETURN_BABY_DAGE_PAWS_WEB_APP_TAB))
                    .until(Objects::nonNull)
                    .onTimeout(throwingRunnable(() -> new BabyDogePawsException("模拟手机登录失败")))
                    .build()
                    .get();

            // 其它线程执行任务时就能感知到最新的authParam了
            user.authParam = JACKSON_OPERATOR.fromJsonString(items.get(1), Constants.OBJECT_DATA_TYPE).get("initDataRaw") +"&referrer=";

            // 保存或更新登录信息
            loginInfoMapper.saveOrUpdateLoginInfo(user.id, LocalDateTime.now(), user.authParam);

            LOGGER.info("[游戏登录成功]-{}:{}", user.phoneNumber, user.authParam);

        } catch (Throwable t) {

            var currentFailNum = ++failNum;
            LOGGER.error(String.format("[游戏登录失败]-%s-%s:", user.phoneNumber, currentFailNum), t);

            // 游戏登录失败3次则取消所有定时任务
            if (currentFailNum == 3) {
                user.cancelAllTask();
                if (isTelegramBanned(user)) {
                    telegramUserMapper.updateTelegramUserBanned(user.id, 1);
                    MailUtil.sendTextMail(user.phoneNumber + "telegram被ban了", "");
                } else {
                    MailUtil.sendTextMail(user.phoneNumber + "游戏登录失败", CommonUtil.convertThrowableToString(t));
                }
            } else {

                // 通过try with resource语法关闭chrome和webdriver进程可能需要时间
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));

                // 重新尝试
                playBabyDogePaws(user, currentFailNum);

            }
        }
    }


    private boolean isTelegramBanned(BabyDogePawsUser user) {
        try (var chrome = Chrome.builder()
                .mode(Mode.ATTACH)
                .addArgs("--user-data-dir=%s".formatted(user.chromeDataDir()), "--headless=new")
                .build()) {

            var webDriver = chrome.webDriver();
            var jsExecutor = (JavascriptExecutor) webDriver;

            // 加载telegram页面
            webDriver.get(TELEGRAM_URL);

            // 判断当前手机号是不是被ban了
            return Poller.<JavascriptExecutor, Boolean>builder()
                    .timing(Duration.ofSeconds(60), Duration.ofSeconds(1))
                    .<CallableFunction<JavascriptExecutor, Boolean>>execute(jsExecutor, o -> Optional.ofNullable(SeleniumSupport.<WebElement>executeScript(o, "return Array.from(document.querySelectorAll(\"button\")).find(button => button.textContent.includes(\"Log in by phone Number\"))"))
                            .map(element -> {
                                element.click();
                                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
                                var phoneNumberInput = webDriver.findElement(By.id("sign-in-phone-number"));
                                phoneNumberInput.clear();
                                phoneNumberInput.sendKeys(user.areaCode + user.phoneNumber);
                                var submitButton = webDriver.findElement(By.cssSelector("button[type=\"submit\"]"));
                                submitButton.click();
                                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
                                return ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("label[for=\"sign-in-phone-number\"]"), "This phone number is banned.").apply((WebDriver) jsExecutor);
                            }).orElse(false))
                    .until(b -> b)
                    .onTimeout(() -> LOGGER.warn("[无法确定游戏登录失败的原因]-{}", user.phoneNumber))
                    .ignoreExceptions(Throwable.class)
                    .build()
                    .get();
        }
    }


}
