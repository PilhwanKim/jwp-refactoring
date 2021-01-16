package kitchenpos.application;

import kitchenpos.dao.MenuDao;
import kitchenpos.dao.OrderDao;
import kitchenpos.dao.OrderLineItemDao;
import kitchenpos.dao.OrderTableDao;
import kitchenpos.domain.Order;
import kitchenpos.domain.OrderLineItem;
import kitchenpos.domain.OrderStatus;
import kitchenpos.domain.OrderTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@DisplayName("주문 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    MenuDao menuDao;

    @Mock
    OrderDao orderDao;

    @Mock
    OrderLineItemDao orderLineItemDao;

    @Mock
    OrderTableDao orderTableDao;

    @InjectMocks
    OrderService orderService;

    @DisplayName("주문을 신청한다.")
    @Test
    void create1() {
        //given
        given(orderDao.save(any()))
                .willReturn(new Order());
        given(menuDao.countByIdIn(any()))
                .willReturn(2L);
        given(orderTableDao.findById(any()))
                .willReturn(Optional.of(new OrderTable()));
        given(orderLineItemDao.save(any()))
                .willReturn(new OrderLineItem());
        given(orderDao.save(any()))
                .willReturn(new Order(1L, 2L, "주문", LocalDateTime.now()));

        Order order = new Order();
        List<OrderLineItem> orderLineItems = new ArrayList<>();
        orderLineItems.add(new OrderLineItem());
        orderLineItems.add(new OrderLineItem());
        order.setOrderLineItems(orderLineItems);
        //when

        Order createOrder = orderService.create(order);

        //then
        assertThat(createOrder.getId()).isEqualTo(1L);
        assertThat(createOrder.getOrderTableId()).isEqualTo(2L);
        assertThat(createOrder.getOrderStatus()).isEqualTo("주문");
    }


    @DisplayName("주문을 신청한다 - 주문 항목에 아무것도 없으면 주문할 수 없다.")
    @Test
    void create2() {
        //given
        Order order = new Order();
        order.setOrderLineItems(Collections.EMPTY_LIST);
        //when
        //then
        assertThatThrownBy(() -> orderService.create(order))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("주문 항목이 비어있습니다.");
    }

    @DisplayName("주문을 신청한다 - 주문 항목에 등록하지 않은 메뉴가 있다면 주문할 수 없다.")
    @Test
    void create3() {
        //given
        given(menuDao.countByIdIn(any())).willReturn(1L);

        List<OrderLineItem> orderLineItems = new ArrayList<>();
        orderLineItems.add(new OrderLineItem(1L, 1L, 3));
        orderLineItems.add(new OrderLineItem(2L, 2L, 3));

        Order order = new Order();
        order.setOrderLineItems(orderLineItems);

        //when
        //then
        assertThatThrownBy(() -> orderService.create(order))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("주문 항목에 등록하지 않은 메뉴가 있습니다.");
    }

    @DisplayName("주문을 신청한다 - 주문 테이블이 없거니 비어있으면 주문할 수 없다.")
    @Test
    void create4() {
        //given
        given(menuDao.countByIdIn(any())).willReturn(2L);
        given(orderTableDao.findById(any()))
                .willReturn(Optional.of(new OrderTable(1L, 2L, 0, true)));

        List<OrderLineItem> orderLineItems = new ArrayList<>();
        orderLineItems.add(new OrderLineItem(1L, 1L, 3));
        orderLineItems.add(new OrderLineItem(2L, 2L, 3));

        Order order = new Order();
        order.setOrderLineItems(orderLineItems);
        //when
        //then
        assertThatThrownBy(() -> orderService.create(order))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비어있는 주문 테이블입니다.");
    }

    @DisplayName("주문 목록을 조회할 수 있다.")
    @Test
    void list() {
        //given
        given(orderDao.findAll())
                .willReturn(
                        Arrays.asList(
                                new Order(1L, 2L, "조리", LocalDateTime.now()),
                                new Order(2L, 3L, "식사", LocalDateTime.now())
                        )
                );
        //when
        List<Order> orders = orderService.list();

        //then
        assertThat(orders.size()).isEqualTo(2);

        assertThat(orders.get(0).getId()).isEqualTo(1L);
        assertThat(orders.get(0).getOrderTableId()).isEqualTo(2L);
        assertThat(orders.get(0).getOrderStatus()).isEqualTo("조리");

        assertThat(orders.get(1).getId()).isEqualTo(2L);
        assertThat(orders.get(1).getOrderTableId()).isEqualTo(3L);
        assertThat(orders.get(1).getOrderStatus()).isEqualTo("식사");
    }

    @DisplayName("주문 상태를 변경한다.")
    @Test
    void changeOrderStatus1() {
        //given
        given(orderDao.findById(any()))
                .willReturn(
                        Optional.of(new Order(1L, 1L, OrderStatus.COOKING.name(), LocalDateTime.now()))
                );

        Order changeOrder = new Order();
        changeOrder.setOrderStatus(OrderStatus.MEAL.name());

        //when
        Order changedOrder = orderService.changeOrderStatus(1L, changeOrder);

        //then
        assertThat(changedOrder.getId()).isEqualTo(1L);
        assertThat(changedOrder.getOrderStatus()).isEqualTo(OrderStatus.MEAL.name());
    }

    @DisplayName("주문 상태를 변경한다. - 신청하지 않은 주문은 상태를 변경할 수 없다.")
    @Test
    void changeOrderStatus2() {
        //given
        given(orderDao.findById(any()))
                .willReturn(Optional.empty());

        //when
        //then
        Order changeOrder = new Order();
        changeOrder.setOrderStatus("식사");
        assertThatThrownBy(() -> orderService.changeOrderStatus(1L, changeOrder))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문 상태를 변경한다. - 이미 왼료상태인 주문은 상태를 변경할 수 없다.")
    @Test
    void changeOrderStatus3() {
        //given
        given(orderDao.findById(any()))
                .willReturn(
                        Optional.of(new Order(1L, 1L, OrderStatus.COMPLETION.name(), LocalDateTime.now()))
                );

        Order changeOrder = new Order();
        changeOrder.setOrderStatus("식사");

        //when
        //then
        assertThatThrownBy(() -> orderService.changeOrderStatus(1L, changeOrder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 왼료상태인 주문은 상태 변경이 불가합니다.");
    }
}
