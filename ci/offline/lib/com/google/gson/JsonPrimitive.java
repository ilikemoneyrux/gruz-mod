package com.google.gson;
public final class JsonPrimitive extends JsonElement { public JsonPrimitive(String s) {} public JsonPrimitive(Number n) {} public JsonPrimitive(Boolean b) {} public boolean isString() { return false; } public boolean isNumber() { return false; } public boolean isBoolean() { return false; } }
