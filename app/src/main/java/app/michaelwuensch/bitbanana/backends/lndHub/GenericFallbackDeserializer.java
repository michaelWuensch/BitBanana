package app.michaelwuensch.bitbanana.backends.lndHub;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import app.michaelwuensch.bitbanana.util.HexUtil;

/**
 * This is only necessary because the LNDHub specification is a mess.
 * The JSONS returned are not identical across implementations.
 * <p>
 * For example for r_hash one implementation returns a hex string, while another returns:
 * "r_hash":{"type":"Buffer","data":[151,82,131,6,197,26,11,187,189,93,241,204,103,92,153,97,10,109,78,34,172,151,235,142,120,24,153,102,71,9,136,109]
 */
public class GenericFallbackDeserializer<T> implements JsonDeserializer<T> {
    private final Class<T> clazz;

    public GenericFallbackDeserializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        T instance;

        try {
            instance = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new JsonParseException("Could not create instance of " + clazz.getName(), e);
        }

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            SerializedName serializedName = field.getAnnotation(SerializedName.class);
            String fieldName = serializedName != null ? serializedName.value() : field.getName();
            JsonElement element = jsonObject.get(fieldName);
            if (element != null && !element.isJsonNull()) {
                try {
                    Object value = parseElementAsStringOrDefault(element, field.getType(), context);
                    field.set(instance, value);
                } catch (IllegalAccessException e) {
                    throw new JsonParseException("Could not set field " + field.getName(), e);
                }
            }
        }

        return instance;
    }

    private Object parseElementAsStringOrDefault(JsonElement element, Class<?> fieldType, JsonDeserializationContext context) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return element.getAsString();
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("type") && "Buffer".equals(obj.get("type").getAsString()) && obj.has("data")) {
                JsonArray dataArray = obj.getAsJsonArray("data");
                byte[] bytes = new byte[dataArray.size()];
                for (int i = 0; i < dataArray.size(); i++) {
                    bytes[i] = dataArray.get(i).getAsByte();
                }
                return HexUtil.bytesToHex(bytes);
            }
        }
        // Use default deserialization for non-string fields
        return context.deserialize(element, fieldType);
    }
}