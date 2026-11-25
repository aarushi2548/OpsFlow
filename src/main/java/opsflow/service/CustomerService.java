package opsflow.service;

import opsflow.dto.CreateCustomerRequest;
import opsflow.dto.CustomerResponse;
import opsflow.entity.Customer;
import opsflow.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Customer email already exists"
            );
        }

        Customer customer = new Customer();
        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());

        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAll() {
        return customerRepository.findAll()
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        return toResponse(findCustomer(id));
    }

    @Transactional
    public CustomerResponse deactivate(Long id) {
        Customer customer = findCustomer(id);
        customer.setActive(false);
        return toResponse(customerRepository.save(customer));
    }

    private Customer findCustomer(Long id) {
        return customerRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Customer not found"
            ));
    }

    private CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(
            c.getId(), c.getName(), c.getEmail(),
            c.getPhone(), c.getAddress(),
            c.getActive(), c.getCreatedAt()
        );
    }
}