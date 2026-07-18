package com.complex.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.customer.CustomerService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class CustomerTokenAuthenticationFilterTests {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void acceptsBearerCustomerTokenForCustomerPaths() throws Exception {
        CustomerService customerService = mock(CustomerService.class);
        when(customerService.resolveCustomerIdQuietly("cust_token_1")).thenReturn(Optional.of("customer_1"));
        CustomerTokenAuthenticationFilter filter = filterWith(customerService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/customer/stored-value/accounts");
        request.setRequestURI("/api/v1/customer/stored-value/accounts");
        request.addHeader("Authorization", "Bearer cust_token_1");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        AccessActor actor = (AccessActor) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(actor.id()).isEqualTo("customer_1");
        assertThat(actor.accountType()).isEqualTo("customer");
        verify(customerService).resolveCustomerIdQuietly("cust_token_1");
    }

    @Test
    void prefersExplicitCustomerTokenHeaderOverBearerHeader() throws Exception {
        CustomerService customerService = mock(CustomerService.class);
        when(customerService.resolveCustomerIdQuietly("cust_header_token")).thenReturn(Optional.of("customer_2"));
        CustomerTokenAuthenticationFilter filter = filterWith(customerService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/customer/stored-value/accounts");
        request.setRequestURI("/api/v1/customer/stored-value/accounts");
        request.addHeader("X-Customer-Token", " cust_header_token ");
        request.addHeader("Authorization", "Bearer cust_bearer_token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        AccessActor actor = (AccessActor) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(actor.id()).isEqualTo("customer_2");
        verify(customerService).resolveCustomerIdQuietly("cust_header_token");
    }

    private CustomerTokenAuthenticationFilter filterWith(CustomerService customerService) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("customerService", customerService);
        return new CustomerTokenAuthenticationFilter(beanFactory.getBeanProvider(CustomerService.class));
    }
}
