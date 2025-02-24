package org.uroran.service.exporters;

import java.io.File;
import java.io.IOException;


public interface ChartExporter {
    void export(Object data, File file) throws IOException;
}
