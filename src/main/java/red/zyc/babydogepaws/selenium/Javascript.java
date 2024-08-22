package red.zyc.babydogepaws.selenium;

/**
 * @author allurx
 */
public final class Javascript {

    private Javascript() {
    }

    public static String WINDOW_LOADED = "return document.readyState === \"complete\"";

    public static final String LINK_IS_CLICKABLE = """
            return (function(){
                function isClickable(link) {
                    if(!link){
                        return false;
                    }
                    if(link.disabled){
                        return false;
                    }
                    if(link.href && !link.href.startsWith('javascript:') && link.href !== '#'){
                         return true;
                    }
                    if(link.onclick && typeof link.onclick === 'function'){
                         return true;
                    }
                }
                var element = document.querySelector(arguments[0]);
                if(isClickable(element)){
                    return element;
                }
            })(arguments[0])
            """;
    public static final String BUTTON_IS_CLICKABLE = """
            return (function(){
                function isClickable(button) {
                    if(!button){
                        return false;
                    }
                    if(button.disabled){
                        return false;
                    }
                    if(button.onclick && typeof button.onclick === 'function'){
                        return true;
                    }
                    return true;
                 }
                var element = document.querySelector(arguments[0]);
                if(isClickable(element)){
                     return element;
                }
             })(arguments[0])
            """;
}
