package pkg.trader;

import java.util.ArrayList;

import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.order.BuyOrder;
import pkg.order.Order;
import pkg.order.OrderType;
import pkg.order.SellOrder;
import pkg.stock.Stock;

public class Trader {
	// Name of the trader
	String name;
	// Cash left in the trader's hand
	double cashInHand;
	// Stocks owned by the trader
	ArrayList<Order> position;
	// Orders placed by the trader
	ArrayList<Order> ordersPlaced;

	public Trader(String name, double cashInHand) {
		super();
		this.name = name;
		this.cashInHand = cashInHand;
		this.position = new ArrayList<Order>();
		this.ordersPlaced = new ArrayList<Order>();
	}

	public void buyFromBank(Market m, String symbol, int volume)
			throws StockMarketExpection {
		// Buy stock straight from the bank
		// Need not place the stock in the order list
		
		// If the stock's price is larger than the cash possessed, then an
		// exception is thrown
		if ((double)volume * m.getStockForSymbol(symbol).getPrice() > cashInHand) {
			throw new StockMarketExpection("Not enough cash in hand to place buy order.");
		}
		
		// Add it straight to the user's position
		int previousVolume = 0;
		for (Order order: position) {
			if (order.getStockSymbol() == symbol)
			{
				previousVolume += order.getSize();
				position.remove(order);
				break;
			}
		}
		Order newOrder = new BuyOrder(symbol, volume + previousVolume, m.getStockForSymbol(symbol).getPrice(), this);
		this.position.add(newOrder);

		// Adjust cash possessed since the trader spent money to purchase a
		// stock.
		this.cashInHand -= (double)volume * m.getStockForSymbol(symbol).getPrice();
	}

	public void placeNewOrder(Market m, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
		// Place a new order and add to the orderlist
		// Also enter the order into the orderbook of the market.
		// Note that no trade has been made yet. The order is in suspension
		// until a trade is triggered.
		//
		
		Order newOrder;
		if (orderType == OrderType.BUY) {
			newOrder = new BuyOrder(symbol, volume, price, this);
			
			// If the stock's price is larger than the cash possessed, then an
			// exception is thrown
			if ((double)volume * price > cashInHand) {
				throw new StockMarketExpection("Not enough cash in hand to place buy order.");
			}
		} else {
			newOrder = new SellOrder(symbol, volume, price, this);
			
		    // Also a person cannot place a sell order for a stock that he does not own. 
			boolean ownsStock = false;
			for (Order order: position) {
				if (order.getStockSymbol() == newOrder.getStockSymbol())
				{
					ownsStock = true;
					//Or he cannot sell more stocks than he possesses. Throw an
					// exception in these cases.
				    if (order.getSize() < newOrder.getSize()) {
						throw new StockMarketExpection("You do not own enough stock to place this order.");
				    }
				}
			}
		    if (ownsStock == false) {
				throw new StockMarketExpection("You must own the stock to sell it.");
		    }
		}
		
		// A trader cannot place two orders for the same stock, throw an
		// exception if there are multiple orders for the same stock.
		boolean alreadyPlacedOrder = false;
		for (Order order: ordersPlaced) {
			if (order.getStockSymbol() == newOrder.getStockSymbol()) alreadyPlacedOrder = true;
		}
	    if (alreadyPlacedOrder) {
			throw new StockMarketExpection("Cannot place two orders for the same stock.");
	    }
	    
	    ordersPlaced.add(newOrder);
	    m.addOrder(newOrder);
	}

	public void placeNewMarketOrder(Market m, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
		// Similar to the other method, except the order is a market order
		
		Stock orderStock = m.getStockForSymbol(symbol);
		Order newOrder;
		if (orderType == OrderType.BUY) {
			newOrder = new BuyOrder(symbol, volume, true, this);
			
			// If the stock's price is larger than the cash possessed, then an
			// exception is thrown
			if ((double)volume * orderStock.getPrice() > cashInHand) {
				throw new StockMarketExpection("Not enough cash in hand to place buy order.");
			}
		} else {
			newOrder = new SellOrder(symbol, volume, true, this);
			
		    // Also a person cannot place a sell order for a stock that he does not own. 
			boolean ownsStock = false;
			for (Order order: position) {
				if (order.getStockSymbol() == newOrder.getStockSymbol())
				{
					ownsStock = true;
					//Or he cannot sell more stocks than he possesses. Throw an
					// exception in these cases.
				    if (order.getSize() < newOrder.getSize()) {
						throw new StockMarketExpection("You do not own enough stock to place this order.");
				    }
				}
			}
		    if (ownsStock == false) {
				throw new StockMarketExpection("You must own the stock to sell it.");
		    }
		}
		
		// A trader cannot place two orders for the same stock, throw an
		// exception if there are multiple orders for the same stock.
		boolean alreadyPlacedOrder = false;
		for (Order order: ordersPlaced) {
			if (order.getStockSymbol() == newOrder.getStockSymbol()) alreadyPlacedOrder = true;
		}
	    if (alreadyPlacedOrder) {
			throw new StockMarketExpection("Cannot place two orders for the same stock.");
	    }
	    
	    ordersPlaced.add(newOrder);
	    m.addOrder(newOrder);
	    
	}

	public void tradePerformed(Order o, double matchPrice)
			throws StockMarketExpection {
		// Notification received that a trade has been made, the parameters are
		// the order corresponding to the trade, and the match price calculated
		// in the order book. Note than an order can sell some of the stocks he
		// bought, etc. Or add more stocks of a kind to his position. Handle
		// these situations.

		// Update the trader's orderPlaced, position, and cashInHand members
		// based on the notification.
		
		if (o.isMarketOrder()) o.setPrice(matchPrice);
		this.ordersPlaced.remove(o);
		
		double stockValue = o.getPrice() * (double)o.getSize();

		if (o.orderType == OrderType.BUY)
		{
			int previousVolume = 0;
			for (Order order: position) {
				if (order.getStockSymbol() == o.getStockSymbol())
				{
					previousVolume += order.getSize();
					position.remove(order);
					break;
				}
			}
			this.cashInHand -= stockValue;
			
			o.setSize(o.getSize() + previousVolume);
			//add to position
			this.position.add(o);
		} else
		{
			//remove from position
			int previousVolume = 0;
			for (Order order: position) {
				if (order.getStockSymbol() == o.getStockSymbol())
				{
					previousVolume += order.getSize();
					position.remove(order);
					break;
				}
			}
			this.cashInHand += stockValue;

			o.setSize(previousVolume - o.getSize());
			if (o.getSize() > 0) this.position.add(o);
		}	
	}

	public void printTrader() {
		System.out.println("Trader Name: " + name);
		System.out.println("=====================");
		System.out.println("Cash: " + cashInHand);
		System.out.println("Stocks Owned: ");
		for (Order o : position) {
			o.printStockNameInOrder();
		}
		System.out.println("Stocks Desired: ");
		for (Order o : ordersPlaced) {
			o.printOrder();
		}
		System.out.println("+++++++++++++++++++++");
		System.out.println("+++++++++++++++++++++");
	}
}
