package com.auction.server.config;

import com.auction.server.controller.RequestController;
import com.auction.server.dao.*;
import com.auction.server.service.*;

/*
 * Chịu trách nhiệm khởi tạo và liên kết các DAO, Service và Controller.
 */
public class AppConfig {
    private static AppConfig instance;

    private final AuctionDAO auctionDAO;
    private final RequestController requestController;
    private final AuctionService auctionService;
    private final AutoBidService autoBidService;

    private AppConfig() {
        // Khởi tạo các DAO
        UserDAO userDAO = new UserDAOImpl();
        auctionDAO = new AuctionDAOImpl();
        ItemDAO itemDAO = new ItemDAOImpl();
        BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl();
        AutoBidDAO autoBidDAO = new AutoBidDAOImpl();

        // Khởi tạo các Service
        UserMapper userMapper = new UserMapper();
        UserService userService = new UserService(userDAO);
        WalletService walletService = new WalletService(userDAO, auctionDAO, userMapper);
        ItemService itemService = new ItemService(itemDAO);
        AuctionMapper auctionMapper = new AuctionMapper(itemDAO, userDAO, bidTransactionDAO);
        
        autoBidService = new AutoBidService(auctionDAO, autoBidDAO, itemDAO);
        auctionService = new AuctionService(auctionDAO, userDAO, itemService, auctionMapper, autoBidService);
        BidService bidService = new BidService(auctionDAO, bidTransactionDAO, autoBidService, itemDAO, userDAO);
        autoBidService.setBidService(bidService);
        PaymentService paymentService = new PaymentService(auctionDAO, userDAO, itemDAO, auctionMapper);

        // Khởi tạo Controller
        requestController = new RequestController(userService, walletService, auctionService, bidService, autoBidService, paymentService);
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    public AuctionDAO getAuctionDAO() {
        return auctionDAO;
    }

    public AuctionService getAuctionService() {
        return auctionService;
    }

    public AutoBidService getAutoBidService() {
        return autoBidService;
    }

    public RequestController getRequestController() {
        return requestController;
    }
}
