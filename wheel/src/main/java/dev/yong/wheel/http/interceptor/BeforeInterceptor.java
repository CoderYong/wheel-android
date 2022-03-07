package dev.yong.wheel.http.interceptor;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author coderyong
 */
@SuppressWarnings("unused")
public interface BeforeInterceptor extends Interceptor {

    /**
     * 响应前
     *
     * @param response response
     * @param chain    chain
     * @return 返回处理后的响应内容，必须不为空
     */
    Response onResponseBefore(Interceptor.Chain chain, Response response);

    /**
     * 请求前
     *
     * @param request request
     * @return 返回处理后的请求，必须不为空
     */
    Request onRequestBefore(Request request);

    @NotNull
    @Override
    default Response intercept(@NotNull Chain chain) throws IOException {
        return onResponseBefore(chain, chain.proceed(onRequestBefore(chain.request())));
    }
}
