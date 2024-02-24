package dev.felnull.shortlifeplugin.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Json関係のユーティリティクラス
 *
 * @author MORIMORI0317
 */
public class JsonUtils {

    private JsonUtils() {
        throw new AssertionError();
    }

    /**
     * Jsonから文字列を取得する
     *
     * @param json         取得対象のJson
     * @param key          文字列のキー
     * @param defaultValue キーの文字列が存在しない場合の値
     * @return 取得した文字列
     */
    @Nullable
    public static String getString(@NotNull JsonObject json, @NotNull String key, @Nullable String defaultValue) {

        // Jsonにキーの値が存在するかどうか、その値がJsonプリミティブかどうか
        if (json.has(key) && json.get(key).isJsonPrimitive()) {
            JsonPrimitive valuePrimitive = json.getAsJsonPrimitive(key);

            // Jsonプリミティブが文字列かどうか
            if (valuePrimitive.isString()) {
                return valuePrimitive.getAsString();
            }
        }

        return defaultValue;
    }
}
