/*
 * Copyright (c) 2010. Gridshore
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.samples.trader.webui.init;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.callbacks.FutureCallback;
import org.axonframework.commandhandling.callbacks.NoOpCallback;
import org.axonframework.domain.AggregateIdentifier;
import org.axonframework.domain.StringAggregateIdentifier;
import org.axonframework.eventstore.mongo.MongoEventStore;
import org.axonframework.eventstore.mongo.MongoTemplate;
import org.axonframework.samples.trader.app.api.order.CreateOrderBookCommand;
import org.axonframework.samples.trader.app.api.tradeitem.CreateTradeItemCommand;
import org.axonframework.samples.trader.app.api.user.CreateUserCommand;
import org.axonframework.samples.trader.app.query.MongoHelper;
import org.axonframework.samples.trader.app.query.tradeitem.TradeItemEntry;
import org.axonframework.samples.trader.app.query.tradeitem.TradeItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * <p>Initializes the repository with a number of users, trade items and order books</p>
 *
 * @author Jettro Coenradie
 */
@Component
public class DBInit {

    private CommandBus commandBus;
    private TradeItemRepository tradeItemRepository;
    private MongoHelper mongo;
    private MongoTemplate systemAxonMongo;
    private MongoEventStore eventStore;

    @Autowired
    public DBInit(CommandBus commandBus,
                  TradeItemRepository tradeItemRepository,
                  MongoHelper mongo,
                  MongoTemplate systemMongo,
                  MongoEventStore eventStore) {
        this.commandBus = commandBus;
        this.tradeItemRepository = tradeItemRepository;
        this.mongo = mongo;
        this.systemAxonMongo = systemMongo;
        this.eventStore = eventStore;
    }

    public String obtainInfo() {
        Set<String> collectionNames = systemAxonMongo.database().getCollectionNames();
        StringBuilder sb = new StringBuilder();
        for (String name : collectionNames) {
            sb.append(name);
            sb.append("  ");
        }
        return sb.toString();
    }

    public void createItems() {
        mongo.getDatabase().dropDatabase();
        systemAxonMongo.domainEventCollection().drop();
        systemAxonMongo.snapshotEventCollection().drop();
        mongo.users().drop();
        mongo.orderBooks().drop();
        mongo.orders().drop();
        mongo.tradeItems().drop();
        mongo.tradesExecuted().drop();

        AggregateIdentifier userIdentifier = createuser("Buyer One", "buyer1");
        createuser("Buyer two", "buyer2");
        createuser("Buyer three", "buyer3");
        createuser("Admin One", "admin1");

        createTradeItems(userIdentifier);
        createOrderBooks();
        eventStore.ensureIndexes();
    }

    private void createTradeItems(AggregateIdentifier userIdentifier) {
        CreateTradeItemCommand command = new CreateTradeItemCommand(userIdentifier, "Philips", 1000, 10000);
        commandBus.dispatch(command);

        command = new CreateTradeItemCommand(userIdentifier, "Shell", 500, 5000);
        commandBus.dispatch(command);

        command = new CreateTradeItemCommand(userIdentifier, "Bp", 15000, 100000);
        commandBus.dispatch(command);

//        To bo used for performance tests
//        for (int i=0; i < 1000; i++) {
//            command = new CreateTradeItemCommand(userIdentifier, "Stock " + i, 15000, 100000);
//            commandBus.dispatch(command);
//        }

    }

    private void createOrderBooks() {
        List<TradeItemEntry> tradeItemEntries = tradeItemRepository.listAllTradeItems();

        for (TradeItemEntry tradeItemEntry : tradeItemEntries) {
            CreateOrderBookCommand command = new CreateOrderBookCommand(
                    new StringAggregateIdentifier(tradeItemEntry.getIdentifier()));
            commandBus.dispatch(command, NoOpCallback.INSTANCE);
        }
    }


    private AggregateIdentifier createuser(String longName, String userName) {
        CreateUserCommand createUser = new CreateUserCommand(longName, userName, userName);
        FutureCallback<AggregateIdentifier> createUserCallback =
                new FutureCallback<AggregateIdentifier>();
        commandBus.dispatch(createUser, createUserCallback);
        AggregateIdentifier userIdentifier;
        try {
            userIdentifier = createUserCallback.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return userIdentifier;
    }
}
