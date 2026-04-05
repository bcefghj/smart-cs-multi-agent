package com.smartcs.agent;

/**
 * Agent基类 —— 定义所有子Agent的统一接口。
 * Supervisor通过此接口调度各Agent。
 */
public interface BaseAgent {

    /**
     * 处理状态，返回更新后的状态
     */
    AgentState process(AgentState state);

    /**
     * Agent名称
     */
    String getName();
}
