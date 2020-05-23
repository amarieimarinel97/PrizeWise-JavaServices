package com.tuiasi.utils.database;

import com.tuiasi.model.StockSymbol;
import com.tuiasi.repository.StockSymbolRepository;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileReader;
import java.util.Iterator;

@Component
public class DatabaseUtils {

    private StockSymbolRepository stockSymbolRepository;

    @Autowired
    public DatabaseUtils(StockSymbolRepository stockSymbolRepository) {
        this.stockSymbolRepository = stockSymbolRepository;
    }

    public void initStockSymbolDatabase() {
        JSONParser jsonParser = new JSONParser();
        try {
            JSONArray jsonArray = (JSONArray) jsonParser.parse(new FileReader("symbols.json"));
            for (JSONObject jsonObject : (Iterable<JSONObject>) jsonArray) {
                stockSymbolRepository.add(
                        StockSymbol.builder()
                                .name((String) jsonObject.get("Name"))
                                .symbol((String) jsonObject.get("Symbol"))
                                .sector((String) jsonObject.get("Sector"))
                                .build()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostConstruct
    private void init() {
        if(stockSymbolRepository.checkIfTableIsEmpty())
            this.initStockSymbolDatabase();
    }


}
