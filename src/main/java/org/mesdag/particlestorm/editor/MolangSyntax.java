package org.mesdag.particlestorm.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// Molang 表达式的轻量语法高亮：把文本切成 token（数字 / 字符串 / 关键字 / 函数 / 变量 / 注释 / 运算符），
/// 逐段用不同颜色画出来。只负责绘制，不解析表达式（不校验语法）。
///
/// 单行文本由调用方给最大宽度，超出部分直接裁掉（tooltip 由调用方处理）。
final class MolangSyntax {
    private static final int DEFAULT = 0xFFD4D4D4;
    private static final int NUMBER = 0xFFB5CEA8;
    private static final int STRING = 0xFFCE9178;
    private static final int KEYWORD = 0xFFC586C0;
    private static final int FUNCTION = 0xFFDCDCAA;
    private static final int VARIABLE = 0xFF9CDCFE;
    private static final int COMMENT = 0xFF6A9955;
    private static final int OPERATOR = 0xFFD4D4D4;

    private static final Set<String> KEYWORDS = Set.of("true", "false", "null", "this", "return", "loop",
            "break", "continue", "for", "while", "if", "else");
    private static final List<String> VARIABLE_PREFIXES = List.of("query.", "q.", "variable.", "v.",
            "temp.", "t.", "math.", "context.", "c.");

    private MolangSyntax() {
    }

    record Token(String text, int color) {
    }

    static void draw(GuiGraphics guiGraphics, Font font, String text, int x, int y, int maxWidth) {
        int cursor = x;
        int maxX = x + Math.max(1, maxWidth);
        for (Token token : tokenize(text)) {
            if (cursor >= maxX) {
                break;
            }
            int width = font.width(token.text());
            if (cursor + width <= maxX) {
                guiGraphics.drawString(font, token.text(), cursor, y, token.color(), false);
                cursor += width;
            } else {
                guiGraphics.drawString(font, font.plainSubstrByWidth(token.text(), maxX - cursor), cursor, y, token.color(), false);
                break;
            }
        }
    }

    /// 从 `startIndex` 开始画（编辑框横向滚动后用），但**按整段文本分词**：
    /// 这样 `v.xxx` 被滚动截掉前缀、屏幕上只剩 `.xxx` 时，token 类型仍然正确，高亮不会丢。
    static void drawRange(GuiGraphics guiGraphics, Font font, String text, int startIndex, int x, int y, int maxWidth) {
        int cursor = x;
        int maxX = x + Math.max(1, maxWidth);
        int start = Math.max(0, startIndex);
        int index = 0;
        for (Token token : tokenize(text)) {
            int tokenStart = index;
            index += token.text().length();
            if (index <= start) {
                continue;
            }
            String visible = tokenStart >= start ? token.text() : token.text().substring(start - tokenStart);
            if (cursor >= maxX) {
                break;
            }
            int width = font.width(visible);
            if (cursor + width <= maxX) {
                guiGraphics.drawString(font, visible, cursor, y, token.color(), false);
                cursor += width;
            } else {
                guiGraphics.drawString(font, font.plainSubstrByWidth(visible, maxX - cursor), cursor, y, token.color(), false);
                break;
            }
        }
    }

    static List<Token> tokenize(String value) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = value.length();
        while (i < n) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c)) {
                int j = i;
                while (j < n && Character.isWhitespace(value.charAt(j))) {
                    j++;
                }
                tokens.add(new Token(value.substring(i, j), DEFAULT));
                i = j;
            } else if (c == '/' && i + 1 < n && value.charAt(i + 1) == '/') {
                tokens.add(new Token(value.substring(i), COMMENT));
                break;
            } else if (c == '\'' || c == '"') {
                int j = i + 1;
                while (j < n && value.charAt(j) != c) {
                    j++;
                }
                j = Math.min(n, j + 1);
                tokens.add(new Token(value.substring(i, j), STRING));
                i = j;
            } else if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(value.charAt(i + 1)))) {
                int j = i + 1;
                while (j < n) {
                    char d = value.charAt(j);
                    if (Character.isDigit(d) || d == '.') {
                        j++;
                    } else if ((d == 'e' || d == 'E') || ((d == '+' || d == '-') && (value.charAt(j - 1) == 'e' || value.charAt(j - 1) == 'E'))) {
                        j++;
                    } else {
                        break;
                    }
                }
                tokens.add(new Token(value.substring(i, j), NUMBER));
                i = j;
            } else if (Character.isLetter(c) || c == '_') {
                int j = i + 1;
                while (j < n) {
                    char d = value.charAt(j);
                    if (Character.isLetterOrDigit(d) || d == '_' || d == '.') {
                        j++;
                    } else {
                        break;
                    }
                }
                String word = value.substring(i, j);
                int color = classify(word, j < n && value.charAt(j) == '(');
                tokens.add(new Token(word, color));
                i = j;
            } else {
                tokens.add(new Token(String.valueOf(c), OPERATOR));
                i++;
            }
        }
        return tokens;
    }

    private static int classify(String word, boolean call) {
        if (call) {
            return FUNCTION;
        }
        if (KEYWORDS.contains(word)) {
            return KEYWORD;
        }
        for (String prefix : VARIABLE_PREFIXES) {
            if (word.startsWith(prefix)) {
                return VARIABLE;
            }
        }
        return DEFAULT;
    }
}
