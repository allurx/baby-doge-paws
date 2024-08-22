package red.zyc.babydogepaws.common.util;

import red.zyc.babydogepaws.common.constant.Constants;
import red.zyc.toolkit.core.reflect.TypeToken;

import java.net.http.HttpResponse;
import java.util.Optional;

import static red.zyc.toolkit.json.Json.JACKSON_OPERATOR;

/**
 * @author allurx
 */
public final class Https {

    private Https() {
    }

    /**
     * 将{@link HttpResponse#body()}作为json字符串解析为{@link TypeToken}类型的对象
     *
     * @param response  {@link HttpResponse}
     * @param typeToken {@link TypeToken}
     * @param <T>       对象的具体类型
     * @return 解析后的对象
     */
    public static <T> Optional<T> parseJsonResponse(HttpResponse<String> response, TypeToken<T> typeToken) {
        return Optional.ofNullable(response.body()).map((s) -> JACKSON_OPERATOR.fromJsonString(s, typeToken));
    }

    /**
     * 将{@link HttpResponse#body()}作为json字符串，格式化输出当前响应的详细信息
     * <br><br>
     * 参考{@link jdk.internal.net.http.HttpResponseImpl#toString()}
     *
     * @param response     {@link HttpResponse}
     * @param containsBody 是否需要包含body数据
     * @return {@link HttpResponse}格式化后的字符串
     */
    public static String formatJsonResponse(HttpResponse<String> response, boolean containsBody) {
        record JsonResponse(String method, String uri, int code, Object body) {
        }
        return JACKSON_OPERATOR.toJsonString(
                new JsonResponse(
                        response.request().method(),
                        response.request().uri().toString(),
                        response.statusCode(),
                        containsBody ? tryConvert(response.body()) : "ignored"));
    }

    /**
     * 尝试将body作为json字符串进行反序列化
     *
     * @param body 响应body
     * @return 反序列化结果
     */
    static Object tryConvert(String body) {
        try {
            return JACKSON_OPERATOR.fromJsonString(body, Constants.OBJECT_DATA_TYPE);
        } catch (Throwable t) {
            return body;
        }
    }
}
