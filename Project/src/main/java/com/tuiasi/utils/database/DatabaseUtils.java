package com.tuiasi.utils.database;

import com.tuiasi.crawler_module.model.StockContext;
import com.tuiasi.crawler_module.repository.StockContextRepository;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileReader;

@Component
public class DatabaseUtils {

    private StockContextRepository stockContextRepository;

    @Autowired
    public DatabaseUtils(StockContextRepository stockContextRepository) {
        this.stockContextRepository = stockContextRepository;
    }

    public void initStockSymbolDatabase() {
        JSONParser jsonParser = new JSONParser();
        try {
            JSONArray jsonArray = (JSONArray) jsonParser.parse(new FileReader("symbols.json"));
            for (JSONObject jsonObject : (Iterable<JSONObject>) jsonArray) {
                stockContextRepository.add(
                        StockContext.builder()
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
//        if(stockSymbolRepository.checkIfTableIsEmpty())
//            this.initStockSymbolDatabase();
    }


}
