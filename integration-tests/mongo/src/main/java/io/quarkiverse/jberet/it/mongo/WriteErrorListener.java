package io.quarkiverse.jberet.it.mongo;

import java.util.List;

import jakarta.batch.api.chunk.listener.AbstractItemWriteListener;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

@Named
@Dependent
public class WriteErrorListener extends AbstractItemWriteListener {
    @Override
    public void afterWrite(List<Object> items) throws Exception {
        // aborts the last chunk (8 elements, so 2 will get rolled back and only 6 are written)
        if (items.size() < 3) {
            throw new WriteException();
        }
    }
}
