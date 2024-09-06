package red.zyc.babydogepaws.selenium;

/**
 * @author allurx
 */
public final class Javascript {

    private Javascript() {
    }

    // 重新加载页面
    public static final String RELOAD_PAGE = "location.reload()";

    // 判断document是否加载完成
    public static final String RETURN_DOCUMENT_READY_STATE_COMPLETE = "return document.readyState === \"complete\"";

    // 通过document.readyState和localStorage判断telegram有没有登录成功
    public static final String RETURN_TELEGRAM_LOGIN_SUCCESS = RETURN_DOCUMENT_READY_STATE_COMPLETE + " && localStorage.getItem(\"user_auth\") !== null";

    // 第一次登录游戏会出现一个confirm按钮
    public static final String RETURN_BABY_DAGE_PAWS_WEB_APP_CONFIRM_BUTTON = "return Array.from(document.querySelectorAll(\"button\")).find(button => button.textContent.includes(\"Confirm\"))";

    // BabyDogePaws游戏的sessionStorage
    public static final String RETURN_TELEGRAM_APPS_SESSION_STORAGE_ITEMS = "return [sessionStorage.getItem(arguments[0]), sessionStorage.getItem(arguments[1])]";
    public static final String SET_TELEGRAM_APPS_SESSION_STORAGE_ITEM = "sessionStorage.setItem(arguments[0], arguments[1])";

    // 模拟手机登录成功后会出现的tab页标签
    public static final String RETURN_BABY_DAGE_PAWS_WEB_APP_TAB = "return document.querySelector('a[aria-current=\"page\"]')";

}
