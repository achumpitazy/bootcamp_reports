package com.bootcamp.reports.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bootcamp.reports.clients.AccountsRestClient;
import com.bootcamp.reports.clients.CreditsRestClient;
import com.bootcamp.reports.clients.CustomersRestClient;
import com.bootcamp.reports.clients.TransactionsRestClient;
import com.bootcamp.reports.dto.Account;
import com.bootcamp.reports.dto.Credit;
import com.bootcamp.reports.dto.CreditCard;
import com.bootcamp.reports.dto.Customer;
import com.bootcamp.reports.dto.Movements;
import com.bootcamp.reports.dto.Products;
import com.bootcamp.reports.dto.Transaction;
import com.bootcamp.reports.service.ConsultService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Clase de implementación para la interfaz ConsultService
 */
@Service
public class ConsultServiceImpl implements ConsultService{

	@Autowired
	AccountsRestClient accountsRestClient;
	
	@Autowired
	CreditsRestClient creditsRestClient;
	
	@Autowired
	TransactionsRestClient transactionsRestClient;
	
	@Autowired
	CustomersRestClient customersRestClient;

	/**
	 * Devuelve la lista de productos de un cliente personal segun el id de cliente.
	 * @param customerId
	 * @return Mono<Products>
	 */
	@Override
	public Mono<Products> productXCustomerIdPerson(String customerId) {
        return customersRestClient.getPersonById(customerId).flatMap(p -> {
        	Customer customer = new Customer();
        	customer.setId(p.getId());
			customer.setDocument(p.getDni());
	        customer.setNameCustomer(p.getName().concat(" ").concat(p.getLastName()));
	        customer.setTypeCustomer(p.getTypeCustomer());
        	return obtainProducts(customer, customerId);
        });
	}

	/**
	 * Devuelve la lista de productos de un cliente empresarial segun el id de cliente.
	 * @param customerId
	 * @return Mono<Products>
	 */
	@Override
	public Mono<Products> productXCustomerIdCompany(String customerId) {
		return customersRestClient.getCompanyById(customerId).flatMap(p -> {
			Customer customer = new Customer();
			customer.setId(p.getId());
			customer.setDocument(p.getRuc());
	        customer.setNameCustomer(p.getBusinessName());
	        customer.setTypeCustomer(p.getTypeCustomer());
        	return obtainProducts(customer, customerId);
        });
	}

	/***
	 * Obtiene la lista de productos de los clientes
	 * @param customer
	 * @param customerId
	 * @return
	 */
	private Mono<Products> obtainProducts(Customer customer, String customerId){
		List<Account> listAccounts = new ArrayList<>();
        List<Credit> listCredits = new ArrayList<>();
        List<CreditCard> listCreditCards = new ArrayList<>();
        return accountsRestClient.getAllAccountXCustomerId(customerId).collectList().flatMap(a -> {
        	listAccounts.addAll(a);
        	return creditsRestClient.getAllCreditXCustomerId(customerId).collectList().flatMap(c -> {
        		listCredits.addAll(c);
        		return creditsRestClient.getAllCreditCardXCustomerId(customerId).collectList().flatMap(cc -> {
        			listCreditCards.addAll(cc);
        			return Mono.just(new Products(customer, listAccounts, listCredits, listCreditCards));
        		});
        	});
        });
	}

	/**
	 * Muestra la lista de movimientos de una cuenta segun su id.
	 * @param id
	 * @return Mono<Movements>
	 */
	@Override
	public Mono<Movements> movementXAccountId(String id) {
		List<Transaction> listTransaction = new ArrayList<>();
		return transactionsRestClient.getAllXProductId(id).collectList().flatMap(t ->{
			listTransaction.addAll(t);
			return accountsRestClient.getAccountById(id).flatMap(a -> {
				return obtainCustomer(listTransaction, a.getCustomerId(), a.getTypeCustomer());
			});
		});	
	};

	/**
	 * Muestra la lista de movimientos de un credito segun su id.
	 * @param id
	 * @return Mono<Movements>
	 */
	@Override
	public Mono<Movements> movementXCreditId(String id) {
		List<Transaction> listTransaction = new ArrayList<>();
		return transactionsRestClient.getAllXProductId(id).collectList().flatMap(t ->{
			listTransaction.addAll(t);
			return creditsRestClient.getCreditById(id).flatMap(a -> {
				return obtainCustomer(listTransaction, a.getCustomerId(), a.getTypeCustomer());
			});
		});	
	};

	/**
	 * Muestra la lista de movimientos de una tarjeta de credito segun su id.
	 * @param id
	 * @return Mono<Movements>
	 */
	@Override
	public Mono<Movements> movementXCreditCardId(String id) {
		List<Transaction> listTransaction = new ArrayList<>();
		return transactionsRestClient.getAllXProductId(id).collectList().flatMap(t ->{
			listTransaction.addAll(t);
			return creditsRestClient.getCreditCardById(id).flatMap(a -> {
				return obtainCustomer(listTransaction, a.getCustomerId(), a.getTypeCustomer());
			});
		});	
	};

	/**
	 * Obtiene al cliente segun su tipo(empresarial/personal) y lo convierte en una clase Customer
	 * @param listTransaction
	 * @param id
	 * @param type
	 * @return Mono<Movements>
	 */
	private Mono<Movements> obtainCustomer(List<Transaction> listTransaction, String id, String type){
		if(type.equals("PERSON")) {
			return customersRestClient.getPersonById(id).flatMap(p -> {
				Customer customer = new Customer();
				customer.setDocument(p.getDni());
				customer.setNameCustomer(p.getName().concat(" ").concat(p.getLastName()));
				customer.setTypeCustomer(p.getTypeCustomer());
				return Mono.just(new Movements(customer, listTransaction));
			});
		}else {
			return customersRestClient.getCompanyById(id).flatMap(p -> {
				Customer customer = new Customer();
				customer.setDocument(p.getRuc());
				customer.setNameCustomer(p.getBusinessName());
				customer.setTypeCustomer(p.getTypeCustomer());
				return Mono.just(new Movements(customer, listTransaction));
			});
		}
	}

	@Override
	public Mono<Movements> commissionXAccountId(String id) {
		LocalDateTime myDateObj = LocalDateTime.now();
		List<Transaction> listTransaction = new ArrayList<>();
		return transactionsRestClient.getAllXProductId(id)
				.filter(a -> a.getTransactionType().equals("COMISION"))
				.filter(a -> (a.getTransactionDate().getMonthValue()==(myDateObj.getMonthValue())) && (a.getTransactionDate().getYear()==(myDateObj.getYear())))
				.collectList().flatMap(t ->{
					listTransaction.addAll(t);
					return accountsRestClient.getAccountById(id).flatMap(a -> {
						return obtainCustomer(listTransaction, a.getCustomerId(), a.getTypeCustomer());
				});
		});	
	}

	@Override
	public Flux<Double> averageBalancesXCustomerIdPerson(String id) {
		return null;
	}


}
