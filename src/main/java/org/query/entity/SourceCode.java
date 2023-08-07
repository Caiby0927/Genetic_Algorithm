package org.query.entity;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author CAI
 * @date 2023/4/14
 **/
public class SourceCode {
    //    源代码文件序号
    public int code_id;

    private String className;
//    源代码文件的词
    public List<String> tokens = new ArrayList<>();

    /**
     * 构造函数
     * @param code_id
     * @param filepath
     */
    public SourceCode(int code_id, String filepath) {
        this.code_id = code_id;
        read_source_code_from_file(filepath);
    }

    public SourceCode(int code_id, String className, String codeContent) {
        this.code_id = code_id;
        this.className = className;

        if (codeContent != null) {
            String[] code_tokens = codeContent.split(" ");
            tokens.addAll(Arrays.asList(code_tokens));
        } else {
            tokens = null;
        }
    }

    /**
     * 从文件中读取源代码文件的词汇
     * @param filepath
     */
    private void read_source_code_from_file(String filepath) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(filepath));

//            源代码文件中，每个词占据一行
            String str;
            while ((str = in.readLine()) != null) {
                tokens.add(str.trim());
            }
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getCode_id() {
        return code_id;
    }

    public String getTokensString() {
        StringBuilder stringBuilder = new StringBuilder();

        for (String token: tokens) {
            stringBuilder.append(token).append(" ");
        }

        return stringBuilder.toString().trim();
    }
}
