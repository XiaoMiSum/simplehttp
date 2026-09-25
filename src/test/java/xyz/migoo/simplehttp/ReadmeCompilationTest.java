/*
 *
 *  * The MIT License (MIT)
 *  *
 *  * Copyright (c) 2025.  Lorem XiaoMiSum (mi_xiao@qq.com)
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining
 *  * a copy of this software and associated documentation files (the
 *  * 'Software'), to deal in the Software without restriction, including
 *  * without limitation the rights to use, copy, modify, merge, publish,
 *  * distribute, sublicense, and/or sell copies of the Software, and to
 *  * permit persons to whom the Software is furnished to do so, subject to
 *  * the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be
 *  * included in all copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
 *  * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package xyz.migoo.simplehttp;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 保证 readme 里的示例与实现始终一致：逐个抽取 readme 中的 java 代码块，用 javac 真编译一遍。
 * 任何被删除、改名或降级的 API，只要还出现在文档里，这个测试就会失败。
 *
 * @author xiaomi
 *         Created at 2026/09/25
 */
public class ReadmeCompilationTest {

    /**
     * 匹配 readme 中的 java 代码块，围栏允许带尾随空格（如 ```java ）
     */
    private static final Pattern JAVA_BLOCK = Pattern.compile("```[ \\t]*java[ \\t]*\\n(.*?)```", Pattern.DOTALL);

    /**
     * 编译 readme 中的每一个 java 代码块
     */
    @Test
    public void testEveryJavaSnippetCompiles() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new SkipException("需要 JDK（而非 JRE）才能编译 readme 示例");
        }

        Path readme = Path.of("readme.md");
        Assert.assertTrue(Files.isRegularFile(readme),
                "未找到 " + readme.toAbsolutePath() + "，测试需在项目根目录运行");

        String markdown = Files.readString(readme, StandardCharsets.UTF_8);
        String classpath = classpath();

        Matcher matcher = JAVA_BLOCK.matcher(markdown);
        Path workDir = Files.createTempDirectory("simplehttp-readme");
        int index = 0;
        StringBuilder failures = new StringBuilder();

        try {
            while (matcher.find()) {
                index++;
                String source = matcher.group(1);
                int line = markdown.substring(0, matcher.start()).split("\n").length;

                if (!source.contains("class ")) {
                    failures.append("readme.md:").append(line)
                            .append(" 的代码块不是完整可编译的类（示例必须自带 import 与 class）\n");
                    continue;
                }

                Path blockDir = Files.createDirectory(workDir.resolve("block" + index));
                Path sourceFile = blockDir.resolve("Demo.java");
                Files.writeString(sourceFile, source, StandardCharsets.UTF_8);

                ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
                int result = compiler.run(null, null, new PrintStream(diagnostics, true, StandardCharsets.UTF_8),
                        "-encoding", "UTF-8", "-nowarn", "-classpath", classpath,
                        "-d", blockDir.toString(), sourceFile.toString());
                if (result != 0) {
                    failures.append("readme.md:").append(line).append(" 的代码块编译失败：\n")
                            .append(diagnostics.toString(StandardCharsets.UTF_8).strip()).append('\n');
                }
            }
        } finally {
            deleteRecursively(workDir);
        }

        Assert.assertTrue(index > 0, "readme.md 中未发现任何 java 代码块");
        Assert.assertTrue(failures.isEmpty(), "\n" + failures);
    }

    /**
     * surefire 会把完整测试类路径写进 surefire.test.class.path；
     * java.class.path 在 useManifestOnlyJar 模式下只是一个引导 jar，不能直接用。
     */
    private String classpath() {
        for (String name : new String[]{"surefire.test.class.path", "java.class.path"}) {
            String value = System.getProperty(name);
            if (value != null && !value.isBlank() && (value.contains("httpclient5") || value.contains("simplehttp"))) {
                return value;
            }
        }
        throw new SkipException("无法解析测试类路径，无法编译 readme 示例");
    }

    private void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            // 清理临时目录失败不影响断言结果
        }
    }
}
