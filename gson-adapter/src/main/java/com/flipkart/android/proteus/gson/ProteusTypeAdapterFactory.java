package com.flipkart.android.proteus.gson;

import android.support.annotation.Nullable;

import com.flipkart.android.proteus.Array;
import com.flipkart.android.proteus.Attribute;
import com.flipkart.android.proteus.Layout;
import com.flipkart.android.proteus.Null;
import com.flipkart.android.proteus.Object;
import com.flipkart.android.proteus.Primitive;
import com.flipkart.android.proteus.Value;
import com.flipkart.android.proteus.builder.ProteusLayoutInflater;
import com.flipkart.android.proteus.toolbox.ProteusConstants;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.JsonReaderInternalAccess;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * ProteusTypeAdapterFactory
 *
 * @author aditya.sharat
 */
public class ProteusTypeAdapterFactory implements TypeAdapterFactory {

    public static final ProteusInstanceHolder PROTEUS_INSTANCE_HOLDER = new ProteusInstanceHolder();

    public static final TypeAdapter<Value> VALUE_TYPE_ADAPTER = new TypeAdapter<Value>() {
        @Override
        public void write(JsonWriter out, Value value) throws IOException {
            if (value == null || value.isNull()) {
                out.nullValue();
            } else if (value.isPrimitive()) {
                Primitive primitive = value.getAsPrimitive();
                if (primitive.isNumber()) {
                    out.value(primitive.getAsNumber());
                } else if (primitive.isBoolean()) {
                    out.value(primitive.getAsBoolean());
                } else {
                    out.value(primitive.getAsString());
                }

            } else if (value.isArray()) {
                out.beginArray();
                Iterator<Value> iterator = value.getAsArray().iterator();
                while (iterator.hasNext()) {
                    write(out, iterator.next());
                }
                out.endArray();

            } else if (value.isObject()) {
                out.beginObject();
                for (Map.Entry<String, Value> e : value.getAsObject().entrySet()) {
                    out.name(e.getKey());
                    write(out, e.getValue());
                }
                out.endObject();

            } else {
                throw new IllegalArgumentException("Couldn't write " + value.getClass());
            }
        }

        @Override
        public Value read(JsonReader in) throws IOException {
            switch (in.peek()) {
                case STRING:
                    return new Primitive(in.nextString());
                case NUMBER:
                    String number = in.nextString();
                    return new Primitive(new LazilyParsedNumber(number));
                case BOOLEAN:
                    return new Primitive(in.nextBoolean());
                case NULL:
                    in.nextNull();
                    return Null.INSTANCE;
                case BEGIN_ARRAY:
                    Array array = new Array();
                    in.beginArray();
                    while (in.hasNext()) {
                        array.add(read(in));
                    }
                    in.endArray();
                    return array;
                case BEGIN_OBJECT:
                    Object object = new Object();
                    in.beginObject();
                    if (in.hasNext()) {
                        String name = in.nextName();
                        if (ProteusConstants.TYPE.equals(name) && JsonToken.STRING.equals(in.peek())) {
                            String type = in.nextString();
                            if (PROTEUS_INSTANCE_HOLDER.isLayout(type)) {
                                return LAYOUT_TYPE_ADAPTER.read(type, PROTEUS_INSTANCE_HOLDER.getInflater(), in);
                            } else {
                                object.add(name, new Primitive(type));
                            }
                        } else {
                            object.add(name, read(in));
                        }
                    }
                    while (in.hasNext()) {
                        object.add(in.nextName(), read(in));
                    }
                    in.endObject();
                    return object;
                case END_DOCUMENT:
                case NAME:
                case END_OBJECT:
                case END_ARRAY:
                default:
                    throw new IllegalArgumentException();
            }
        }
    }.nullSafe();
    public static final TypeAdapter<Primitive> PRIMITIVE_TYPE_ADAPTER = new TypeAdapter<Primitive>() {

        @Override
        public void write(JsonWriter out, Primitive value) throws IOException {
            VALUE_TYPE_ADAPTER.write(out, value);
        }

        @Override
        public Primitive read(JsonReader in) throws IOException {
            Value value = VALUE_TYPE_ADAPTER.read(in);
            return value != null && value.isPrimitive() ? value.getAsPrimitive() : null;
        }
    }.nullSafe();
    public static final TypeAdapter<Object> OBJECT_TYPE_ADAPTER = new TypeAdapter<Object>() {
        @Override
        public void write(JsonWriter out, Object value) throws IOException {
            VALUE_TYPE_ADAPTER.write(out, value);
        }

        @Override
        public Object read(JsonReader in) throws IOException {
            Value value = VALUE_TYPE_ADAPTER.read(in);
            return value != null && value.isObject() ? value.getAsObject() : null;
        }
    }.nullSafe();
    public static final TypeAdapter<Array> ARRAY_TYPE_ADAPTER = new TypeAdapter<Array>() {
        @Override
        public void write(JsonWriter out, Array value) throws IOException {
            VALUE_TYPE_ADAPTER.write(out, value);
        }

        @Override
        public Array read(JsonReader in) throws IOException {
            Value value = VALUE_TYPE_ADAPTER.read(in);
            return value != null && value.isArray() ? value.getAsArray() : null;
        }
    }.nullSafe();
    public static final TypeAdapter<Null> NULL_TYPE_ADAPTER = new TypeAdapter<Null>() {

        @Override
        public void write(JsonWriter out, Null value) throws IOException {
            VALUE_TYPE_ADAPTER.write(out, value);
        }

        @Override
        public Null read(JsonReader in) throws IOException {
            Value value = VALUE_TYPE_ADAPTER.read(in);
            return value != null && value.isNull() ? value.getAsNull() : null;
        }
    }.nullSafe();
    public static final LayoutTypeAdapter LAYOUT_TYPE_ADAPTER = new LayoutTypeAdapter();

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class clazz = type.getRawType();

