package com.example.demo.common; // 定义包名为 com.example.demo.common

public class Result { // 定义统一响应结果类
    private int code; // 状态码字段（200表示成功，500表示失败）
    private String msg; // 消息字段（提示信息）
    private Object data; // 数据字段（返回的业务数据）

    public static Result success(Object data) { // 静态方法：成功响应，参数为业务数据
        Result r = new Result(); // 创建 Result 实例
        r.code = 200; // 设置状态码为200（成功）
        r.msg = "成功"; // 设置消息为"成功"
        r.data = data; // 设置业务数据
        return r; // 返回构建的 Result 对象
    }

    public static Result fail(String msg) { // 静态方法：失败响应，参数为错误消息
        Result r = new Result(); // 创建 Result 实例
        r.code = 500; // 设置状态码为500（失败）
        r.msg = msg; // 设置错误消息
        return r; // 返回构建的 Result 对象
    }

    public int getCode() { return code; } // 获取状态码的方法
    public void setCode(int code) { this.code = code; } // 设置状态码的方法
    public String getMsg() { return msg; } // 获取消息的方法
    public void setMsg(String msg) { this.msg = msg; } // 设置消息的方法
    public Object getData() { return data; } // 获取数据的方法
    public void setData(Object data) { this.data = data; } // 设置数据的方法
}