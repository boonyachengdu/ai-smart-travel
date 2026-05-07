package com.boonya.business.trip.dialog.service.impl;

import com.alibaba.dashscope.common.Message;
import com.alibaba.fastjson.JSONObject;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.models.channel.*;
import com.boonya.business.trip.common.models.dialog.OrderGenerationResponse;
import com.boonya.business.trip.common.models.rag.RagPolicyResult;
import com.boonya.business.trip.dialog.constant.DialogStage;
import com.boonya.business.trip.dialog.context.DialogContext;
import com.boonya.business.trip.dialog.service.SolutionService;
import com.boonya.business.trip.dialog.vo.ChatMessageVO;
import com.boonya.business.trip.feign.clients.ChannelFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolutionServiceImpl implements SolutionService {

    @Value("${channel.data.mock:true}")
    private Boolean mock;

    private final ChannelFeignClient channelFeignClient;

    private final ChatClient chatClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public OrderGenerationResponse handleSolutionRecommendationStage(DialogContext context, String userInput,
                                                                     List<ChatMessageVO> history, RagPolicyResult ragResult) {
        log.info("【方案推荐】用户选择：{}", userInput);

        try {
            int selectedIndex = Integer.parseInt(userInput.trim()) - 1;
            if (selectedIndex >= 0 && selectedIndex < context.getRecommendedSolutions().size()) {
                context.setSelectedSolutionIndex(selectedIndex);
                Map<String, Object> selectedSolution = JSONObject.parseObject(JSONObject.toJSONString(context.getRecommendedSolutions().get(selectedIndex)), Map.class);
                applySolutionToContext(context, selectedSolution);

                OrderGenerationResponse response = new OrderGenerationResponse();
                response.setSessionId(context.getSessionId());
                response.setStatus("clarify");
                response.setMessage("好的，已为您选择该方案。接下来请选择等级：\n- 经济型\n- 商务型\n- 豪华型 \n\n");

                context.advanceStage(DialogStage.FEATURE_SELECTION);
                return response;
            }
        } catch (NumberFormatException e) {
            log.error("【方案推荐】用户输入转换错误", e);
        }

        OrderGenerationResponse response = new OrderGenerationResponse();
        response.setSessionId(context.getSessionId());
        response.setStatus("error");
        response.setMessage("请输入有效的选项编号（1-" + context.getRecommendedSolutions().size() + "）");
        return response;
    }

    @Override
    public List<?> generateSolutions(DialogContext context, RagPolicyResult ragResult) {
        if (mock){
            return generateMockSolutions(context, ragResult);
        }
        String searchQuery = buildSearchQueryFromContext(context);

        String result = chatClient.prompt()
                .functions("searchFlights", "searchHotels", "searchTrains", "searchCars")
                .user(searchQuery)
                .advisors(advisor -> advisor.param("chatMemoryContextId", "solution_" + context.getSessionId()))
                .call()
                .content();

        return parseSearchResults(result);
    }

    @NotNull
    private List<?> generateMockSolutions(DialogContext context, RagPolicyResult ragResult) {
        log.info("【生成mock解决方案】开始生成解决方案");
        log.info("【生成mock解决方案】上下文参数：{}", JSONObject.toJSONString(context));
        log.info("【生成mock解决方案】政策信息：{}", JSONObject.toJSONString(ragResult));
        List<?> solutions = new ArrayList<>();
        switch (context.getScene()) {
            case FLIGHT -> {
                FlightSearchRequest req = new FlightSearchRequest();
                req.setDeparture(context.getCollectedParams().getString("departureCity"));
                req.setArrival(context.getCollectedParams().getString("arrivalCity"));
                String departureDateStr = context.getCollectedParams().getString("departureDate");
                req.setDepartureDate(parseDateToLocalDateTime(departureDateStr));
                Response<List<FlightSearchResponse>> res = channelFeignClient.searchFlights(req);
                List<FlightSearchResponse> list = res.getData().stream().sorted(Comparator.comparing(FlightSearchResponse::getPrice)).collect(Collectors.toList());
                for (int i = 0; i < list.size(); i++) {
                    list.get(i).setIndex(i+1);
                }
                return list;
            }
            case HOTEL -> {
                HotelSearchRequest req = new HotelSearchRequest();
                req.setCity(context.getCollectedParams().getString("checkInCity"));
                String checkInDateStr = context.getCollectedParams().getString("checkInDate");
                String checkOutDateStr = context.getCollectedParams().getString("checkOutDate");
                req.setCheckInDate(parseDateToLocalDateTime(checkInDateStr));
                req.setCheckOutDate(parseDateToLocalDateTime(checkOutDateStr));
                Response<List<HotelSearchResponse>> res = channelFeignClient.searchHotels(req);
                List<HotelSearchResponse> list = res.getData().stream().sorted(Comparator.comparing(HotelSearchResponse::getPrice)).collect(Collectors.toList());
                for (int i = 0; i < list.size(); i++) {
                    list.get(i).setIndex(i+1);
                }
                return list;
            }
            case TRAIN -> {
                TrainSearchRequest req = new TrainSearchRequest();
                req.setDeparture(context.getCollectedParams().getString("departureCity"));
                req.setArrival(context.getCollectedParams().getString("arrivalCity"));
                String departureDateStr = context.getCollectedParams().getString("departureDate");
                req.setDepartureDate(parseDateToLocalDateTime(departureDateStr));
                Response<List<TrainSearchResponse>> res = channelFeignClient.searchTrains(req);
                List<TrainSearchResponse> list = res.getData().stream().sorted(Comparator.comparing(TrainSearchResponse::getPrice)).collect(Collectors.toList());
                for (int i = 0; i < list.size(); i++) {
                    list.get(i).setIndex(i+1);
                }
                return list;
            }
            case CAR -> {
                CarSearchRequest req = new CarSearchRequest();
                req.setCity(context.getCollectedParams().getString("city"));
                String useTimeStr = context.getCollectedParams().getString("useTime");
                req.setUseTime(parseDateToLocalDateTime(useTimeStr));
                req.setDestination(context.getCollectedParams().getString("destination"));
                Response<List<CarSearchResponse>> res = channelFeignClient.searchCars(req);
                List<CarSearchResponse> list = res.getData().stream().sorted(Comparator.comparing(CarSearchResponse::getPrice)).collect(Collectors.toList());
                for (int i = 0; i < list.size(); i++) {
                    list.get(i).setIndex(i+1);
                }
                return list;
            }
            default -> {
                return solutions;
            }
        }
    }

    private String buildSearchQueryFromContext(DialogContext context) {
        StringBuilder query = new StringBuilder();

        switch (context.getScene()) {
            case FLIGHT:
                query.append("搜索航班：从 ")
                        .append(context.getCollectedParam("departureCity"))
                        .append(" 到 ")
                        .append(context.getCollectedParam("arrivalCity"))
                        .append("，出发日期 ")
                        .append(context.getCollectedParam("departureDate"));
                break;
            case HOTEL:
                query.append("搜索酒店：城市 ")
                        .append(context.getCollectedParam("checkInCity"))
                        .append("，入住日期 ")
                        .append(context.getCollectedParam("checkInDate"))
                        .append("，离店日期 ")
                        .append(context.getCollectedParam("checkOutDate"));
                break;
            case TRAIN:
                query.append("搜索火车：从 ")
                        .append(context.getCollectedParam("departureCity"))
                        .append(" 到 ")
                        .append(context.getCollectedParam("arrivalCity"))
                        .append("，出发日期 ")
                        .append(context.getCollectedParam("departureDate"));
                break;
            case CAR:
                query.append("搜索用车：城市 ")
                        .append(context.getCollectedParam("city"))
                        .append("，用车时间 ")
                        .append(context.getCollectedParam("useTime"))
                        .append("，目的地 ")
                        .append(context.getCollectedParam("destination"));
                break;
            default:
                query.append("搜索差旅服务");
        }

        if (context.getCollectedParam("budget") != null) {
            query.append("，预算 ").append(context.getCollectedParam("budget")).append(" 元");
        }

        return query.toString();
    }

    @SuppressWarnings("unchecked")
    private List<?> parseSearchResults(String result) {
        try {
            if (result == null || result.trim().isEmpty()) {
                log.warn("Function calling 返回结果为空");
                return Collections.emptyList();
            }

            JSONObject jsonResponse = JSONObject.parseObject(result);

            if (!jsonResponse.containsKey("solutions")) {
                log.warn("Function calling 返回结果中未找到 solutions 字段");
                return Collections.emptyList();
            }

            return jsonResponse.getJSONArray("solutions").toJavaList(Object.class);

        } catch (Exception e) {
            log.error("解析 Function Calling 结果失败：{}", result, e);
            return Collections.emptyList();
        }
    }

    private LocalDateTime parseDateToLocalDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            LocalDate localDate = LocalDate.parse(dateStr, DATE_FORMATTER);
            return localDate.atStartOfDay();
        } catch (Exception e) {
            log.warn("日期解析失败：{}, 使用当前时间", dateStr, e);
            return LocalDateTime.now();
        }
    }

    private void applySolutionToContext(DialogContext context, Map<String, Object> solution) {
        context.addCollectedParam("selectedSolution", solution);
    }
}