        if (clazz == Primitive.class) {
            //noinspection unchecked
            return (TypeAdapter<T>) PRIMITIVE_TYPE_ADAPTER;
        } else if (clazz == Object.class) {
            //noinspection unchecked
            return (TypeAdapter<T>) OBJECT_TYPE_ADAPTER;
        } else if (clazz == Array.class) {
            //noinspection unchecked
            return (TypeAdapter<T>) ARRAY_TYPE_ADAPTER;
        } else if (clazz == Null.class) {
            //noinspection unchecked
            return (TypeAdapter<T>) NULL_TYPE_ADAPTER;
        } else if (clazz == Layout.class) {
            //noinspection unchecked
            return (TypeAdapter<T>) LAYOUT_TYPE_ADAPTER;
        } else if (clazz == Value.class) {
            //noinspection unchecked
            return (TypeAdapter<T>) VALUE_TYPE_ADAPTER;
        }

        return null;
    }


    public static class ProteusInstanceHolder {

        private ProteusLayoutInflater inflater;

        private ProteusInstanceHolder() {
        }

        @Nullable
        public ProteusLayoutInflater getInflater() {
            return inflater;
        }

        public void setInflater(ProteusLayoutInflater inflater) {
            this.inflater = inflater;
        }

        public boolean isLayout(String type) {
            return null != inflater && null != inflater.getParser(type);
        }
    }

    public static class LayoutTypeAdapter extends TypeAdapter<Layout> {

        @Override
        public void write(JsonWriter out, Layout value) throws IOException {
            VALUE_TYPE_ADAPTER.write(out, value);
        }

        @Override
        public Layout read(JsonReader in) throws IOException {
            Value value = VALUE_TYPE_ADAPTER.read(in);
            return value != null && value.isLayout() ? value.getAsLayout() : null;
        }

        public Layout read(String type, ProteusLayoutInflater inflater, JsonReader in) throws IOException {
            List<Attribute> attributes = new ArrayList<>();
            Map<String, String> scope = null;
            String name;
            while (in.hasNext()) {
                name = in.nextName();
                if (ProteusConstants.SCOPE.equals(name)) {
                    scope = readScope(in);
                } else {
                    int id = inflater.getAttributeId(name, type);
                    if (-1 != id) {
                        attributes.add(new Attribute(id, VALUE_TYPE_ADAPTER.read(in)));
                    } else {
                        in.skipValue();
                    }
                }
            }

            in.endObject();

            return new Layout(type, attributes.size() > 0 ? attributes : null, scope);
        }

        @Nullable
        public Map<String, String> readScope(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            if (peek == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            if (peek != JsonToken.BEGIN_OBJECT) {
                throw new JsonSyntaxException("scope must be a Map<String, String>.");
            }

            Map<String, String> scope = new HashMap<>();

            in.beginObject();
            while (in.hasNext()) {
                JsonReaderInternalAccess.INSTANCE.promoteNameToValue(in);
                String key = in.nextString();
                String value = in.nextString();
                String replaced = scope.put(key, value);
                if (replaced != null) {
                    throw new JsonSyntaxException("duplicate key: " + key);
                }
            }
            in.endObject();

            return scope;
        }
    }
}
