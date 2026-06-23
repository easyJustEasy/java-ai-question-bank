package com.questionbank;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import com.sun.net.httpserver.*;

/**
 * Java 试题生成器 - 轻量版
 * 基于 DeepSeek API 生成 Java 试题
 */
public class Main {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8001;
        WebServer.start(port);
    }
}
