package com.boonya.business.trip.dialog;

import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.models.dialog.OrderGenerationResponse;
import com.boonya.business.trip.common.models.dialog.OrderRequirements;
import com.boonya.business.trip.dialog.service.DialogSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@Slf4j
@RunWith(SpringRunner .class)
@RequiredArgsConstructor
public class ChatTest {

    private final DialogSessionService dialogSessionService;

    // 用例1.对话式预订机票
    @Test
    public void testDialogFlightBooking() {
        // 1. 用户发起请求
        String userInput = "帮我订一张明天北京到上海的机票";

        // 2. 调用对话服务
        OrderGenerationResponse response = dialogSessionService.chat(Scene.FLIGHT,true, userInput);
        OrderRequirements  order = response.getOrder();

        // 3. 验证意图识别
        assertEquals("FLIGHT", order.getOrderType());

        // 4. 验证槽位提取
        assertEquals("北京", order.getDepartureCity());
        assertEquals("上海", order.getArrivalCity());
        assertEquals("tomorrow", order.getDepartureDate());

        // 5. 验证是否调用查询接口（Mock 验证）
        //verify(flightSearchService).channel(any(SearchRequest.class));
    }


//    // 用例2.政策咨询（RAG 检索）
//    @Test
//    public void testPolicyConsultation() {
//        String question = "一线城市住宿标准是多少？";
//
//        ChatResponse response = ragService.query(question, companyId);
//
//        // 验证检索到相关政策
//        assertTrue(response.getSources().size() > 0);
//        assertEquals("standards", response.getSources().get(0).getType());
//        assertTrue(response.getContent().contains("500 元"));
//    }
//
//    // 用例 3：订单创建完整流程
//    @Test
//    public void testOrderCreation() {
//        // 1. 对话生成订单数据
//        OrderData orderData = dialogService.generateOrderData(slots);
//
//        // 2. 调用订单服务
//        OrderCreateDTO dto = convertToDTO(orderData);
//        OrderVO order = orderService.create(dto);
//
//        // 3. 验证订单落库
//        assertNotNull(order.getId());
//        assertEquals("PENDING_APPROVAL", order.getStatus());
//
//        // 4. 验证审批记录创建
//        Approval approval = approvalMapper.selectByOrderId(order.getId());
//        assertNotNull(approval);
//        assertEquals("SUBMITTED", approval.getStatus());  // ❗ 这里会暴露问题
//    }


}
