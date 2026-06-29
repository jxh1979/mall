package com.complex.platform.notification;

public interface SmsSender {

    SmsSendResult send(SmsMessage message);
}
