package com.bootcamp.reports.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clase de tranferencia de datos para los movimientos de los clientes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class AverageDay {
	private Double customer;
//	private Double transaction;
//	private Double transaction;
//	private Double transaction;
}
