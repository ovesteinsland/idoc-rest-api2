package no.softwarecontrol.idoc.web.signup;

import jakarta.servlet.AsyncContext;
import no.softwarecontrol.idoc.data.entityhelper.CustomerData;

public class SignupClient {

    private final AsyncContext asyncContext;
    private final CustomerData customerData;

    public SignupClient(AsyncContext asyncContext, CustomerData customerData) {
        this.asyncContext = asyncContext;
        this.customerData = customerData;
    }

    public AsyncContext getAsyncContext() {
        return asyncContext;
    }

    public CustomerData getCustomerData() {
        return customerData;
    }
}
