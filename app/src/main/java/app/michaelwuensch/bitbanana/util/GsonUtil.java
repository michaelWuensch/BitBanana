package app.michaelwuensch.bitbanana.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class GsonUtil {
    public static Gson getTypeSafeGson() {
        return new GsonBuilder()
                .registerTypeAdapterFactory(new MapTypeAdapterFactory())
                .create();
    }
}

class MapTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!Map.class.isAssignableFrom(type.getRawType())) {
            return null;
        }
        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                JsonElement jsonElement = JsonParser.parseReader(in);
                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    LinkedTreeMap<String, Object> map = new LinkedTreeMap<>();
                    Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
                    for (Map.Entry<String, JsonElement> entry : entries) {
                        map.put(entry.getKey(), parseElement(entry.getValue()));
                    }
                    return (T) map;
                }
                return delegate.fromJsonTree(jsonElement);
            }

            private Object parseElement(JsonElement element) {
                if (element.isJsonNull()) {
                    return null;
                } else if (element.isJsonPrimitive()) {
                    JsonPrimitive primitive = element.getAsJsonPrimitive();
                    if (primitive.isBoolean()) {
                        return primitive.getAsBoolean();
                    } else if (primitive.isString()) {
                        return primitive.getAsString();
                    } else if (primitive.isNumber()) {
                        String numStr = primitive.getAsString();
                        if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                            return primitive.getAsDouble();
                        } else {
                            try {
                                return Integer.parseInt(numStr);
                            } catch (NumberFormatException e) {
                                try {
                                    return Long.parseLong(numStr);
                                } catch (NumberFormatException ex) {
                                    return primitive.getAsDouble(); // fallback
                                }
                            }
                        }
                    }
                } else if (element.isJsonArray()) {
                    return element.getAsJsonArray();
                } else if (element.isJsonObject()) {
                    return element.getAsJsonObject();
                }
                return null;
            }
        };
    }
}