package com.sarbondhu.app.data.source;

import android.content.res.AssetManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import com.sarbondhu.app.data.model.MockData;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/** Reads mock_data.json from the app's assets folder. */
public class JsonDataSource implements DataLoader {

    public static final String FILE_NAME = "mock_data.json";

    // Strict so typos like trailing commas fail loudly instead of producing null entries.
    private static final Gson GSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();

    private final AssetManager assets;

    public JsonDataSource(AssetManager assets) {
        this.assets = assets;
    }

    @Override
    public MockData load() throws IOException {
        try (InputStream in = assets.open(FILE_NAME)) {
            return parse(in);
        }
    }

    public static MockData parse(InputStream in) throws IOException {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            MockData data = GSON.fromJson(reader, MockData.class);
            if (data == null) {
                throw new IOException("Data file is empty");
            }
            if (data.getFertilizers().contains(null) || data.getShops().contains(null)
                    || data.getStocks().contains(null) || data.getAdmins().contains(null)
                    || data.getSales().contains(null)) {
                throw new IOException("Data file has empty entries");
            }
            return data;
        } catch (JsonParseException e) {
            throw new IOException("Data file is malformed", e);
        }
    }
}
