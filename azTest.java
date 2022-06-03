package utils;

import contingency.PaymentContingency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Customer {
    private Integer ID;
    private String firstName;
    private String lastName;
}

class PaymentMethod {
    private String type;
    private Float amount;

    public String getType() {
        return type;
    }
}

class Restaurant {
    private Integer ID;
    private String name;
    private String address;
}

class Order {
    private Integer ID;
    private Customer customer;
    private Restaurant restaurant;
    private OrderDetail[] orderDetails;
    private PaymentDetail[] paymentDetails;

    public OrderDetail[] getOrderDetails(){
        return this.orderDetails;
    }
    public PaymentDetail[] getPaymentDetails(){
        return this.paymentDetails;
    }
    public Integer getID(){
        return  this.ID;
    }
}

class OrderDetail {
    private String Item;
    private Integer quantity;
    private Float amount;

    public Float getAmount() {
        return this.amount;
    }
}

class Payment {
    private Integer ID;
    private Float transactionAmount;
    private PaymentMethod pm;
    private String status;
    private Integer orderId;

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }
    public Float getTransactionAmount() {
        return this.transactionAmount;
    }
    public String getStatus() {
        return this.status;
    }
    public PaymentMethod getPaymentMethod() {
        return this.pm;
    }
}

class PaymentDetail {
    private Float amount;
    private PaymentMethod paymentMethod;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
}

class PaymentService {

    PaymentProcessorFactory paymentProcessorFactory;
    PaymentRepository paymentRepository;
    PaymentContingencyService paymentContingencyService;

    public PaymentService(PaymentProcessorFactory paymentProcessorFactory, PaymentRepository paymentRepository, PaymentContingencyService paymentContingencyService){
        this.paymentProcessorFactory = paymentProcessorFactory;
        this.paymentRepository = paymentRepository;
        this.paymentContingencyService = paymentContingencyService;
    }

    public boolean pay(Order order) {
        List<PaymentDetail> payDetails = Arrays.asList(order.getPaymentDetails());
        List<Payment> payments = new ArrayList<>();
        boolean paymentResult = false;
        try {
            for (PaymentDetail payDetail : payDetails) {
                PaymentMethod pm = payDetail.getPaymentMethod();
                PaymentProcessorService processor = paymentProcessorFactory.getProcessorService(pm.getType());
                payments.add(processor.executePayment(payDetail));
            }
            setOrderId(payments, order.getID());
            Float totalAmount = calculateOrderTotalAmount(order.getOrderDetails());
            paymentResult = validateOrderTotalPayments(payments, totalAmount);
            if (paymentResult == false) {
                reversePayments(payments);
            }
            save(payments);
        } catch (Exception ex){
            //log Error
            //send Metrics
            //create a fallback strategy to retry the payment later
            Payment[] payArray = new Payment[payments.size()];
            payments.toArray(payArray);
            paymentContingencyService.createContingency(payArray);
        }
        return paymentResult;
    }

    private void reversePayments(List<Payment> payments) {
        for (Payment payment:payments) {
            PaymentMethod pm = payment.getPaymentMethod();
            PaymentProcessorService processor = paymentProcessorFactory.getProcessorService(pm.getType());
            processor.reversePayment(payment);
        }
    }

    private void save(List<Payment> payments) {
        for (Payment payment : payments) {
            paymentRepository.save(payment);
        }
    }

    private boolean validateOrderTotalPayments(List<Payment> payments, Float totalAmount) {
        for (Payment payment : payments) {
            if(payment.getStatus() == "APPROVED") {
                totalAmount = totalAmount - payment.getTransactionAmount();
            }
        }
        return totalAmount == 0 ? true : false;
    }

    private void setOrderId(List<Payment> payments, Integer orderId) {
        payments.forEach(p -> p.setOrderId(orderId));
    }

    private Float calculateOrderTotalAmount(OrderDetail[] orderDetails) {
        List<OrderDetail> orderList = Arrays.asList(orderDetails);
        Float totalOrderAmount = (float) orderList.stream().mapToDouble(o -> o.getAmount()).sum();
        return totalOrderAmount;
    }
}

interface PaymentProcessorFactory {
    PaymentProcessorService getProcessorService(String type);
}

class SimpleProcessorFactory implements PaymentProcessorFactory {
    public PaymentProcessorService getProcessorService(String type) {
        if (type.equals("credit_card")) {
            return new CreditCardProcessorService();
        } else if (type.equals("wallet")) {
            return new WalletProcessorService();
        } else if (type.equals("cash")) {
            return new CashProcessorService();
        }
        return null;
    }
}

interface PaymentProcessorService {
    Payment executePayment(PaymentDetail payDetail);
    void reversePayment(Payment payment);
}

class CreditCardProcessorService implements PaymentProcessorService {
    @Override
    public Payment executePayment(PaymentDetail payDetail) {
        return null;
    }

    @Override
    public void reversePayment(Payment payment) {

    }
}

class WalletProcessorService implements PaymentProcessorService {
    @Override
    public Payment executePayment(PaymentDetail payDetail) {
        return null;
    }

    @Override
    public void reversePayment(Payment payment) {

    }
}

class CashProcessorService implements PaymentProcessorService {
    @Override
    public Payment executePayment(PaymentDetail payDetail) {
        return null;
    }

    @Override
    public void reversePayment(Payment payment) {

    }
}

interface PaymentRepository {
    void save(Payment pay);
}

interface PaymentContingencyService {
    void createContingency(Payment[] payments);
}
