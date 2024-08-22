package red.zyc.babydogepaws.selenium;

import org.openqa.selenium.By;

/**
 * 元素位置
 *
 * @author allurx
 */
public final class ElementPosition {

    private ElementPosition() {
    }

    /**
     * baby-doge-paws聊天列表
     */
    public static final String BABY_DAGE_PAWS_CHAT_CSS_SELECTOR = "a[href=\"#7413313712\"]";
    public static final By BABY_DAGE_PAWS_CHAT = By.cssSelector(BABY_DAGE_PAWS_CHAT_CSS_SELECTOR);

    /**
     * 开始baby-doge-paws游戏的play按钮
     */
    public static final String BABY_DAGE_PAWS_PLAY_BUTTON_CSS_SELECTOR = "button[title=\"Open bot command keyboard\"]";
    public static final By BABY_DAGE_PAWS_PLAY_BUTTON = By.cssSelector(BABY_DAGE_PAWS_PLAY_BUTTON_CSS_SELECTOR);

    public static final String BABY_DAGE_PAWS_WEB_APP_CONFIRM_BUTTON = "return Array.from(document.querySelectorAll(\"button\")).find(button => button.textContent.includes(\"Confirm\"))";

    /**
     * 游戏加载后的iframe
     */
    public static final String BABY_DAGE_PAWS_WEB_APP_CSS_SELECTOR = "iframe[title=\"BabyDoge PAWS Web App\"]";
    public static final By BABY_DAGE_PAWS_WEB_APP = By.cssSelector(BABY_DAGE_PAWS_WEB_APP_CSS_SELECTOR);

}
