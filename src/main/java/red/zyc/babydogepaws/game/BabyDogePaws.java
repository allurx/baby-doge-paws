package red.zyc.babydogepaws.game;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import red.zyc.babydogepaws.common.constant.Constants;
import red.zyc.babydogepaws.common.util.Commons;
import red.zyc.babydogepaws.common.util.Mails;
import red.zyc.babydogepaws.dao.LoginInfoMapper;
import red.zyc.babydogepaws.dao.UserMapper;
import red.zyc.babydogepaws.exception.BabyDogePawsException;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;
import red.zyc.babydogepaws.model.request.BabyDogePawsGameRequestParam;
import red.zyc.kit.core.poller.CallableFunction;
import red.zyc.kit.core.poller.Poller;
import red.zyc.selenium.browser.Chrome;
import red.zyc.selenium.browser.Mode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static red.zyc.babydogepaws.selenium.ElementPosition.BABY_DAGE_PAWS_PLAY_BUTTON;
import static red.zyc.babydogepaws.selenium.ElementPosition.BABY_DAGE_PAWS_WEB_APP;
import static red.zyc.babydogepaws.selenium.Javascript.*;
import static red.zyc.babydogepaws.selenium.SeleniumSupport.executeScript;
import static red.zyc.kit.core.poller.Poller.throwingRunnable;
import static red.zyc.kit.json.JsonOperator.JACKSON_OPERATOR;

/**
 * @author allurx
 */
@Service
public class BabyDogePaws {

    private static final Logger LOGGER = LoggerFactory.getLogger(BabyDogePaws.class);
    private static final String BABY_DOGE_PAWS_URL = "https://web.telegram.org/a/#7413313712";

    private final UserMapper userMapper;
    private final LoginInfoMapper loginInfoMapper;
    private final BabyDogePawsTask babyDogePawsTask;

    @Value("${baby-doge-paws.bootstrap}")
    private List<String> bootstrap;

    public BabyDogePaws(UserMapper userMapper,
                        LoginInfoMapper loginInfoMapper,
                        BabyDogePawsTask babyDogePawsTask) {
        this.userMapper = userMapper;
        this.loginInfoMapper = loginInfoMapper;
        this.babyDogePawsTask = babyDogePawsTask;
    }

    /**
     * 启动
     */
    public void bootstrap() {
        if (bootstrap.isEmpty()) {
            userMapper.listBabyDogeUsers().forEach(user -> babyDogePawsTask.schedule(new BabyDogePawsGameRequestParam(user)));
        } else {
            bootstrap.stream().map(userMapper::getBabyDogeUser).forEach(user -> babyDogePawsTask.schedule(new BabyDogePawsGameRequestParam(user)));
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
                    .predicate(r -> r)
                    .onTimeout(throwingRunnable(() -> new BabyDogePawsException("telegram页面加载失败")))
                    .build()
                    .poll();

            // 点击play按钮
            Poller.<WebDriver, WebElement>builder()
                    .timing(Duration.ofSeconds(60), Duration.ofMillis(500))
                    .<CallableFunction<WebDriver, WebElement>>execute(webDriver, o -> ExpectedConditions.elementToBeClickable(BABY_DAGE_PAWS_PLAY_BUTTON).apply(o))
                    .predicate(Objects::nonNull)
                    .onTimeout(throwingRunnable(() -> new BabyDogePawsException("找不到play按钮")))
                    .build()
                    .poll()
                    .ifPresent(WebElement::click);

            // 第一次play会出现一个confirm按钮
            Poller.<JavascriptExecutor, WebElement>builder()
                    .timing(Duration.ofSeconds(10), Duration.ofMillis(500))
                    .<CallableFunction<JavascriptExecutor, WebElement>>execute(jsExecutor, o -> executeScript(o, RETURN_BABY_DAGE_PAWS_WEB_APP_CONFIRM_BUTTON))
                    .predicate(Objects::nonNull)
                    .build()
                    .poll()
                    .ifPresent(WebElement::click);

            // 1、定位游戏iframe，定位成功后webdriver就会切换到这个iframe中
            Poller.<WebDriver, WebDriver>builder()
                    .timing(Duration.ofSeconds(60), Duration.ofMillis(500))
                    .<CallableFunction<WebDriver, WebDriver>>execute(webDriver, o -> ExpectedConditions.frameToBeAvailableAndSwitchToIt(BABY_DAGE_PAWS_WEB_APP).apply(o))
                    .predicate(Objects::nonNull)
                    .ignoreExceptions(NoSuchElementException.class)
                    .onTimeout(throwingRunnable(() -> new BabyDogePawsException("定位游戏iframe失败")))
                    .build()
                    .poll();

            // 2、修改sessionStorage模拟手机登录
            String key1 = "telegram-apps/launch-params";
            String key2 = "telegram-apps/mini-app";
            var items = Poller.<JavascriptExecutor, List<String>>builder()
                    .timing(Duration.ofSeconds(30), Duration.ofMillis(500))
                    .<CallableFunction<JavascriptExecutor, List<String>>>execute(jsExecutor, o -> executeScript(jsExecutor, RETURN_TELEGRAM_APPS_SESSION_STORAGE_ITEMS, key1, key2))
                    .predicate(o -> o != null && o.size() == 2 && o.getFirst() != null && o.get(1) != null)
                    .build()
                    .poll()
                    .orElseThrow(() -> new BabyDogePawsException("获取sessionStorage失败"));

            String mockPhoneLaunchParams = items.getFirst().replaceFirst("tgWebAppPlatform=weba", "tgWebAppPlatform=ios");
            jsExecutor.executeScript(SET_TELEGRAM_APPS_SESSION_STORAGE_ITEM, key1, mockPhoneLaunchParams);

            // 3、重新加载iframe使其能够在web端显示（reload后webdriver依旧在iframe中）
            jsExecutor.executeScript(RELOAD_PAGE);
            Poller.<JavascriptExecutor, WebElement>builder()
                    .timing(Duration.ofSeconds(30), Duration.ofMillis(500))
                    .<CallableFunction<JavascriptExecutor, WebElement>>execute(jsExecutor, o -> executeScript(o, RETURN_BABY_DAGE_PAWS_WEB_APP_TAB))
                    .predicate(Objects::nonNull)
                    .onTimeout(throwingRunnable(() -> new BabyDogePawsException("模拟手机登录失败")))
                    .build()
                    .poll();

            // 其它线程执行任务时就能感知到最新的authParam了
            user.authParam = (String) JACKSON_OPERATOR.fromJsonString(items.get(1), Constants.OBJECT_DATA_TYPE).get("initDataRaw");

            // 保存或更新登录信息
            loginInfoMapper.saveOrUpdateLoginInfo(user.id, LocalDateTime.now(), user.authParam);

            LOGGER.info("[游戏登录成功]-{}:{}", user.phoneNumber, user.authParam);

        } catch (Throwable t) {

            var currentFailNum = ++failNum;
            LOGGER.error(String.format("[游戏登录失败]-%s-%s:", user.phoneNumber, currentFailNum), t);

            // 游戏登录失败3次则取消所有定时任务
            if (currentFailNum == 3) {
                user.cancelAllTask();
                Mails.sendTextMail(user.phoneNumber + "游戏登录失败", Commons.convertThrowableToString(t));
            } else {

                // 通过try with resource语法关闭chrome和webdriver进程可能需要时间
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));

                // 重新尝试
                playBabyDogePaws(user, currentFailNum);

            }
        }
    }

}
