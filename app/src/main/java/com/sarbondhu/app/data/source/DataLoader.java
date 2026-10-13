package com.sarbondhu.app.data.source;

import com.sarbondhu.app.data.model.MockData;

import java.io.IOException;

/** Supplies the app's data. Swap the implementation to move from mock JSON to a real API. */
public interface DataLoader {
    MockData load() throws IOException;
}
