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
     * 开始baby-doge-paws游戏的play按钮
     */
    public static final By BABY_DAGE_PAWS_PLAY_BUTTON = By.cssSelector("button[title=\"Open bot command keyboard\"]");

    /**
     * 游戏加载后的iframe
     */
    public static final By BABY_DAGE_PAWS_WEB_APP = By.cssSelector("iframe[title=\"BabyDoge PAWS Web App\"]");

}
