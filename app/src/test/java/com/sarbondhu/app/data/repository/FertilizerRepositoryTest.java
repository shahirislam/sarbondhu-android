package com.sarbondhu.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.FertilizerDetail;
import com.sarbondhu.app.data.model.FertilizerItem;
import com.sarbondhu.app.data.model.HomeData;
import com.sarbondhu.app.data.model.MockData;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.model.ShopStock;
import com.sarbondhu.app.data.model.Stock;
import com.sarbondhu.app.data.model.Weather;
import com.sarbondhu.app.data.source.DataLoader;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FertilizerRepositoryTest {

    private static Fertilizer fertilizer(int id) {
        return new Fertilizer(id, "সার " + id, "c", "n", "d", "u",
                Collections.emptyList(), 10.0, "কেজি");
    }

    private static Shop shop(int id) {
        return new Shop(id, "দোকান " + id, "o", "017", "জেলা", "উপজেলা", "ঠিকানা");
    }

    private static MockData sampleData() {
        return new MockData(
                Arrays.asList(fertilizer(1), fertilizer(2), fertilizer(3)),
                Arrays.asList(shop(10), shop(20), shop(30)),
                Arrays.asList(
                        new Stock(10, 1, 100.0, 27.0),
                        new Stock(20, 1, 0.0, 27.0),
                        new Stock(30, 1, 400.0, 28.0),
                        new Stock(99, 1, 999.0, 27.0),   // unknown shop → skipped
                        new Stock(10, 2, 0.0, 20.0)));   // fertilizer 2 only out of stock
    }

    private static FertilizerRepository repo(MockData data) {
        return new FertilizerRepository(() -> data);
    }

    @Test
    public void fertilizerItems_countOnlyKnownShopsWithStock() throws IOException {
        List<FertilizerItem> items = repo(sampleData()).getFertilizerItems();

        assertEquals(3, items.size());
        assertEquals(1, items.get(0).getFertilizer().getId());
        assertEquals(2, items.get(0).getShopCount());
        assertEquals(0, items.get(1).getShopCount());
        assertEquals(0, items.get(2).getShopCount());
    }

    @Test
    public void detail_shopsSortedByQuantityDescending_unknownShopSkipped() throws IOException {
        FertilizerDetail detail = repo(sampleData()).getFertilizerDetail(1);

        List<ShopStock> stocks = detail.getShopStocks();
        assertEquals(3, stocks.size());
        assertEquals(30, stocks.get(0).getShop().getId());
        assertEquals(10, stocks.get(1).getShop().getId());
        assertEquals(20, stocks.get(2).getShop().getId());
        assertTrue(stocks.get(0).isInStock());
        assertFalse(stocks.get(2).isInStock());
        assertEquals(28.0, stocks.get(0).getSellingPrice(), 0.0001);
    }

    @Test
    public void detail_fertilizerWithoutStockRowsHasEmptyList() throws IOException {
        FertilizerDetail detail = repo(sampleData()).getFertilizerDetail(3);

        assertEquals(3, detail.getFertilizer().getId());
        assertTrue(detail.getShopStocks().isEmpty());
    }

    @Test
    public void inactiveDealersAreHiddenFromFarmers() throws IOException {
        Shop inactive = new Shop(30, "দোকান 30", "o", "017", "জেলা", "উপজেলা", "ঠিকানা",
                "dealer30", "pw", false);
        MockData data = new MockData(
                Collections.singletonList(fertilizer(1)),
                Arrays.asList(shop(10), inactive),
                Arrays.asList(new Stock(10, 1, 100.0, 27.0), new Stock(30, 1, 400.0, 28.0)));
        FertilizerRepository repository = repo(data);

        assertEquals(1, repository.getFertilizerItems().get(0).getShopCount());
        List<ShopStock> stocks = repository.getFertilizerDetail(1).getShopStocks();
        assertEquals(1, stocks.size());
        assertEquals(10, stocks.get(0).getShop().getId());
    }

    @Test
    public void detail_unknownIdReturnsNull() throws IOException {
        assertNull(repo(sampleData()).getFertilizerDetail(42));
    }

    @Test
    public void homeData_containsItemsAndWeather() throws IOException {
        MockData data = new MockData(
                Collections.singletonList(fertilizer(1)),
                Collections.singletonList(shop(10)),
                Collections.singletonList(new Stock(10, 1, 5.0, 27.0)),
                new Weather("ঢাকা", 35));

        HomeData home = repo(data).getHomeData();

        assertEquals(1, home.getFertilizers().size());
        assertEquals(1, home.getFertilizers().get(0).getShopCount());
        assertEquals(35, home.getWeather().getRainProbability());
    }

    @Test
    public void dataIsLoadedOnlyOnce() throws IOException {
        int[] calls = {0};
        FertilizerRepository repository = new FertilizerRepository(() -> {
            calls[0]++;
            return sampleData();
        });

        repository.getHomeData();
        repository.getFertilizerDetail(1);

        assertEquals(1, calls[0]);
    }

    @Test
    public void loadFailureIsNotCached_retrySucceeds() throws IOException {
        int[] calls = {0};
        DataLoader flaky = () -> {
            calls[0]++;
            if (calls[0] == 1) {
                throw new IOException("boom");
            }
            return sampleData();
        };
        FertilizerRepository repository = new FertilizerRepository(flaky);

        try {
            repository.getFertilizerItems();
            fail("expected IOException");
        } catch (IOException expected) {
            // first attempt fails
        }

        assertEquals(3, repository.getFertilizerItems().size());
    }
}
