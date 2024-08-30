package red.zyc.babydogepaws.core;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import red.zyc.babydogepaws.common.Poller;
import red.zyc.babydogepaws.common.util.Commons;
import red.zyc.babydogepaws.common.util.Mails;
import red.zyc.babydogepaws.dao.LoginInfoMapper;
import red.zyc.babydogepaws.dao.UserMapper;
import red.zyc.babydogepaws.exception.BabyDogePawsException;
import red.zyc.babydogepaws.model.persistent.BabyDogePawsUser;
import red.zyc.babydogepaws.model.request.BabyDogePawsGameRequestParam;
import red.zyc.babydogepaws.selenium.ElementPosition;
import red.zyc.babydogepaws.selenium.Javascript;
import red.zyc.selenium.browser.Chrome;
import red.zyc.selenium.browser.Mode;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author allurx
 */
@Service
public class BabyDogePaws {

    private static final Logger LOGGER = LoggerFactory.getLogger(BabyDogePaws.class);
    private static final String TELEGRAM_WEB_URL = "https://web.telegram.org/a/";
    private static final List<String> AUTH_PARAM_NAMES = List.of("query_id", "user", "auth_date", "hash");
    private static final String BABY_DOGE_PAWS_AUTH_PARAMS_LOCATION_PREFIX = "https://babydogeclikerbot.com/#tgWebAppData=";

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
                .addArgs("--user-data-dir=" + user.chromeDataDir(), "--headless=new")
                .build()) {

            var webDriver = chrome.webDriver();
            var waiter = new WebDriverWait(webDriver, Duration.ofSeconds(60L), Duration.ofSeconds(1L));
            var javascriptExecutor = (JavascriptExecutor) webDriver;

            // 加载页面
            webDriver.get(TELEGRAM_WEB_URL);

            // 等待页面加载完毕
            Poller.<JavascriptExecutor, Boolean>builder()
                    .duration(60L)
                    .interval(1L)
                    .timeUnit(TimeUnit.SECONDS)
                    .function(e -> (Boolean) e.executeScript(Javascript.TELEGRAM_PAGE_LOADED))
                    .input(javascriptExecutor)
                    .predicate(r -> r)
                    .build()
                    .throwWhenMiss(() -> new BabyDogePawsException("telegram页面加载失败"));

            // 点击baby-doge-paws聊天室
            waiter.until(ExpectedConditions.elementToBeClickable(ElementPosition.BABY_DAGE_PAWS_CHAT)).click();

            // 点击play按钮
            waiter.until(ExpectedConditions.elementToBeClickable(ElementPosition.BABY_DAGE_PAWS_PLAY_BUTTON)).click();

            // 第一次play会出现一个confirm按钮
            Poller.<JavascriptExecutor, WebElement>builder()
                    .duration(3000L)
                    .interval(500L)
                    .timeUnit(TimeUnit.MILLISECONDS)
                    .function(e -> (WebElement) e.executeScript(ElementPosition.BABY_DAGE_PAWS_WEB_APP_CONFIRM_BUTTON))
                    .input(javascriptExecutor)
                    .predicate(Objects::nonNull)
                    .build()
                    .poll()
                    .ifPresent(WebElement::click);

            // 切换到游戏加载后所在的iframe
            waiter.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(ElementPosition.BABY_DAGE_PAWS_WEB_APP));
            var w = webDriver.switchTo().parentFrame();

            // 从iframe的src中提取游戏所需的authParam
            var babyDogePawsWebAppUrl = w.findElement(ElementPosition.BABY_DAGE_PAWS_WEB_APP).getAttribute("src");
            var decode = URLDecoder.decode(babyDogePawsWebAppUrl.substring(BABY_DOGE_PAWS_AUTH_PARAMS_LOCATION_PREFIX.length()), StandardCharsets.UTF_8);
            var authParam = Arrays.stream(decode.split("&"))
                    .map(s -> s.split("="))
                    .filter(arr -> AUTH_PARAM_NAMES.contains(arr[0]))
                    .map(arr -> arr[0] + "=" + arr[1])
                    .reduce("", (s1, s2) -> s1 + "&" + s2).substring(1)
                    .describeConstable()
                    .orElseThrow(() -> new BabyDogePawsException("BabyDogePaws授权参数为null"));

            // 其它线程执行任务时就能感知到最新的authParam了
            user.authParam = authParam;

            // 保存或更新登录信息
            loginInfoMapper.saveOrUpdateLoginInfo(user.id, LocalDateTime.now(), authParam);

            LOGGER.info("[游戏登录成功]-{}:{}", user.phoneNumber, authParam);

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
