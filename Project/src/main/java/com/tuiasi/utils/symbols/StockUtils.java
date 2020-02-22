package com.tuiasi.utils.symbols;

import com.tuiasi.exception.ObjectNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class StockUtils {
    public String[] searchCompanyAndStock(String searchKey) {
        final String uri = "http://d.yimg.com/autoc.finance.yahoo.com/autoc?query=" + searchKey + "&region=1&lang=en&callback=YAHOO.Finance.SymbolSuggest.ssCallback";

        String[] symbols = new RestTemplate().getForObject(uri, String.class).split("\"");
        int i;
        String symbol = "";
        String companyName = "";
        for (i = 0; i < symbols.length; ++i) {
            if (symbols[i].equals("symbol")) {
                symbol = symbols[i + 2];
                companyName = symbols[i + 6];
                break;
            }
        }

        if (i == symbols.length)
            throw new ObjectNotFoundException("Stock symbol not found.");

        return new String[]{symbol, companyName};
    }

}
