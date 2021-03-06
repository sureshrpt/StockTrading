/*******************************************************************************
 * Project Stock Trading
 * Copyright (c) 2016-2017
 * All rights reserved.
 *******************************************************************************/
package com.jpmorgan.daily.trade.action;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.jpmorgan.daily.trade.exception.StockTradingException;
import com.jpmorgan.daily.trade.model.OrderType;
import com.jpmorgan.daily.trade.model.Stock;
import com.jpmorgan.daily.trade.model.StockRanking;

/**
 * @author suresh
 * The class is to process the stock data
 */
public class ProcessStockDataAction implements IProcessStockData {

	/**
	 * Condition to filter the Outgoing (BUT)
	 */
	private static Predicate<Stock> predicateOutgoing = data -> data.getOrderType().equals(OrderType.B);

	/**
	 * Condition to filter the Incoming (SELL)
	 */
	private static Predicate<Stock> predicateIncoming = data -> data.getOrderType().equals(OrderType.S);

	/* (non-Javadoc)
	 * @see com.jpmorgan.daily.trade.action.IProcessStockData#computeIncomingDayAmout(java.util.List)
	 */
	@Override
	public Map<LocalDate, BigDecimal> computeIncomingDayAmout(List<Stock> stocks) throws StockTradingException {
		return computeDayAmoutTransaction(stocks, predicateIncoming);
	}

	/* (non-Javadoc)
	 * @see com.jpmorgan.daily.trade.action.IProcessStockData#computeOutgoingDayAmout(java.util.List)
	 */
	@Override
	public Map<LocalDate, BigDecimal> computeOutgoingDayAmout(List<Stock> stocks) throws StockTradingException {
		return computeDayAmoutTransaction(stocks, predicateOutgoing);
	}

	/* (non-Javadoc)
	 * @see com.jpmorgan.daily.trade.action.IProcessStockData#computeIncomingDayRanking(java.util.List)
	 */
	@Override
	public List<StockRanking> computeIncomingDayRanking(List<Stock> stocks) throws StockTradingException {
		return computeDayRanking(stocks, predicateIncoming);
	}

	/* (non-Javadoc)
	 * @see com.jpmorgan.daily.trade.action.IProcessStockData#computeOutgoingDayRanking(java.util.List)
	 */
	@Override
	public List<StockRanking> computeOutgoingDayRanking(List<Stock> stocks) throws StockTradingException {
		return computeDayRanking(stocks, predicateOutgoing);
	}

	/**
	 * The method is to compute Day amount transaction for both BUY and SELL Order Type
	 * @param stocks The incoming stock data
	 * @param predicate the condition to added as part of filter
	 * @return date with sumTotal
	 */
	private static Map<LocalDate, BigDecimal> computeDayAmoutTransaction(List<Stock> stocks, Predicate<Stock> predicate) 
			throws StockTradingException {
		return stocks.stream()
			.filter(predicate)
			.collect(groupingBy(Stock::getUpdatedSettlementDate,
				Collectors.mapping(Stock::getTotalTradeAmt,
						Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
	}

	/**
	 * 
	 * @param stocks The incoming stock data
	 * @param predicate the condition to added as part of filter
	 * @return stockRating object list
	 */
	private static List<StockRanking> computeDayRanking(List<Stock> stocks, Predicate<Stock> predicate) 
			throws StockTradingException {
		List<StockRanking> rankingObj = new ArrayList<StockRanking>();
		stocks.stream().filter(predicate).collect(groupingBy(Stock::getUpdatedSettlementDate, toSet()))
				.forEach(computeSortingAndRanking(rankingObj));
		return rankingObj;
	}

	/**
	 * The method is to sort the based on the amount
	 * @param rankingObj the incoming ranking object
	 * @return
	 */
	private static BiConsumer<? super LocalDate, ? super Set<Stock>> computeSortingAndRanking(List<StockRanking> rankingObj) 
			throws StockTradingException{
		return (date, stocks2) -> {
			final AtomicInteger couter = new AtomicInteger(1);
			try{
				LinkedList<StockRanking> ranking = stocks2.stream()
						.sorted((obj1, obj2) -> obj2.getTotalTradeAmt().compareTo(obj1.getTotalTradeAmt()))
						.map(instruction -> new StockRanking(couter.getAndIncrement(), instruction.getEntity(), date))
						.collect(Collectors.toCollection(LinkedList::new));
				rankingObj.addAll(ranking);
			}catch(StockTradingException e){
				throw e;
			}
		};
	}
}
