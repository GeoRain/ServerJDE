package com.example.serverjde.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * 文本工具
 * 处理旧版颜色代码字符串与 Adventure Component 的转换
 */
public class TextUtil {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

    /**
     * 将包含 § 颜色代码的字符串转换为 Component
     */
    public static Component toComponent(String legacy) {
        if (legacy == null || legacy.isEmpty()) return Component.empty();
        return SERIALIZER.deserialize(legacy);
    }
}
