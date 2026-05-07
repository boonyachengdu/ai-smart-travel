package com.boonya.business.trip.search.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 降级策略配置
 */
@Slf4j
@Configuration
public class SentinelConfig {

    @PostConstruct
    public void initRules() {
        initFlowRules();
        initDegradeRules();
        log.info("Sentinel 规则初始化完成");
    }

    /**
     * 初始化限流规则
     */
    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 航班查询限流：每秒最多处理 100 个请求
        FlowRule flightFlowRule = new FlowRule();
        flightFlowRule.setResource("searchFlights");
        flightFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        flightFlowRule.setCount(100);
        //flightFlowRule.setTimeWindow(1);
        rules.add(flightFlowRule);

        // 酒店查询限流：每秒最多处理 100 个请求
        FlowRule hotelFlowRule = new FlowRule();
        hotelFlowRule.setResource("searchHotels");
        hotelFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        hotelFlowRule.setCount(100);
        //hotelFlowRule.setTimeWindow(1);
        rules.add(hotelFlowRule);

        // 火车查询限流：每秒最多处理 100 个请求
        FlowRule trainFlowRule = new FlowRule();
        trainFlowRule.setResource("searchTrains");
        trainFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        trainFlowRule.setCount(100);
        //trainFlowRule.setTimeWindow(1);
        rules.add(trainFlowRule);

        FlowRuleManager.loadRules(rules);
        log.info("Sentinel 限流规则加载完成");
    }

    /**
     * 初始化降级规则
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // 航班查询降级：平均响应时间超过 3 秒则降级
        DegradeRule flightDegradeRule = new DegradeRule();
        flightDegradeRule.setResource("searchFlights");
        flightDegradeRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        flightDegradeRule.setCount(3000); // RT 单位是毫秒
        flightDegradeRule.setTimeWindow(60); // 降级持续时间 60 秒
        flightDegradeRule.setMinRequestAmount(5); // 最小请求数
        rules.add(flightDegradeRule);

        // 酒店查询降级：平均响应时间超过 3 秒则降级
        DegradeRule hotelDegradeRule = new DegradeRule();
        hotelDegradeRule.setResource("searchHotels");
        hotelDegradeRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        hotelDegradeRule.setCount(3000);
        hotelDegradeRule.setTimeWindow(60);
        hotelDegradeRule.setMinRequestAmount(5);
        rules.add(hotelDegradeRule);

        // 火车查询降级：平均响应时间超过 3 秒则降级
        DegradeRule trainDegradeRule = new DegradeRule();
        trainDegradeRule.setResource("searchTrains");
        trainDegradeRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        trainDegradeRule.setCount(3000);
        trainDegradeRule.setTimeWindow(60);
        trainDegradeRule.setMinRequestAmount(5);
        rules.add(trainDegradeRule);

        DegradeRuleManager.loadRules(rules);
        log.info("Sentinel 降级规则加载完成");
    }
}
