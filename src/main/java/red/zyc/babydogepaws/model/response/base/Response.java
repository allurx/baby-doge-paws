package red.zyc.babydogepaws.model.response.base;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author allurx
 */
@Schema(description = "响应")
public class Response<T> {

    @Schema(description = "响应数据")
    public T data;

    @Schema(description = "响应码")
    public final String code;

    @Schema(description = "响应信息")
    public final String message;

    private Response() {
        this.code = ResponseMessage.SUCCESS.code;
        this.message = ResponseMessage.SUCCESS.message;
    }

    private Response(T data) {
        this();
        this.data = data;
    }


    private Response(T data, String message, String code) {
        this.data = data;
        this.message = message;
        this.code = code;
    }

    /**
     * @param <R> 响应数据的类型
     * @return 响应结果
     */
    public static <R> Response<R> ok() {
        return new Response<>();
    }

    /**
     * @param data 响应数据
     * @param <R>  响应数据的类型
     * @return 响应结果
     */
    public static <R> Response<R> ok(R data) {
        return new Response<>(data);
    }

    /**
     * @param responseMessage {@link ResponseMessage}
     * @param <R>             响应数据的类型
     * @return 响应结果
     */
    public static <R> Response<R> ok(ResponseMessage responseMessage) {
        return ok(null, responseMessage);
    }

    /**
     * @param data            响应数据
     * @param responseMessage {@link ResponseMessage}
     * @param <R>             响应数据的类型
     * @return 响应结果
     */
    public static <R> Response<R> ok(R data, ResponseMessage responseMessage) {
        return new Response<>(data, responseMessage.message, responseMessage.code);
    }
}
