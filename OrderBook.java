package pkg.order;

import java.util.Collections;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.stock.Stock;

public class OrderBook {
	Market m;
	HashMap<String, ArrayList<Order>> buyOrders;
	HashMap<String, ArrayList<Order>> sellOrders;

	public OrderBook(Market m) {
		this.m = m;
		buyOrders = new HashMap<String, ArrayList<Order>>();
		sellOrders = new HashMap<String, ArrayList<Order>>();
	}

	public void addToOrderBook(Order order) {
		if (order.orderType == OrderType.BUY) {
			ArrayList<Order> buyList = buyOrders.get(order.getStockSymbol());
			if (buyList == null) buyList = new ArrayList<Order>();
			buyList.add(order);
			buyOrders.put(order.getStockSymbol(), buyList);
		} else {
			ArrayList<Order> sellList = sellOrders.get(order.getStockSymbol());
			if (sellList == null) sellList = new ArrayList<Order>();
			sellList.add(order);
			sellOrders.put(order.getStockSymbol(), sellList);
		}
	}

	public void trade() {
		// Complete the trading.
		// --> 1. Follow and create the orderbook data representation (see spec)
		// --> 2. Find the matching price
		// --> 3. Update the stocks price in the market using the PriceSetter.
		// --> Note that PriceSetter follows the Observer pattern. Use the pattern.
		// --> 4. Remove the traded orders from the orderbook
		// --> 5. Delegate to trader that the trade has been made, so that the
		// trader's orders can be placed to his possession (a trader's position
		// is the stocks he owns)
		// (Add other methods as necessary)

		
		HashMap<String, ArrayList<Order>> buyOrdersUpdate = new HashMap<String, ArrayList<Order>>();
		HashMap<String, ArrayList<Order>> sellOrdersUpdate = new HashMap<String, ArrayList<Order>>();
		
		
 		for (Map.Entry<String, ArrayList<Order>> entry : buyOrders.entrySet()) {
			ArrayList<Order> buyList = entry.getValue();
			ArrayList<Order> sellList = sellOrders.get(entry.getKey());
			
			
			for (Order buyOrder: buyList) {
				if (buyOrder.isMarketOrder())
				{
					buyOrder.setPrice(Integer.MAX_VALUE);
				}
			}
			Collections.sort(buyList, new Comparator<Order>() {
			    @Override
			    public int compare(Order o1, Order o2) {
			        return Double.compare(o2.getPrice(), o1.getPrice());
			    }
			});
			Collections.sort(sellList, new Comparator<Order>() {
			    @Override
			    public int compare(Order o1, Order o2) {
			        return Double.compare(o1.getPrice(), o2.getPrice());
			    }
			});
			
			
			//Consolidate all the buy orders that have the same price
			ArrayList<Order> buyListCLFP = new ArrayList<Order>();
			for (Order buyOrder: buyList) {
			  
				boolean newPrice = true;
				for (Order existingBuyOrder: buyListCLFP) {
					if (buyOrder.getPrice() == existingBuyOrder.getPrice() && existingBuyOrder != null)
					{
						existingBuyOrder.setSize(buyOrder.getSize() + existingBuyOrder.getSize());
						newPrice = false;
				        break;
					}
				}
				if (newPrice) buyListCLFP.add(buyOrder);
			}
			
			//Consolidate all the sell orders that have the same price
			ArrayList<Order> sellListCLFP = new ArrayList<Order>();
			for (Order sellOrder: sellList) {
			  
				boolean newPrice = true;
				for (Order existingSellOrder: sellListCLFP) {
					if (sellOrder.getPrice() == existingSellOrder.getPrice())
					{
						existingSellOrder.setSize(sellOrder.getSize() + existingSellOrder.getSize());
						newPrice = false;
				        break;
					}
				}
				if (newPrice) sellListCLFP.add(sellOrder);
			}
			
			
			ArrayList<OBMatchEntry> buyCLFP = new ArrayList<OBMatchEntry>();
			int previousBuyCLFP = 0;
			for (Order buyOrder: buyListCLFP) {
				int currentCLFP = previousBuyCLFP + buyOrder.getSize();
				OBMatchEntry matchEntry = new OBMatchEntry();
				matchEntry.price = buyOrder.getPrice();
				matchEntry.buyCLFP = currentCLFP;
		        buyCLFP.add(matchEntry);
		        previousBuyCLFP = currentCLFP;
		      }
			
			ArrayList<OBMatchEntry> sellCLFP = new ArrayList<OBMatchEntry>();
			int previousSellCLFP = 0;
			for (Order sellOrder: sellListCLFP) {
				int currentCLFP = previousSellCLFP + sellOrder.getSize();
				OBMatchEntry matchEntry = new OBMatchEntry();
				matchEntry.price = sellOrder.getPrice();
				matchEntry.sellCLFP = currentCLFP;
		        sellCLFP.add(matchEntry);
		        previousSellCLFP = currentCLFP;
		      }
			
			
			ArrayList<OBMatchEntry> combinedEntries = new ArrayList<OBMatchEntry>();

			OBMatchEntry highestEntry = new OBMatchEntry();
			for (OBMatchEntry buyEntry: buyCLFP) {
				for (OBMatchEntry sellEntry: sellCLFP) {
					int lowestCLFP = Math.min(buyEntry.buyCLFP, sellEntry.sellCLFP);
					if (buyEntry.price == sellEntry.price) {
						
						OBMatchEntry newEntry = new OBMatchEntry();
						newEntry.price = buyEntry.price;
						newEntry.buyCLFP = buyEntry.buyCLFP;
						newEntry.sellCLFP = sellEntry.sellCLFP;
						newEntry.minCLFP = lowestCLFP;
						combinedEntries.add(newEntry);
						
						if (lowestCLFP >= highestEntry.minCLFP) {
							highestEntry.price = buyEntry.price;
							highestEntry.buyCLFP = buyEntry.buyCLFP;
							highestEntry.sellCLFP = sellEntry.sellCLFP;
							highestEntry.minCLFP = lowestCLFP;
						}
					}
				}
			}
			
			
			
			
			System.out.println("/n/n@@@@@@@@@@@@@@@@@@@@@@@@");
			ArrayList<Order> buyListUpdate = new ArrayList<Order>();
			ArrayList<Order> sellListUpdate = new ArrayList<Order>();
			buyListUpdate.addAll(buyList);
			sellListUpdate.addAll(sellList);
			
			for (OBMatchEntry matchedEntry: combinedEntries) {
				System.out.print(matchedEntry.buyCLFP + " - ");
				System.out.print(matchedEntry.price + " - ");
				System.out.println(matchedEntry.sellCLFP);
				
				for (Order buyOrder : buyList)
				{
					boolean didMatch = false;
					Order matchedOrder = buyOrder;
					for (Order sellOrder : sellList) {
						if (buyOrder.getPrice() == sellOrder.getPrice() && matchedEntry.price == buyOrder.getPrice()) {
							
							buyListUpdate.remove(buyOrder);
							sellListUpdate.remove(buyOrder);
							
							int stocksTraded = Math.min(buyOrder.getSize(), sellOrder.getSize());
							buyOrder.size = stocksTraded;
							sellOrder.size = stocksTraded;
							
							try {
								buyOrder.trader.tradePerformed(buyOrder, highestEntry.price);
							} catch (StockMarketExpection e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							try {
								sellOrder.trader.tradePerformed(sellOrder, highestEntry.price);
							} catch (StockMarketExpection e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							didMatch = true;
							matchedOrder = sellOrder;
							break;
						}
					}
					if (didMatch) sellList.remove(matchedOrder);
				}
			}
			System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@\n\n");
			
			
			//Update Stock Price.
			try {
				this.m.updateStockPrice(entry.getKey(), highestEntry.price);
			} catch (StockMarketExpection e) {
				e.printStackTrace();
			}	
			
			for (Order buyOrder: buyList) {
				if (buyOrder.isMarketOrder())
				{
					Stock theStock = this.m.getStockForSymbol(buyOrder.getStockSymbol());
					buyOrder.setPrice(theStock.getPrice());
				}
			}
		
			
			String stockName = entry.getKey();
			buyOrdersUpdate.put(stockName, buyListUpdate);
			sellOrdersUpdate.put(stockName, sellListUpdate);
			
		}
 		
 		buyOrders = buyOrdersUpdate;
 		sellOrders = sellOrdersUpdate;
	}
	
	private class OBMatchEntry {
		double price = 0;
		int buyCLFP = 0;
		int sellCLFP = 0;
		int minCLFP = 0;
	}

}
