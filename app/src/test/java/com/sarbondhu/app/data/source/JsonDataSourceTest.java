package com.sarbondhu.app.data.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.model.MockData;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.model.Stock;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class JsonDataSourceTest {

    private static InputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void parse_readsAllSections() throws IOException {
        String json = "{"
                + "\"fertilizers\":[{\"id\":1,\"name\":\"ইউরিয়া\",\"category\":\"নাইট্রোজেন\","
                + "\"npkRatio\":\"৪৬-০-০\",\"description\":\"d\",\"usage\":\"u\","
                + "\"recommendedCrops\":[\"ধান\",\"গম\"],\"unitPrice\":27.0,\"unit\":\"কেজি\"}],"
                + "\"shops\":[{\"id\":5,\"shopName\":\"রহমান\",\"ownerName\":\"o\",\"phone\":\"017\","
                + "\"district\":\"বগুড়া\",\"upazila\":\"শেরপুর\",\"address\":\"a\"}],"
                + "\"stocks\":[{\"shopId\":5,\"fertilizerId\":1,\"quantity\":500.0,\"sellingPrice\":28.5}]"
                + "}";

        MockData data = JsonDataSource.parse(stream(json));

        Fertilizer f = data.getFertilizers().get(0);
        assertEquals(1, f.getId());
        assertEquals("ইউরিয়া", f.getName());
        assertEquals(2, f.getRecommendedCrops().size());
        assertEquals(27.0, f.getUnitPrice(), 0.0001);
        assertEquals("বগুড়া", data.getShops().get(0).getDistrict());
        Stock s = data.getStocks().get(0);
        assertEquals(5, s.getShopId());
        assertEquals(500.0, s.getQuantity(), 0.0001);
        assertEquals(28.5, s.getSellingPrice(), 0.0001);
    }

    @Test
    public void parse_missingOptionalFieldsAreEmptyNotNull() throws IOException {
        String json = "{\"fertilizers\":[{\"id\":1,\"name\":\"x\"}],\"shops\":[{\"id\":1}]}";

        MockData data = JsonDataSource.parse(stream(json));

        Fertilizer f = data.getFertilizers().get(0);
        assertTrue(f.getRecommendedCrops().isEmpty());
        assertEquals("", f.getUsage());
        assertEquals("", data.getShops().get(0).getPhone());
        assertTrue(data.getStocks().isEmpty());
        assertEquals("", data.getWeather().getLocation());
        assertEquals(0, data.getWeather().getRainProbability());
    }

    @Test(expected = IOException.class)
    public void parse_malformedJsonThrowsIOException() throws IOException {
        JsonDataSource.parse(stream("{\"fertilizers\": [ {"));
    }

    @Test(expected = IOException.class)
    public void parse_emptyFileThrowsIOException() throws IOException {
        JsonDataSource.parse(stream(""));
    }

    @Test(expected = IOException.class)
    public void parse_trailingCommaThrowsIOException() throws IOException {
        JsonDataSource.parse(stream("{\"stocks\":[{\"shopId\":1,\"fertilizerId\":1},]}"));
    }

    @Test(expected = IOException.class)
    public void parse_nullListEntryThrowsIOException() throws IOException {
        JsonDataSource.parse(stream("{\"shops\":[{\"id\":1}, null]}"));
    }

    @Test
    public void realAssetFile_isValidAndConsistent() throws IOException {
        MockData data;
        try (InputStream in = new FileInputStream("src/main/assets/mock_data.json")) {
            data = JsonDataSource.parse(in);
        }

        assertEquals(8, data.getFertilizers().size());
        assertEquals(6, data.getShops().size());
        assertFalse(data.getStocks().isEmpty());
        assertEquals("ঢাকা", data.getWeather().getLocation());
        assertEquals(1, data.getAdmins().size());
        assertEquals("admin", data.getAdmins().get(0).getUsername());
        for (Shop shop : data.getShops()) {
            assertFalse("missing username for shop " + shop.getId(), shop.getUsername().isEmpty());
            assertFalse("missing password for shop " + shop.getId(), shop.getPassword().isEmpty());
            assertTrue(shop.isActive());
        }
        assertEquals(20, data.getWeather().getRainProbability());

        Set<Integer> fertilizerIds = new HashSet<>();
        for (Fertilizer f : data.getFertilizers()) {
            assertTrue("duplicate fertilizer id " + f.getId(), fertilizerIds.add(f.getId()));
            assertFalse(f.getName().isEmpty());
        }
        Set<Integer> shopIds = new HashSet<>();
        for (Shop s : data.getShops()) {
            assertTrue("duplicate shop id " + s.getId(), shopIds.add(s.getId()));
            assertNotNull(s.getPhone());
        }
        assertTrue("expected seeded sales", data.getSales().size() >= 10);
        Set<String> invoices = new HashSet<>();
        for (com.sarbondhu.app.data.model.Sale sale : data.getSales()) {
            assertTrue("duplicate invoice " + sale.getInvoiceNo(), invoices.add(sale.getInvoiceNo()));
            assertTrue("unknown shop in sale " + sale.getId(), shopIds.contains(sale.getShopId()));
            assertFalse(sale.getItems().isEmpty());
            for (com.sarbondhu.app.data.model.SaleItem item : sale.getItems()) {
                assertTrue(fertilizerIds.contains(item.getFertilizerId()));
                assertTrue(item.getQuantity() > 0);
            }
        }
        for (Stock s : data.getStocks()) {
            assertTrue("unknown shop " + s.getShopId(), shopIds.contains(s.getShopId()));
            assertTrue("unknown fertilizer " + s.getFertilizerId(),
                    fertilizerIds.contains(s.getFertilizerId()));
        }
    }
}
