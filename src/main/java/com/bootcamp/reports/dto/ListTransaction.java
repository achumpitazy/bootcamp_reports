package com.bootcamp.reports.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ListTransaction {
	
	private LocalDate transactionDate;
	private Double amount;
	private String productId;

}
